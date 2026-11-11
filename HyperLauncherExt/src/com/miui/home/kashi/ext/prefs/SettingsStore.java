package com.miui.home.kashi.ext.prefs;

import com.miui.home.kashi.ext.L;

import android.content.Context;
import android.provider.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * All persistence in one place: Settings.System kashi_* keys (read directly; written directly when the app is a
 * privileged system app, otherwise through one persistent root shell), root helpers, launcher restart and the
 * launcher helper's start-up cache.
 */
public final class SettingsStore {
    public static final String LAUNCHER = "com.miui.home";

    private final Context ctx;
    private final Map<String, Integer> pending = new HashMap<>();
    public final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean directWrite;
    private Process shell;
    private OutputStream shellIn;

    public interface Listener { void onError(String message); }
    public Listener listener = m -> { };

    public SettingsStore(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        directWrite = probeDirectWrite();
    }

    public boolean isDirectWrite() { return directWrite; }

    /** Root was granted (welcome screen or a "needs root" row). */
    public boolean hasRoot() { return ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean("root_ok", false); }
    public void setRoot(boolean granted) { ctx.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean("root_ok", granted).apply(); }

    /** Asks su for root (shows the root manager prompt); true when the shell really runs as uid 0. */
    public static boolean requestRoot() {
        try { return isRootOutput(su("echo KASHI_UID=$(id -u); id; exit 0", null).text()); }
        catch (Exception e) { return false; }
    }

    public int get(String key, int def) {
        Integer p = pending.get(key);   // a write may still be in flight
        return p != null ? p : Settings.System.getInt(ctx.getContentResolver(), key, def);
    }

    /** -1 screen choice -> the launcher's own Settings.System switch_personal_assistant value (0 = leave the ROM's). */
    static final String[] MINUS_VALUES = {null, "personal_assistant_app_vault", "personal_assistant_google"};

    public void putSystemString(String key, String value) {
        io.execute(() -> {
            if (directWrite) {
                try { if (Settings.System.putString(ctx.getContentResolver(), key, value)) return; }
                catch (Exception e) { directWrite = false; }
            }
            try { shell().write(("settings put system " + key + " " + value + "\n").getBytes(StandardCharsets.UTF_8)); shellIn.flush(); }
            catch (Exception e) { shell = null; listener.onError(L.s(ctx, "save_no_permission")); }
        });
    }

    public void put(String key, int value) {
        if ("kashi_minus_screen".equals(key) && value > 0 && value < MINUS_VALUES.length)
            putSystemString("switch_personal_assistant", MINUS_VALUES[value]);
        pending.put(key, value);
        io.execute(() -> {
            if (directWrite) {
                try { if (Settings.System.putInt(ctx.getContentResolver(), key, value)) return; }
                catch (Exception e) { directWrite = false; }
            }
            try { shell().write(("settings put system " + key + " " + value + "\n").getBytes(StandardCharsets.UTF_8)); shellIn.flush(); }
            catch (Exception e) { shell = null; listener.onError(L.s(ctx, "save_no_permission")); }
        });
    }

    private boolean probeDirectWrite() {
        try {
            return Settings.System.canWrite(ctx) && Settings.System.putInt(ctx.getContentResolver(), "kashi_ext_probe", 1);
        } catch (Exception e) { return false; }
    }

    private OutputStream shell() throws Exception {
        if (shell == null || !shell.isAlive()) {
            shell = startSu();
            shellIn = shell.getOutputStream();
            final Process p = shell;
            new Thread(() -> { try { byte[] b = new byte[512]; while (p.getInputStream().read(b) >= 0) { } } catch (Exception ignored) { } }).start();
        }
        return shellIn;
    }

    // ------------------------------------------------------------------ root

    public static final class Result {
        public int code; public byte[] out;
        public String text() { return new String(out, StandardCharsets.UTF_8); }
    }

    /** su binaries of Magisk, KernelSU, APatch and older managers; the first that starts is remembered. */
    private static final String[] SU_PATHS = {"su", "/system/bin/su", "/system/xbin/su", "/debug_ramdisk/su", "/sbin/su",
            "/data/adb/ksu/bin/su", "/data/adb/ap/bin/su", "/system_ext/bin/su", "/vendor/bin/su"};
    private static volatile String suBin;

    /** Starts su with the given arguments, trying every known su location (some managers do not put it on PATH). */
    public static Process startSu(String... args) throws Exception {
        Exception last = null;
        String known = suBin;
        for (String bin : known != null ? new String[] {known} : SU_PATHS) {
            if (!bin.equals("su") && !new java.io.File(bin).exists()) continue;
            try {
                String[] cmd = new String[args.length + 1];
                cmd[0] = bin;
                System.arraycopy(args, 0, cmd, 1, args.length);
                Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                suBin = bin;
                return p;
            } catch (Exception e) { last = e; }
        }
        if (known != null) { suBin = null; return startSu(args); }
        throw last != null ? last : new java.io.IOException("su not found");
    }

    /** True when su output proves uid 0 (our marker, or a plain `id` line). */
    public static boolean isRootOutput(String out) {
        return out.contains("KASHI_UID=0") || out.matches("(?s).*\\buid=0\\(.*");
    }

    public static Result su(String script, byte[] stdin) throws Exception {
        Process p = startSu("-c", script);
        if (stdin != null) p.getOutputStream().write(stdin);
        p.getOutputStream().close();
        Result r = new Result();
        r.out = p.getInputStream().readAllBytes();
        r.code = p.waitFor();
        return r;
    }

    // ------------------------------------------------------------------ launcher restart

    /** Restarts the launcher; values it reads only at start-up are written to the helper cache first. */
    public void restartLauncher(Runnable done) {
        io.execute(() -> {
            try {
                Thread.sleep(directWrite ? 3500 : 300);   // without root the helper picks changes up by itself (3 s poll)
                writeHelperCache();
                if (!forceStopDirect()) {
                    shell().write(("am force-stop " + LAUNCHER + "\n").getBytes(StandardCharsets.UTF_8));
                    shellIn.flush();
                }
                done.run();
            } catch (Exception e) { listener.onError(L.s(ctx, "restart_failed", e.getMessage())); }
        });
    }

    /** Force-stops a package: ActivityManager.forceStopPackage (FORCE_STOP_PACKAGES) or root. */
    public static void forceStop(Context c, String pkg) {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getClass().getMethod("forceStopPackage", String.class).invoke(am, pkg);
        } catch (Throwable t) {
            try { su("am force-stop " + pkg, null); } catch (Exception ignored) { }
        }
    }

    /** Clears another app's cache: PackageManager.deleteApplicationCacheFiles (DELETE_CACHE_FILES, privileged). */
    public static boolean clearAppCache(Context c, String pkg) {
        try {
            Class<?> observer = Class.forName("android.content.pm.IPackageDataObserver");
            android.content.pm.PackageManager pm = c.getPackageManager();
            pm.getClass().getMethod("deleteApplicationCacheFiles", String.class, observer).invoke(pm, pkg, null);
            return true;
        } catch (Throwable t) { return false; }
    }

    private boolean forceStopDirect() {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            am.getClass().getMethod("forceStopPackage", String.class).invoke(am, LAUNCHER);
            return true;
        } catch (Throwable t) { return false; }
    }

    // ------------------------------------------------------------------ helper cache (KEEP IN SYNC with helper/kashidock.c)

    public static final int HELPER_MAGIC = 0x4b443039;
    public static final String[] HELPER_KEYS = {"kashi_dock_enable", "kashi_dock_style", "kashi_dock_radius", "kashi_dock_padding",
            "kashi_dock_height", "kashi_home_qsb", "kashi_widget_resize", "kashi_paid_widgets", "kashi_grid_gap",
            "kashi_recents_blur", "kashi_recents_hide_clear", "kashi_icon_effect", "kashi_hide_any_app", "kashi_indicator_offset",
            "kashi_dock_icon_offset", "kashi_full_anim", "kashi_icon_bigger", "kashi_widget_source", "kashi_minus_screen"};
    public static final int[] HELPER_DEFAULTS = {0, 1, 30, 25, 0, 2, 1, 1, 0, 100, 0, 1, 1, 0, 0, 1, 0, 0, 0};
    public static final String[] GLASS_KEYS = {"blur", "luminance", "saturation", "brightness", "alpha", "refraction",
            "thickness", "bg_saturation", "bg_brightness", "burn", "edge", "edge_feather", "reflection_offset", "light", "darker"};

    private void writeHelperCache() {
        try {
            ByteBuffer main = ByteBuffer.allocate(4 * (HELPER_KEYS.length + 1)).order(ByteOrder.LITTLE_ENDIAN);
            main.putInt(HELPER_MAGIC);
            for (int i = 0; i < HELPER_KEYS.length; i++) main.putInt(get(HELPER_KEYS[i], HELPER_DEFAULTS[i]));
            ByteBuffer glass = ByteBuffer.allocate(4 * 32).order(ByteOrder.LITTLE_ENDIAN);
            for (String rec : new String[] {"home", "dock"}) {
                glass.putInt(get("kashi_glass_" + rec + "_on", 0));
                for (int i = 0; i < GLASS_KEYS.length; i++) glass.putInt(get("kashi_glass_" + rec + "_" + GLASS_KEYS[i], i == 0 ? 100 : 0));
            }
            File a = new File(ctx.getCacheDir(), "kd.bin"), b = new File(ctx.getCacheDir(), "kd.glass");
            try (FileOutputStream o = new FileOutputStream(a)) { o.write(main.array()); }
            try (FileOutputStream o = new FileOutputStream(b)) { o.write(glass.array()); }
            a.setReadable(true, false); b.setReadable(true, false);
            String dir = "/data/user_de/0/" + LAUNCHER, f1 = dir + "/kashi_dock.bin", f2 = dir + "/kashi_dock.bin.glass";
            su("o=$(stat -c %u:%g " + dir + "); cp -f '" + a + "' " + f1 + " && cp -f '" + b + "' " + f2 + " && chown $o " + f1 + " " + f2
                    + " && chmod 600 " + f1 + " " + f2 + " && restorecon " + f1 + " " + f2 + " 2>/dev/null; true", null);
            a.delete(); b.delete();
        } catch (Exception ignored) { /* no root: the helper's own poll still applies the values */ }
    }
}

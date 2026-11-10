package com.miui.home.kashi.ext.modules;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;
import com.miui.home.kashi.ext.prefs.SettingsStore;

import java.io.File;
import java.util.List;

/**
 * Icon pack apps -> theme icons file (/data/system/theme/icons), see IconPacks. Written directly when the ROM allows it
 * (SELinux rule for priv_app / platform_app), otherwise with root.
 */
public final class IconPackModule implements Action {
    static final String KEY_LABEL = "icon_pack_label", KEY_PKG = "icon_pack_pkg";

    /** Rebuilds the applied pack, so apps installed since then get their icons from it too. */
    public static final Action REFRESH = new Action() {
        @Override public String value(Host host, PrefItem item) { return null; }
        @Override public void run(Host host, PrefItem item) {
            if (!available(host)) { host.toast(com.miui.home.kashi.ext.L.s(host.activity(), "iconpack_root")); return; }
            Context c = host.activity();
            android.content.SharedPreferences sp = c.getSharedPreferences("app", Context.MODE_PRIVATE);
            String pkg = sp.getString(KEY_PKG, null), label = sp.getString(KEY_LABEL, "System");
            if (pkg == null && "System".equals(label)) { host.toast(com.miui.home.kashi.ext.L.s(c, "iconpack_none")); return; }
            for (IconPacks.Pack p : IconPacks.installed(c))   // packs applied by v2.2 only stored the label
                if (pkg != null ? p.pkg.equals(pkg) : p.label.equals(label)) { apply(host, p); return; }
            host.toast(com.miui.home.kashi.ext.L.s(c, "iconpack_gone"));
        }
    };

    @Override public String value(Host host, PrefItem item) {
        String l = host.activity().getSharedPreferences("app", Context.MODE_PRIVATE).getString(KEY_LABEL, "System");
        return "System".equals(l) ? com.miui.home.kashi.ext.L.s(host.activity(), "iconpack_system") : l;
    }

    @Override public void run(Host host, PrefItem item) {
        if (!available(host)) { host.toast(com.miui.home.kashi.ext.L.s(host.activity(), "iconpack_root")); return; }
        Context c = host.activity();
        List<IconPacks.Pack> packs = IconPacks.installed(c);
        String[] names = new String[packs.size() + 1], subs = new String[packs.size() + 1];
        Drawable[] icons = new Drawable[packs.size() + 1];
        names[0] = com.miui.home.kashi.ext.L.s(c, "iconpack_system");
        subs[0] = com.miui.home.kashi.ext.L.s(c, packs.isEmpty() ? "iconpack_no_apps" : "iconpack_remove");
        icons[0] = c.getApplicationInfo().loadIcon(c.getPackageManager());
        for (int i = 0; i < packs.size(); i++) { names[i + 1] = packs.get(i).label; subs[i + 1] = packs.get(i).pkg; icons[i + 1] = packs.get(i).icon; }
        host.ui().sheet(com.miui.home.kashi.ext.L.s(c, "iconpack_title"), names, subs, icons, -1, i -> apply(host, i == 0 ? null : packs.get(i - 1))).show();
    }

    /** Root granted, or the ROM lets this app write the theme folder. */
    public static boolean available(Host host) { return host.store().hasRoot() || IconPacks.canWriteThemeDirect(); }

    /** Install or restore the theme icons file without root; false when the theme folder is not writable. */
    private static boolean applyDirect(Context c, File zip) throws Exception {
        if (!IconPacks.canWriteThemeDirect()) return false;
        File t = new File(IconPacks.THEME_ICONS), bak = new File(IconPacks.THEME_ICONS + ".kashi_backup");
        if (zip == null) {                                    // back to the stock icons
            if (bak.exists()) { if (!bak.renameTo(t)) throw new Exception("restore failed"); }
            else if (t.exists() && !t.delete()) throw new Exception("remove failed");
        } else {
            if (t.exists() && !bak.exists()) java.nio.file.Files.copy(t.toPath(), bak.toPath());
            File tmp = new File(t.getParentFile(), "icons.kashi_tmp");
            java.nio.file.Files.copy(zip.toPath(), tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.setReadable(true, false);
            java.nio.file.Files.move(tmp.toPath(), t.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            t.setReadable(true, false);
        }
        SettingsStore.clearAppCache(c, SettingsStore.LAUNCHER);
        SettingsStore.forceStop(c, SettingsStore.LAUNCHER);
        return true;
    }

    private static void apply(Host host, IconPacks.Pack pack) {
        Context c = host.activity();
        host.toast(pack == null ? com.miui.home.kashi.ext.L.s(c, "iconpack_restoring") : com.miui.home.kashi.ext.L.s(c, "iconpack_building", pack.label));
        host.store().io.execute(() -> {
            try {
                String t = IconPacks.THEME_ICONS, bak = t + ".kashi_backup", L = SettingsStore.LAUNCHER;
                if (pack == null) {
                    if (applyDirect(c, null)) { /* done without root */ } else {
                    SettingsStore.Result r = SettingsStore.su("if [ -f '" + bak + "' ]; then mv -f '" + bak + "' '" + t + "'; else rm -f '" + t + "'; fi; am force-stop " + L, null);
                    if (r.code != 0) throw new Exception(r.text().trim());
                    }
                } else {
                    File zip = new File(c.getCacheDir(), "icons.zip");
                    long[] last = {0};
                    int n = IconPacks.build(c, pack.pkg, zip, (done, total) -> {
                        long now = System.currentTimeMillis();
                        if (now - last[0] > 1500) { last[0] = now; host.toast(com.miui.home.kashi.ext.L.s(c, "iconpack_progress", done, total)); }
                    });
                    if (n == 0) throw new Exception(com.miui.home.kashi.ext.L.s(c, "iconpack_no_icons"));
                    zip.setReadable(true, false);
                    if (!applyDirect(c, zip)) {
                    SettingsStore.Result r = SettingsStore.su("[ -f '" + t + "' ] && [ ! -f '" + bak + "' ] && cp -p '" + t + "' '" + bak + "'; "
                            + "cp -f '" + zip + "' '" + t + "' && chown system_theme:system_theme '" + t + "' && chmod 0644 '" + t + "' "
                            + "&& (restorecon '" + t + "' 2>/dev/null; true) && rm -rf /data/user/0/" + L + "/cache/* && am force-stop " + L, null);
                    if (r.code != 0) throw new Exception(r.text().trim().isEmpty() ? com.miui.home.kashi.ext.L.s(c, "root_denied") : r.text().trim());
                    }
                    zip.delete();
                    host.toast(com.miui.home.kashi.ext.L.s(c, "iconpack_applied", pack.label, n));
                }
                c.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString(KEY_LABEL, pack == null ? "System" : pack.label)
                        .putString(KEY_PKG, pack == null ? null : pack.pkg).apply();
                host.post(host::reloadPage);
            } catch (Exception e) { host.toast(com.miui.home.kashi.ext.L.s(c, "iconpack_failed", e.getMessage())); }
        });
    }
}

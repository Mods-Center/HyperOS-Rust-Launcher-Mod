package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;
import com.miui.home.kashi.ext.prefs.SettingsStore;

import java.nio.charset.StandardCharsets;

/** About page helpers: version text and the diagnostics sheet (helper log + launcher search preferences). */
public final class AboutModule {
    static final class Version implements Action {
        @Override public void run(Host host, PrefItem item) { }
        @Override public String value(Host host, PrefItem item) {
            try { return host.activity().getPackageManager().getPackageInfo(host.activity().getPackageName(), 0).versionName; }
            catch (Exception e) { return null; }
        }
    }

    static final class Diagnostics implements Action {
        @Override public void run(Host host, PrefItem item) {
            host.store().io.execute(() -> {
                StringBuilder b = new StringBuilder();
                b.append(L.s(host.activity(), "diag_mode", L.s(host.activity(), host.store().isDirectWrite() ? "diag_direct" : "diag_root"))).append('\n');
                String log = null;
                try {   // READ_LOGS as a priv-app: no root needed
                    Process lp = new ProcessBuilder("logcat", "-d", "-s", "KashiDock").redirectErrorStream(true).start();
                    String all = new String(lp.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    lp.waitFor();
                    if (all.contains("KashiDock")) log = tail(all, 6);
                } catch (Exception ignored) { }
                if (log == null) try { log = tail(SettingsStore.su("logcat -d -s KashiDock", null).text(), 6); } catch (Exception ignored) { }
                b.append(log == null ? L.s(host.activity(), "diag_nolog") : log);
                String text = b.toString();
                host.post(() -> host.ui().sheet(L.s(host.activity(), "diag_title"), new String[] {L.s(host.activity(), "copy_clipboard")}, new String[] {text}, null, -1, i -> {
                    Context c = host.activity();
                    ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("HyperLauncherExt", text));
                    host.toast(L.s(host.activity(), "copied"));
                }).show());
            });
        }
    }

    static String tail(String s, int n) {
        String[] l = s.trim().split("\n");
        StringBuilder b = new StringBuilder();
        for (int k = Math.max(0, l.length - n); k < l.length; k++) b.append(l[k]).append('\n');
        return b.toString();
    }

    private AboutModule() { }
}

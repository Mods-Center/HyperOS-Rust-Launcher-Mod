package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.Context;
import android.content.Intent;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.SettingsStore;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Home screen setup backup (root): the launcher's layout databases (every grid size launcherCxR.db, the drawer copy and
 * miui_widget.db, journals included), files/maml (MAML widget configs + resources) and files/launcher.preferences,
 * as one tar.gz with a KASHI_HOME_BACKUP marker. Restore stops the launcher, swaps the files in and fixes owner,
 * mode and SELinux labels. App widgets keep their ids: they come back on the same device as long as they still exist.
 */
public final class HomeBackupModule {
    private static final String MARKER = "KASHI_HOME_BACKUP";

    private static String dir() {
        return "/data/user_de/" + (android.os.Process.myUid() / 100000) + "/" + SettingsStore.LAUNCHER;
    }

    private static boolean rootOrToast(Host host) {
        if (host.store().hasRoot()) return true;
        host.toast(L.s(host.activity(), "hb_need_root"));
        return false;
    }

    static final Action EXPORT = (host, item) -> {
        if (!rootOrToast(host)) return;
        Context c = host.activity();
        String name = "HyperLauncher-home-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date()) + ".tar.gz";
        host.startForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/gzip").putExtra(Intent.EXTRA_TITLE, name), (code, data) -> {
            if (data == null || data.getData() == null) return;
            host.toast(L.s(c, "hb_working"));
            host.store().io.execute(() -> {
                try {
                    String d = dir();
                    SettingsStore.Result r = SettingsStore.su("exec 2>/dev/null; S=/data/local/tmp/kashi_hb_$$; rm -rf $S; "
                            + "mkdir -p $S/databases/drawer $S/files && cd '" + d + "' || exit 3; "
                            + "for f in databases/launcher*.db* databases/miui_widget.db* databases/drawer/launcher*.db*; do "
                            + "[ -f \"$f\" ] && cp -p \"$f\" \"$S/$f\"; done; "
                            + "[ -f files/launcher.preferences ] && cp -p files/launcher.preferences $S/files/; "
                            + "[ -d files/maml ] && cp -rp files/maml $S/files/; "
                            + "ls $S/databases/launcher*.db >/dev/null || { rm -rf $S; exit 4; }; "
                            + "echo \"" + MARKER + " 1 $(dumpsys package " + SettingsStore.LAUNCHER + " | grep -m1 versionName)\" > $S/" + MARKER + "; "
                            + "tar -czf - -C $S .; r=$?; rm -rf $S; exit $r", null);
                    byte[] b = r.out;
                    if (r.code != 0 || b.length < 2 || (b[0] & 0xff) != 0x1f || (b[1] & 0xff) != 0x8b)
                        throw new Exception(r.code == 4 ? L.s(c, "hb_no_layout") : r.text().trim().isEmpty() ? L.s(c, "root_denied") : "exit " + r.code);
                    try (OutputStream o = c.getContentResolver().openOutputStream(data.getData(), "wt")) { o.write(b); }
                    host.toast(L.s(c, "hb_saved"));
                } catch (Exception e) { host.toast(L.s(c, "save_failed", e.getMessage())); }
            });
        });
    };

    static final Action IMPORT = (host, item) -> {
        if (!rootOrToast(host)) return;
        Context c = host.activity();
        host.startForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), (code, data) -> {
            if (data == null || data.getData() == null) return;
            byte[] b;
            try (InputStream in = c.getContentResolver().openInputStream(data.getData())) { b = in.readAllBytes(); }
            catch (Exception e) { host.toast(L.s(c, "import_failed", e.getMessage())); return; }
            if (b.length < 2 || (b[0] & 0xff) != 0x1f || (b[1] & 0xff) != 0x8b) { host.toast(L.s(c, "hb_not_backup")); return; }
            host.ui().sheet(L.s(c, "hb_confirm_title"), new String[] {L.s(c, "hb_restore"), L.s(c, "cancel")},
                    new String[] {L.s(c, "hb_confirm_sum"), null}, null, -1, i -> { if (i == 0) restore(host, b); }).show();
        });
    };

    private static void restore(Host host, byte[] archive) {
        Context c = host.activity();
        host.toast(L.s(c, "hb_working"));
        host.store().io.execute(() -> {
            try {
                String d = dir(), p = SettingsStore.LAUNCHER;
                SettingsStore.Result r = SettingsStore.su("exec 2>/dev/null; S=/data/local/tmp/kashi_hr; D='" + d + "'; rm -rf $S; mkdir -p $S; "
                        + "tar -xzf - -C $S || { rm -rf $S; exit 5; }; "
                        + "[ -f $S/" + MARKER + " ] && ls $S/databases/launcher*.db >/dev/null || { rm -rf $S; exit 6; }; "
                        + "[ -d $D/databases ] || { rm -rf $S; exit 7; }; o=$(stat -c %u:%g $D); "
                        + "am force-stop " + p + "; "
                        + "rm -f $D/databases/launcher*.db* $D/databases/miui_widget.db* $D/databases/drawer/launcher*.db*; "
                        + "mkdir -p $D/databases/drawer; cp -rf $S/databases/. $D/databases/; "
                        + "if [ -d $S/files/maml ]; then rm -rf $D/files/maml; cp -rf $S/files/maml $D/files/; fi; "
                        + "[ -f $S/files/launcher.preferences ] && cp -f $S/files/launcher.preferences $D/files/; "
                        + "chown -R $o $D/databases $D/files; "
                        + "find $D/databases -type f -exec chmod 660 {} +; "
                        + "[ -d $D/files/maml ] && { find $D/files/maml -type d -exec chmod 700 {} +; find $D/files/maml -type f -exec chmod 600 {} +; }; "
                        + "restorecon -R $D/databases $D/files; rm -rf $S; am force-stop " + p + "; exit 0", archive);
                if (r.code == 5 || r.code == 6) throw new Exception(L.s(c, "hb_not_backup"));
                if (r.code != 0) throw new Exception(r.text().trim().isEmpty() ? (r.code == 7 ? "launcher data not found" : L.s(c, "root_denied")) : r.text().trim());
                host.toast(L.s(c, "hb_restored"));
            } catch (Exception e) { host.toast(L.s(c, "import_failed", e.getMessage())); }
        });
    }

    private HomeBackupModule() { }
}

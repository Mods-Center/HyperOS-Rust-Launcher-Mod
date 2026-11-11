package com.miui.home.kashi.ext;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Invisible trampoline for the launcher's long-press "Share" (patched to send com.miui.home.kashi.ext.SH with the app's
 * package name in its extras, type application/vnd.android.package-archive). Shares the installed APK through the
 * system share sheet, served by {@link ApkProvider} (no copy).
 */
public class ShareActivity extends Activity {
    private boolean launched;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        overridePendingTransition(0, 0);
        android.view.Window w = getWindow();
        w.setDecorFitsSystemWindows(false);
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        w.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        w.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        w.setStatusBarContrastEnforced(false);
        w.setNavigationBarContrastEnforced(false);
    }

    @Override protected void onResume() {
        super.onResume();
        if (launched) return;
        launched = true;
        getWindow().getDecorView().post(this::share);
    }

    /** The package named in the launcher's extras (any string extra that is an installed package). */
    private String packageFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return null;
        PackageManager pm = getPackageManager();
        for (String key : extras.keySet()) {
            Object v = extras.get(key);
            if (!(v instanceof String)) continue;
            String s = (String) v;
            try { pm.getApplicationInfo(s, 0); return s; } catch (Exception ignored) { }
        }
        return null;
    }

    private void share() {
        try {
            String pkg = packageFromIntent();
            if (pkg == null) throw new IllegalStateException();
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            String label = String.valueOf(ai.loadLabel(getPackageManager()));
            Uri uri = ApkProvider.uriFor(pkg);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("application/vnd.android.package-archive")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TITLE, label)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(android.content.ClipData.newRawUri(label, uri));
            Intent chooser = Intent.createChooser(send, L.s(this, "share_app_title", label))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, L.s(this, "share_failed"), Toast.LENGTH_SHORT).show();
        }
        finish();
        overridePendingTransition(0, 0);
    }
}

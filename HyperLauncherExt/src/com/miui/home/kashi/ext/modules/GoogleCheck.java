package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.miui.home.kashi.ext.prefs.Host;

/**
 * Circle to Search and the other assistant features of the Google app only work when it is a privileged system app
 * AND the default assistant. When the Google app is installed but one of the two is missing, a sheet points to the
 * Google App PrivApp module (Download) or is dismissed for good (Ignore).
 */
public final class GoogleCheck {
    static final String GSA = "com.google.android.googlequicksearchbox";
    public static final String REPO = "https://github.com/kakashi-1v1/Google-App-PrivApp/releases/latest";
    private static final String KEY_IGNORED = "gsa_warning_ignored";
    private static final int PRIVATE_FLAG_PRIVILEGED = 1 << 3;

    /** Privileged system app (also when it runs as an update from /data). */
    static boolean privileged(Context c, ApplicationInfo ai) {
        try {
            int pf = ApplicationInfo.class.getField("privateFlags").getInt(ai);
            return (pf & PRIVATE_FLAG_PRIVILEGED) != 0;
        } catch (Throwable ignored) { }
        // hidden field not reachable: privileged-only permissions are granted only to priv-apps
        PackageManager pm = c.getPackageManager();
        return pm.checkPermission("android.permission.CAPTURE_AUDIO_HOTWORD", GSA) == PackageManager.PERMISSION_GRANTED
                || pm.checkPermission("android.permission.MANAGE_VOICE_KEYPHRASES", GSA) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean defaultAssistant(Context c) {
        for (String k : new String[] {"assistant", "voice_interaction_service"}) {
            try {
                String v = Settings.Secure.getString(c.getContentResolver(), k);
                if (v != null && !v.isEmpty()) return v.startsWith(GSA + "/");
            } catch (Throwable ignored) { }
        }
        return false;
    }

    /** Shows the sheet when needed; call once the first page is up. */
    public static void maybeWarn(Host host) {
        Context c = host.activity();
        SharedPreferences sp = c.getSharedPreferences("app", Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_IGNORED, false)) return;
        ApplicationInfo ai;
        try { ai = c.getPackageManager().getApplicationInfo(GSA, 0); }
        catch (PackageManager.NameNotFoundException e) { return; }   // no Google app: nothing to warn about
        if (!ai.enabled) return;
        boolean priv = privileged(c, ai), asst = defaultAssistant(c);
        if (priv && asst) return;
        String why = !priv && !asst ? L.s(c, "gsa_warn_both") : !priv ? L.s(c, "gsa_warn_priv") : L.s(c, "gsa_warn_assistant");
        host.ui().sheet(L.s(c, "gsa_warn_title"),
                new String[] {L.s(c, "gsa_warn_download"), L.s(c, "gsa_warn_ignore")},
                new String[] {why + " " + L.s(c, "gsa_warn_download_sum"), L.s(c, "gsa_warn_ignore_sum")}, null, -1, i -> {
                    if (i == 0) Modules.openUrl(host, REPO);
                    else sp.edit().putBoolean(KEY_IGNORED, true).apply();
                }).show();
    }

    private GoogleCheck() { }
}

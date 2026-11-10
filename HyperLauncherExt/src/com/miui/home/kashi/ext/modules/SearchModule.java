package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Search bar: which app the patched launcher opens (see SearchActivity). Stored in the app's own preferences. */
public final class SearchModule implements Action {
    public static final String PREFS = "app", KEY_PKG = "search_app_pkg", GOOGLE = "com.google.android.googlequicksearchbox";
    /** Text search entry points, best first. */
    public static final String[] SEARCH_ACTIONS = {"android.search.action.GLOBAL_SEARCH", Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH};
    /** Voice entry points, best first. */
    public static final String[] VOICE_ACTIONS = {"android.speech.action.WEB_SEARCH", "android.speech.action.VOICE_SEARCH_HANDS_FREE",
            Intent.ACTION_VOICE_COMMAND, "android.intent.action.VOICE_ASSIST", Intent.ACTION_ASSIST};

    public static String selected(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PKG, GOOGLE); }

    @Override public String value(Host host, PrefItem item) { return label(host.activity(), selected(host.activity())); }

    @Override public void run(Host host, PrefItem item) {
        if (host.store().get("kashi_home_qsb", 1) == 0) host.store().put("kashi_home_qsb", 1);   // tap redirect always on
        Context c = host.activity();
        PackageManager pm = c.getPackageManager();
        Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        TreeMap<String, String> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ResolveInfo ri : pm.queryIntentActivities(launch, 0)) {
            String p = ri.activityInfo.packageName;
            if (!p.equals(c.getPackageName())) apps.put(ri.loadLabel(pm) + "\u0000" + p, p);
        }
        List<String> pkgs = new ArrayList<>(apps.values());
        String[] names = new String[pkgs.size()], subs = new String[pkgs.size()];
        Drawable[] icons = new Drawable[pkgs.size()];
        String cur = selected(c);
        int sel = -1, k = 0;
        for (Map.Entry<String, String> e : apps.entrySet()) {
            String p = e.getValue();
            names[k] = e.getKey().substring(0, e.getKey().indexOf('\u0000'));
            boolean text = direct(c, p, false) != null, voice = direct(c, p, true) != null;
            subs[k] = L.s(c, text && voice ? "search_sub_both" : text ? "search_sub_text" : voice ? "search_sub_voice" : "search_sub_app");
            try { icons[k] = pm.getApplicationIcon(p); } catch (Exception ignored) { }
            if (p.equals(cur)) sel = k;
            k++;
        }
        host.ui().sheet(L.s(c, "search_app_title"), names, subs, icons, sel, i -> {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PKG, pkgs.get(i)).apply();
            host.toast(L.s(c, "search_set", names[i]));
            host.reloadPage();
        }).show();
    }

    /** The app's own search / voice screen if it declares one, otherwise its launcher activity. */
    public static Intent resolve(Context c, String pkg, boolean voice) {
        Intent direct = direct(c, pkg, voice);
        if (direct == null && voice) direct = direct(c, pkg, false);
        Intent i = direct != null ? direct : c.getPackageManager().getLaunchIntentForPackage(pkg);
        return i == null ? null : i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    public static Intent direct(Context c, String pkg, boolean voice) {
        PackageManager pm = c.getPackageManager();
        for (String action : voice ? VOICE_ACTIONS : SEARCH_ACTIONS) {
            Intent i = new Intent(action).setPackage(pkg);
            if (Intent.ACTION_WEB_SEARCH.equals(action) || Intent.ACTION_SEARCH.equals(action)) i.putExtra("query", "");
            if (pm.resolveActivity(i, 0) != null) return i;
        }
        return null;
    }

    public static String label(Context c, String pkg) {
        try { PackageManager pm = c.getPackageManager(); return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }
}

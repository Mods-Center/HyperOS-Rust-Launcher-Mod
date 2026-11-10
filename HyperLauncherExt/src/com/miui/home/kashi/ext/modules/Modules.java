package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.Intent;
import android.net.Uri;

import com.miui.home.kashi.ext.prefs.Host;

import java.util.HashMap;
import java.util.Map;

/** Registry of feature actions used by &lt;Preference action="…"/&gt; in res/xml/prefs_*.xml. Add new modules here. */
public final class Modules {
    private static final Map<String, Action> ACTIONS = new HashMap<>();

    static {
        ACTIONS.put("search_app", new SearchModule());
        ACTIONS.put("icon_pack", new IconPackModule());
        ACTIONS.put("icon_pack_refresh", IconPackModule.REFRESH);
        ACTIONS.put("glass_state", GlassModule.STATE);
        ACTIONS.put("glass_export", GlassModule.EXPORT);
        ACTIONS.put("glass_import", GlassModule.IMPORT);
        ACTIONS.put("glass_reset", GlassModule.RESET);
        ACTIONS.put("diagnostics", new AboutModule.Diagnostics());
        ACTIONS.put("version", new AboutModule.Version());
    }

    public static Action get(String id) {
        Action a = ACTIONS.get(id);
        if (a == null) throw new IllegalArgumentException("unknown action '" + id + "' (register it in Modules)");
        return a;
    }

    public static void openUrl(Host host, String url) {
        try { host.activity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { host.toast(url); }
    }

    /** Opens pkg/cls (exported) directly, otherwise through root (am start). */
    public static void openComponent(Host host, String component, String action) {
        android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(component);
        Intent i = new Intent(action != null ? action : Intent.ACTION_MAIN).setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { host.activity().startActivity(i); }
        catch (Exception e) {
            host.store().io.execute(() -> {
                try { com.miui.home.kashi.ext.prefs.SettingsStore.su("am start -n " + component, null); }
                catch (Exception e2) { host.toast(L.s(host.activity(), "cannot_open", component)); }
            });
        }
    }

    private Modules() { }
}

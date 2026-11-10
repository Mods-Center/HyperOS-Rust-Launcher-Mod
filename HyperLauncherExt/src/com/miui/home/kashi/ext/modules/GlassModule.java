package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;
import com.miui.home.kashi.ext.prefs.SettingsStore;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Soft glass presets for res/xml/prefs_glass.xml (attribute prefix="kashi_glass_home_" / "kashi_glass_dock_").
 * Preset JSON: {"app","kind":"soft_glass","version":1,"source","enabled","params":{name:int}}.
 */
public final class GlassModule {
    static final String[] KEYS = SettingsStore.GLASS_KEYS;
    static int min(int i) { return i == 0 ? 0 : i == 11 ? -200 : -100; }
    static int max(int i) { return i == 0 ? 200 : i == 11 ? 100 : 200; }
    static int def(int i) { return i == 0 ? 100 : 0; }

    /** "On"/"Off" for a glass sub page row (attribute prefix). */
    static final Action STATE = new Action() {
        @Override public void run(Host host, PrefItem item) { }
        @Override public String value(Host host, PrefItem item) { return host.store().get(item.attr("prefix") + "on", 0) != 0 ? L.s(host.activity(), "state_on") : L.s(host.activity(), "state_off"); }
    };

    static final Action EXPORT = (host, item) -> host.ui().sheet(L.s(host.activity(), "export_title"),
            new String[] {L.s(host.activity(), "save_file"), L.s(host.activity(), "share"), L.s(host.activity(), "copy_clipboard")}, null, null, -1, i -> {
                String prefix = item.attr("prefix"), json = json(host, prefix, item.attr("name", "glass"));
                Context c = host.activity();
                if (i == 0) {
                    host.startForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/json").putExtra(Intent.EXTRA_TITLE, "HyperLauncherExt-" + item.attr("name", "glass") + "-glass.json"),
                            (code, data) -> {
                                if (data == null || data.getData() == null) return;
                                try (OutputStream o = c.getContentResolver().openOutputStream(data.getData(), "wt")) {
                                    o.write(json.getBytes(StandardCharsets.UTF_8));
                                    host.toast(L.s(host.activity(), "preset_saved"));
                                } catch (Exception e) { host.toast(L.s(host.activity(), "save_failed", e.getMessage())); }
                            });
                } else if (i == 1) {
                    c.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("application/json")
                            .putExtra(Intent.EXTRA_TEXT, json), L.s(host.activity(), "share_title")));
                } else {
                    ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("glass preset", json));
                    host.toast(L.s(host.activity(), "preset_copied"));
                }
            }).show();

    static final Action IMPORT = (host, item) -> host.ui().sheet(L.s(host.activity(), "import_title"),
            new String[] {L.s(host.activity(), "from_file"), L.s(host.activity(), "from_clipboard")}, null, null, -1, i -> {
                Context c = host.activity();
                if (i == 0) {
                    host.startForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),
                            (code, data) -> {
                                if (data == null || data.getData() == null) return;
                                try (InputStream in = c.getContentResolver().openInputStream(data.getData())) {
                                    apply(host, item.attr("prefix"), new String(in.readAllBytes(), StandardCharsets.UTF_8));
                                } catch (Exception e) { host.toast(L.s(host.activity(), "import_failed", e.getMessage())); }
                            });
                } else {
                    ClipData clip = ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
                    apply(host, item.attr("prefix"), clip == null || clip.getItemCount() == 0 ? null : String.valueOf(clip.getItemAt(0).coerceToText(c)));
                }
            }).show();

    static final Action RESET = (host, item) -> {
        for (int i = 0; i < KEYS.length; i++) host.store().put(item.attr("prefix") + KEYS[i], def(i));
        host.markDirty();
        host.toast(L.s(host.activity(), "params_reset"));
        host.reloadPage();
    };

    static String json(Host host, String prefix, String name) {
        try {
            JSONObject params = new JSONObject();
            for (int i = 0; i < KEYS.length; i++) params.put(KEYS[i], host.store().get(prefix + KEYS[i], def(i)));
            return new JSONObject().put("app", "HyperLauncherExt").put("kind", "soft_glass").put("version", 1).put("source", name)
                    .put("enabled", host.store().get(prefix + "on", 0) != 0).put("params", params).toString(2);
        } catch (Exception e) { return "{}"; }
    }

    static void apply(Host host, String prefix, String text) {
        try {
            if (text == null || text.trim().isEmpty()) throw new Exception(L.s(host.activity(), "import_empty"));
            JSONObject o = new JSONObject(text.trim());
            if (!"soft_glass".equals(o.optString("kind"))) throw new Exception(L.s(host.activity(), "import_not_preset"));
            JSONObject p = o.getJSONObject("params");
            int n = 0;
            for (int i = 0; i < KEYS.length; i++) {
                if (!p.has(KEYS[i])) continue;
                host.store().put(prefix + KEYS[i], Math.max(min(i), Math.min(max(i), p.getInt(KEYS[i]))));
                n++;
            }
            if (o.has("enabled")) host.store().put(prefix + "on", o.getBoolean("enabled") ? 1 : 0);
            host.markDirty();
            host.toast(L.s(host.activity(), "imported_n", n));
            host.post(host::reloadPage);
        } catch (Exception e) { host.toast(L.s(host.activity(), "import_failed", e.getMessage())); }
    }

    private GlassModule() { }
}

package com.miui.home.kashi.ext.prefs;

import com.miui.home.kashi.ext.L;

import android.content.res.XmlResourceParser;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miui.home.kashi.ext.modules.Action;
import com.miui.home.kashi.ext.modules.Modules;
import com.miui.home.kashi.ext.ui.Ui;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders res/xml/&lt;name&gt;.xml into miuix-style cards. Elements:
 * <pre>
 * &lt;PreferenceScreen title summary&gt;
 *   &lt;PreferenceCategory title [dependency]&gt;                   card with a grey caption
 *     &lt;SwitchPreference key title [summary] defaultValue="true|false" [onValue] [offValue]
 *                       [controls="keyA,keyB"]&gt;                  controls: virtual switch that writes the sliders'
 *                                                                 values when on and 0 to them when off
 *     &lt;SeekBarPreference key title min max defaultValue [unit] [format="signed"] [zeroAs] [dependency]/&gt;
 *     &lt;DropDownPreference key title entries="a|b" entryValues="0|1" defaultValue [summary]/&gt;
 *     &lt;Preference title [summary] action="id" | page="xml" [args="k=v;k=v"] [pageTitle] | url="https://…"
 *                 | component="pkg/cls" [intentAction]/&gt;          component: opens another app's screen
 * </pre>
 * Attributes may use ${arg} placeholders filled from the page arguments.
 */
public final class PrefScreen {
    private final Host host;
    private final Ui ui;
    private final SettingsStore store;
    private final Map<String, List<View>> dependents = new HashMap<>();
    private final Map<String, Ui.Slider> sliders = new HashMap<>();
    private final Map<View, Runnable> valueRefresh = new HashMap<>();

    public PrefScreen(Host host) { this.host = host; this.ui = host.ui(); this.store = host.store(); }

    // ------------------------------------------------------------------ parsing

    public static PrefItem parse(android.content.Context c, String xml, Map<String, String> args) throws Exception {
        int id = c.getResources().getIdentifier(xml, "xml", c.getPackageName());
        if (id == 0) throw new IllegalArgumentException("missing res/xml/" + xml + ".xml");
        XmlResourceParser p = c.getResources().getXml(id);
        Deque<PrefItem> stack = new ArrayDeque<>();
        PrefItem root = null;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
            if (ev == XmlPullParser.START_TAG) {
                PrefItem item = new PrefItem(p.getName());
                for (int i = 0; i < p.getAttributeCount(); i++) {
                    int ref = p.getAttributeResourceValue(i, 0);   // title="@string/x" -> localized text
                    String v = ref != 0 && "string".equals(c.getResources().getResourceTypeName(ref)) ? c.getString(ref) : p.getAttributeValue(i);
                    item.attrs.put(p.getAttributeName(i), subst(v, args));
                }
                if (stack.isEmpty()) root = item; else stack.peek().children.add(item);
                stack.push(item);
            } else if (ev == XmlPullParser.END_TAG) stack.pop();
        }
        p.close();
        return root;
    }

    private static String subst(String v, Map<String, String> args) {
        if (v == null || args == null || !v.contains("${")) return v;
        for (Map.Entry<String, String> e : args.entrySet()) v = v.replace("${" + e.getKey() + "}", e.getValue());
        return v;
    }

    public static Map<String, String> args(String spec) {
        Map<String, String> m = new HashMap<>();
        if (spec == null) return m;
        for (String part : spec.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) m.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
        }
        return m;
    }

    // ------------------------------------------------------------------ rendering

    /** Renders the children of a PreferenceScreen into page. */
    public void render(PrefItem screen, LinearLayout page) {
        LinearLayout loose = null;   // rows directly under the screen go into an untitled card
        for (PrefItem child : screen.children) {
            if ("PreferenceCategory".equals(child.tag)) {
                loose = null;
                LinearLayout card = ui.group(page, child.attr("title"));
                View caption = captionOf(page, card);
                for (PrefItem row : child.children) renderRow(row, card);
                dependOn(child.attr("dependency"), card, caption);
            } else {
                if (loose == null) loose = ui.group(page, null);
                renderRow(child, loose);
            }
        }
        for (String key : dependents.keySet()) applyDependency(key);
    }

    private View captionOf(LinearLayout page, View card) {
        int i = page.indexOfChild(card);
        return i > 0 && page.getChildAt(i - 1) instanceof TextView ? page.getChildAt(i - 1) : null;
    }

    private void renderRow(PrefItem it, LinearLayout card) {
        String req = it.attr("requires");
        if ("root".equals(req) && !store.hasRoot()) { rootNotice(it, card); return; }
        // theme: root, or a ROM that lets this app write /data/system/theme (icon packs)
        if ("theme".equals(req) && !store.hasRoot()
                && !com.miui.home.kashi.ext.modules.IconPacks.canWriteThemeDirect()) { rootNotice(it, card); return; }
        switch (it.tag) {
            case "SwitchPreference": switchRow(it, card); break;
            case "SeekBarPreference": sliderRow(it, card); break;
            case "DropDownPreference": dropdownRow(it, card); break;
            case "Preference": actionRow(it, card); break;
            default: break;
        }
    }

    private void switchRow(PrefItem it, LinearLayout card) {
        final String key = it.key(), controls = it.attr("controls");
        final int on = it.intAttr("onValue", 1), off = it.intAttr("offValue", 0);
        boolean checked;
        if (controls != null) {
            checked = false;
            for (String k : controls.split(",")) if (store.get(k.trim(), 0) != 0) checked = true;
        } else {
            checked = store.get(key, it.intAttr("defaultValue", 0) != 0 ? on : off) == on;
        }
        Ui.Toggle t = ui.switchRow(card, it.title(), it.summary(), checked, value -> {
            if (controls != null) {
                for (String k : controls.split(",")) {
                    Ui.Slider s = sliders.get(k.trim());
                    store.put(k.trim(), value && s != null ? sliderValue(s, k.trim()) : 0);
                }
            } else store.put(key, value ? on : off);
            switchStates.put(controls != null ? "@" + controls : key, value);
            applyDependency(controls != null ? "@" + controls : key);
            host.markDirty();
        });
        switchStates.put(controls != null ? "@" + controls : key, t.checked);
        dependOn(it.attr("dependency"), t.row, null);
    }

    private final Map<String, Boolean> switchStates = new HashMap<>();
    private final Map<String, Integer> zeroAs = new HashMap<>();

    private int sliderValue(Ui.Slider s, String key) {
        Integer z = zeroAs.get(key);
        return s.value == 0 && z != null ? z : s.value;
    }

    private void sliderRow(PrefItem it, LinearLayout card) {
        final String key = it.key();
        int min = it.intAttr("min", 0), max = it.intAttr("max", 100), def = it.intAttr("defaultValue", min);
        String unit = it.attr("unit", "");
        if (it.attr("zeroAs") != null) zeroAs.put(key, it.intAttr("zeroAs", 1));
        int stored = store.get(key, def);
        // a virtual switch stores 0 while off: show the default instead of 0 in that case
        if (stored == 0 && it.attr("dependency") != null && it.attr("dependency").startsWith("@")) stored = def;
        Ui.Slider s = ui.sliderRow(card, it.title(), min, max, stored, def, unit, v -> { store.put(key, sliderValueFor(key, v)); host.markDirty(); });
        if ("signed".equals(it.attr("format"))) {
            s.format = v -> (v > 0 ? "+" : "") + v + unit;
            s.onChange.accept(s.value);
        }
        sliders.put(key, s);
        dependOn(it.attr("dependency"), s.row, null);
    }

    private int sliderValueFor(String key, int v) {
        Integer z = zeroAs.get(key);
        return v == 0 && z != null ? z : v;
    }

    private void dropdownRow(PrefItem it, LinearLayout card) {
        final String key = it.key();
        String[] entries = it.attr("entries", "").split("\\|");
        String[] values = it.attr("entryValues", "").split("\\|");
        int current = store.get(key, it.intAttr("defaultValue", 0)), sel = 0;
        for (int i = 0; i < values.length; i++) if (values[i].trim().equals(String.valueOf(current))) sel = i;
        View row = ui.dropdownRow(card, it.title(), it.summary(), entries, sel, i -> {
            store.put(key, Integer.parseInt(values[i].trim()));
            host.markDirty();
        });
        dependOn(it.attr("dependency"), row, null);
    }

    private void actionRow(PrefItem it, LinearLayout card) {
        final Action action = it.attr("action") != null ? Modules.get(it.attr("action")) : null;
        final String page = it.attr("page"), url = it.attr("url");
        String value = action != null ? action.value(host, it) : null;
        final View[] row = new View[1];
        View.OnClickListener click = null;   // page > action > url; the action also supplies the value text
        if (page != null) click = v -> host.openPage(page, it.attr("pageTitle", it.title()), args(it.attr("args")));
        else if (action != null) click = v -> action.run(host, it);
        else if (url != null) click = v -> Modules.openUrl(host, url);
        else if (it.attr("component") != null) click = v -> Modules.openComponent(host, it.attr("component"), it.attr("intentAction"));
        row[0] = ui.arrowRow(card, it.title(), it.summary(), value, click);
        if (action != null) valueRefresh.put(row[0], () -> Ui.setValue(row[0], action.value(host, it)));
        dependOn(it.attr("dependency"), row[0], null);
    }

    /** Placeholder for a root-only row while root is not granted (system-app mode): tapping asks for root. */
    private void rootNotice(PrefItem it, LinearLayout card) {
        ui.arrowRow(card, it.title(), L.s(host.activity(), "root_needed_sum"), L.s(host.activity(), "root_badge"), v -> {
            host.toast(L.s(host.activity(), "requesting_root"));
            store.io.execute(() -> {
                boolean ok = SettingsStore.requestRoot();
                store.setRoot(ok);
                host.post(() -> {
                    host.toast(L.s(host.activity(), ok ? "root_granted" : "root_denied"));
                    if (ok) host.reloadPage();
                });
            });
        });
    }

    /** Refreshes the value text of action rows (after returning from a sub page or a picker). */
    public void refreshValues() { for (Runnable r : valueRefresh.values()) r.run(); }

    // ------------------------------------------------------------------ dependencies

    private void dependOn(String key, View v, View extra) {
        if (key == null || key.isEmpty()) return;
        List<View> list = dependents.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(v);
        if (extra != null) list.add(extra);
    }

    private void applyDependency(String key) {
        List<View> views = dependents.get(key);
        if (views == null) return;
        Boolean on = switchStates.get(key);
        boolean visible = on != null ? on : store.get(key, 0) != 0;
        for (View v : views) Ui.hide(v, !visible);
    }
}

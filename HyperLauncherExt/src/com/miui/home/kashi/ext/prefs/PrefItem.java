package com.miui.home.kashi.ext.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One element of a preference XML: its tag, its attributes (after ${arg} substitution) and its children. */
public final class PrefItem {
    public final String tag;
    public final Map<String, String> attrs = new HashMap<>();
    public final List<PrefItem> children = new ArrayList<>();

    public PrefItem(String tag) { this.tag = tag; }

    public String attr(String name) { return attrs.get(name); }
    public String attr(String name, String def) { String v = attrs.get(name); return v != null ? v : def; }
    public int intAttr(String name, int def) {
        String v = attrs.get(name);
        if (v == null) return def;
        if ("true".equals(v)) return 1;
        if ("false".equals(v)) return 0;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }
    public String key() { return attr("key"); }
    public String title() { return attr("title", ""); }
    public String summary() { return attr("summary"); }
}

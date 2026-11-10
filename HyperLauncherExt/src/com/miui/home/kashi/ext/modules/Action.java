package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;

/**
 * A feature behind a &lt;Preference action="id"/&gt; row. Register new ones in {@link Modules}.
 * value() supplies the grey text on the right of the row (null = none).
 */
public interface Action {
    void run(Host host, PrefItem item);
    default String value(Host host, PrefItem item) { return null; }
}

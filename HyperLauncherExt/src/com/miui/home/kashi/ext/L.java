package com.miui.home.kashi.ext;

import android.content.Context;

/** Localized strings by resource name (res/values-xx/strings.xml); the build has no generated R class. */
public final class L {
    private L() { }

    public static String s(Context c, String name, Object... args) {
        int id = c.getResources().getIdentifier(name, "string", c.getPackageName());
        if (id == 0) return name;
        return args.length == 0 ? c.getString(id) : c.getString(id, args);
    }
}

package com.miui.home.kashi.ext.prefs;

import android.app.Activity;
import android.content.Intent;

import com.miui.home.kashi.ext.ui.Ui;

import java.util.Map;

/** What modules and the preference renderer may use from the activity. */
public interface Host {
    Activity activity();
    Ui ui();
    SettingsStore store();
    void toast(String message);
    void post(Runnable r);
    /** Marks unapplied launcher changes (red dot on the restart button). */
    void markDirty();
    /** Opens a preference XML (res/xml/&lt;name&gt;.xml) as a sub page; args replace ${name} in its attributes. */
    void openPage(String xml, String title, Map<String, String> args);
    /** Re-renders the current page (after an import, reset, …). */
    void reloadPage();
    void startForResult(Intent intent, ResultCallback callback);

    interface ResultCallback { void onResult(int resultCode, Intent data); }
}

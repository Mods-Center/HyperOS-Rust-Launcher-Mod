package com.miui.home.kashi.ext;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.miui.home.kashi.ext.modules.SearchModule;

/**
 * Invisible trampoline started by the patched launcher:
 *   com.miui.home.kashi.ext.SEARCH_OPEN - home search bar tapped
 *   com.miui.home.kashi.ext.VOICE_GO    - XiaoAi / mic icon of the search bar tapped
 * Opens the app picked in HyperLauncherExt, on its search (or voice search) screen when it has one.
 */
public class SearchActivity extends Activity {
    static final String ACTION_VOICE = "com.miui.home.kashi.ext.VOICE_GO";

    private boolean launched;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        overridePendingTransition(0, 0);
        // edge to edge with transparent bars: nothing of this window may show while the target app starts
        android.view.Window w = getWindow();
        w.setDecorFitsSystemWindows(false);
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        w.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        w.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        w.setStatusBarContrastEnforced(false);
        w.setNavigationBarContrastEnforced(false);
    }

    // Started from onResume: once this activity is the resumed top activity its app counts as foreground, so the
    // system's 5 s app-switch protection (armed by the Home gesture) no longer queues the start of the target app.
    // As a priv-app, STOP_APP_SWITCHES lifts it entirely.
    @Override protected void onResume() {
        super.onResume();
        if (launched) return;
        launched = true;
        getWindow().getDecorView().post(this::open);
    }

    private void open() {
        // Since launcher mod 1.2 the text search also arrives as VOICE_GO (the GLOBAL_SEARCH text stays stock for the
        // native Google mode); the launcher's text intent carries FLAG_ACTIVITY_CLEAR_TASK, its voice intent does not.
        Intent in = getIntent();
        boolean voice = ACTION_VOICE.equals(in.getAction()) && (in.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) == 0;
        String pkg = SearchModule.selected(this);
        Intent target = SearchModule.resolve(this, pkg, voice);
        if (target == null && !SearchModule.GOOGLE.equals(pkg)) target = SearchModule.resolve(this, SearchModule.GOOGLE, voice);
        if (target != null) {
            try { startActivity(target); } catch (Exception e) { target = null; }
        }
        if (target == null) Toast.makeText(this, L.s(this, "pick_search_app"), Toast.LENGTH_SHORT).show();
        finish();
        overridePendingTransition(0, 0);
    }
}

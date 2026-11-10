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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean voice = ACTION_VOICE.equals(getIntent().getAction());
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

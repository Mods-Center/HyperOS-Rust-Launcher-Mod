package com.miui.home.kashi.ext;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.miui.home.kashi.ext.prefs.Host;
import com.miui.home.kashi.ext.prefs.PrefItem;
import com.miui.home.kashi.ext.prefs.PrefScreen;
import com.miui.home.kashi.ext.prefs.SettingsStore;
import com.miui.home.kashi.ext.ui.Ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Shell of HyperLauncherExt: first-run root screen, a stack of preference pages (res/xml/prefs_*.xml rendered by
 * PrefScreen) with HyperOS-style transitions, and the Host services modules use. Features live in modules/.
 */
public class MainActivity extends Activity implements Host {
    static final String APP_NAME = "HyperLauncherExt";
    /** HyperOS-like ease-out used for page and sheet motion. */
    static final PathInterpolator EASE = new PathInterpolator(0.25f, 1f, 0.5f, 1f);

    private final Handler main = new Handler(Looper.getMainLooper());
    private Ui ui;
    private SettingsStore store;
    private FrameLayout stage;
    private final Deque<Page> pages = new ArrayDeque<>();
    private boolean dirty;

    static final class Page {
        String xml, title; Map<String, String> args; View root; PrefScreen screen; Ui.RestartButton restart;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getActionBar() != null) getActionBar().hide();
        ui = new Ui(this);
        store = new SettingsStore(this);
        store.listener = this::toast;
        Window w = getWindow();
        w.setStatusBarColor(ui.bg);
        w.setNavigationBarColor(ui.bg);
        if (!ui.dark) w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        stage = new FrameLayout(this);
        stage.setBackgroundColor(ui.bg);
        setContentView(stage);
        // system/priv-app: settings work without root, so skip the root screen; root-only rows ask for it themselves
        if (store.isDirectWrite() || store.hasRoot()) openPage("prefs_main", APP_NAME, null);
        else showWelcome();
    }

    // ------------------------------------------------------------------ Host

    @Override public Activity activity() { return this; }
    @Override public Ui ui() { return ui; }
    @Override public SettingsStore store() { return store; }
    @Override public void toast(String m) { main.post(() -> Toast.makeText(this, m, Toast.LENGTH_SHORT).show()); }
    @Override public void post(Runnable r) { main.post(r); }

    @Override public void markDirty() {
        dirty = true;
        for (Page p : pages) if (p.restart != null) p.restart.setHighlight(true);
    }

    @Override public void openPage(String xml, String title, Map<String, String> args) {
        Page p = new Page();
        p.xml = xml; p.title = title; p.args = args == null ? new HashMap<>() : args;
        build(p);
        boolean first = pages.isEmpty();
        Page below = pages.peek();
        pages.push(p);
        stage.addView(p.root, Ui.match());
        if (first) { stagger(p); return; }
        // push: new page slides in from the right, the old one drifts left and dims
        float w = stage.getWidth() == 0 ? getResources().getDisplayMetrics().widthPixels : stage.getWidth();
        p.root.setTranslationX(w);
        p.root.animate().translationX(0).setDuration(420).setInterpolator(EASE).start();
        below.root.animate().translationX(-w * 0.25f).alpha(0.6f).setDuration(420).setInterpolator(EASE).start();
        stagger(p);
    }

    @Override public void reloadPage() {
        Page p = pages.peek();
        if (p == null) return;
        int index = stage.indexOfChild(p.root);
        View old = p.root;
        build(p);
        stage.addView(p.root, index, Ui.match());
        stage.removeView(old);
    }

    private void closePage() {
        Page top = pages.pop();
        Page below = pages.peek();
        float w = stage.getWidth();
        top.root.animate().translationX(w).setDuration(360).setInterpolator(EASE).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { stage.removeView(top.root); }
        }).start();
        below.root.animate().translationX(0).alpha(1f).setDuration(360).setInterpolator(EASE).start();
        if (below.screen != null) below.screen.refreshValues();
    }

    @Override public void onBackPressed() {
        if (pages.size() > 1) closePage(); else super.onBackPressed();
    }

    private final Map<Integer, ResultCallback> results = new HashMap<>();
    private int nextRequest = 100;

    @Override public void startForResult(Intent intent, ResultCallback callback) {
        int req = nextRequest++;
        results.put(req, callback);
        startActivityForResult(intent, req);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        ResultCallback cb = results.remove(req);
        if (cb != null && result == RESULT_OK) cb.onResult(result, data);
    }

    // ------------------------------------------------------------------ pages

    private void build(Page p) {
        PrefItem screen;
        try { screen = PrefScreen.parse(this, p.xml, p.args); }
        catch (Exception e) { toast(L.s(this, "err_load_page", p.xml, e.getMessage())); screen = new PrefItem("PreferenceScreen"); }
        boolean sub = !pages.isEmpty() && pages.peekLast() != p;
        Screen sc = makeScreen(p.title != null ? p.title : screen.title(), screen.summary(), sub ? this::closePage : null, p);
        p.screen = new PrefScreen(this);
        p.screen.render(screen, sc.page);
        p.root = sc.root;
    }

    /** Fades and lifts the cards in one after another. */
    private void stagger(Page p) {
        LinearLayout page = (LinearLayout) ((ScrollView) ((FrameLayout) p.root).getChildAt(0)).getChildAt(0);
        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(ui.dp(18));
            v.animate().alpha(1f).translationY(0).setStartDelay(60 + Math.min(i, 14) * 22L).setDuration(380).setInterpolator(EASE).start();
        }
    }

    static final class Screen { FrameLayout root; LinearLayout page; }

    /** Page with a large title that collapses into a blurred top bar while scrolling. back != null adds a back arrow. */
    private Screen makeScreen(String name, String subtitle, Runnable back, Page owner) {
        Screen sc = new Screen();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ui.bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, ui.dp(56), 0, ui.dp(40));
        scroll.addView(page);
        root.addView(scroll, Ui.match());

        TextView title = ui.label(name, 30, ui.text, true);
        title.setPadding(ui.dp(28), ui.dp(12), ui.dp(28), ui.dp(4));
        page.addView(title);
        if (subtitle != null) {
            TextView sub = ui.label(subtitle, 14, ui.summary, false);
            sub.setPadding(ui.dp(28), 0, ui.dp(28), ui.dp(4));
            page.addView(sub);
        }

        FrameLayout bar = new FrameLayout(this);
        Ui.BlurBar glass = new Ui.BlurBar(ui, scroll, page);
        bar.addView(glass, Ui.match());
        TextView small = ui.label(name, 17, ui.text, true);
        small.setGravity(Gravity.CENTER);
        small.setAlpha(0);
        bar.addView(small, Ui.frame(-1, -1, Gravity.CENTER));
        if (back != null) {
            View arrow = new Ui.BackArrow(ui);
            arrow.setOnClickListener(v -> back.run());
            FrameLayout.LayoutParams alp = Ui.frame(ui.dp(40), ui.dp(40), Gravity.START | Gravity.CENTER_VERTICAL);
            alp.setMarginStart(ui.dp(14));
            bar.addView(arrow, alp);
        }
        if (owner != null) {
            Ui.RestartButton rb = new Ui.RestartButton(ui);
            rb.setHighlight(dirty);
            rb.setOnClickListener(v -> restartLauncher());
            FrameLayout.LayoutParams rlp = Ui.frame(ui.dp(40), ui.dp(40), Gravity.END | Gravity.CENTER_VERTICAL);
            rlp.setMarginEnd(ui.dp(14));
            bar.addView(rb, rlp);
            owner.restart = rb;
        }
        root.addView(bar, Ui.frame(-1, ui.dp(56), Gravity.TOP));

        Runnable collapse = () -> {
            float start = title.getBottom() - ui.dp(56) - ui.dp(8);
            float p = Math.max(0, Math.min(1, (scroll.getScrollY() - start) / ui.dp(28)));
            glass.setAlpha(p);
            small.setAlpha(p);
            title.setAlpha(1 - p);
            glass.invalidate();
        };
        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> collapse.run());
        page.addOnLayoutChangeListener((v, a1, b1, c1, d1, a2, b2, c2, d2) -> collapse.run());
        sc.root = root;
        sc.page = page;
        return sc;
    }

    private void restartLauncher() {
        for (Page p : pages) if (p.restart != null) p.restart.animate().rotationBy(-360).setDuration(700).setInterpolator(EASE).start();
        toast(L.s(this, "restarting"));
        store.restartLauncher(() -> main.post(() -> {
            dirty = false;
            for (Page p : pages) if (p.restart != null) p.restart.setHighlight(false);
            toast(L.s(MainActivity.this, "restarted"));
        }));
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        store.io.shutdown();
    }

    // ------------------------------------------------------------------ first run: root permission

    private void showWelcome() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ui.bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(0, ui.dp(72), 0, ui.dp(120));
        scroll.addView(page);
        root.addView(scroll, Ui.match());

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        page.addView(icon, new LinearLayout.LayoutParams(ui.dp(96), ui.dp(96)));
        TextView title = ui.label(APP_NAME, 28, ui.text, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(ui.dp(28), ui.dp(20), ui.dp(28), ui.dp(6));
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView sub = ui.label(L.s(this, "welcome_sub"), 14, ui.summary, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(ui.dp(40), 0, ui.dp(40), ui.dp(8));
        page.addView(sub, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout req = new LinearLayout(this);
        req.setOrientation(LinearLayout.VERTICAL);
        page.addView(req, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout card = ui.group(req, L.s(this, "req_title"));
        Ui.StatusRow rootRow = ui.statusRow(card, L.s(this, "req_root"), L.s(this, "req_root_sum"));
        Ui.StatusRow libRow = ui.statusRow(card, L.s(this, "req_patched"), L.s(this, "req_patched_sum"));
        LinearLayout note = ui.group(req, null);
        TextView why = ui.label(L.s(this, "welcome_why"), 13, ui.summary, false);
        why.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        note.addView(why);

        TextView button = ui.label(L.s(this, "grant_root"), 16, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), ui.round(ui.primary, 26), null));
        FrameLayout.LayoutParams blp = Ui.frame(-1, ui.dp(52), Gravity.BOTTOM);
        blp.setMargins(ui.dp(24), 0, ui.dp(24), ui.dp(28));
        root.addView(button, blp);

        final boolean[] granted = {false};
        Runnable check = () -> {
            rootRow.set(Ui.StatusRow.BUSY, L.s(MainActivity.this, "waiting_root"));
            libRow.set(Ui.StatusRow.BUSY, null);
            button.setEnabled(false);
            button.setAlpha(.6f);
            store.io.execute(() -> {
                boolean ok = false, patched = false;
                String detail = null;
                try {
                    // root is decided by the uid line only; the launcher check never fails the root check
                    SettingsStore.Result r = SettingsStore.su("echo KASHI_UID=$(id -u); id; "
                            + "p=$(pm path " + SettingsStore.LAUNCHER + " 2>/dev/null | head -n1 | cut -d: -f2); "
                            + "if [ -n \"$p\" ] && grep -q libkashidock.so \"$p\" 2>/dev/null; then echo KASHI_PATCHED; fi; exit 0", null);
                    String out = r.text();
                    ok = SettingsStore.isRootOutput(out);
                    patched = out.contains("KASHI_PATCHED");
                    if (!ok) {
                        String t = out.replaceAll("KASHI_[A-Z]+=?\\S*", "").trim();
                        detail = t.isEmpty() ? L.s(MainActivity.this, "root_denied") : t.length() > 160 ? t.substring(0, 160) : t;
                    }
                } catch (Exception e) { detail = L.s(MainActivity.this, "su_missing"); }
                final boolean fOk = ok, fPatched = patched;
                final String fDetail = detail;
                main.post(() -> {
                    granted[0] = fOk;
                    rootRow.set(fOk ? Ui.StatusRow.OK : Ui.StatusRow.FAIL,
                            fOk ? L.s(MainActivity.this, "granted") : L.s(MainActivity.this, "root_allow", fDetail, APP_NAME));
                    libRow.set(!fOk ? Ui.StatusRow.IDLE : fPatched ? Ui.StatusRow.OK : Ui.StatusRow.WARN,
                            !fOk ? null : fPatched ? L.s(MainActivity.this, "detected") : L.s(MainActivity.this, "not_detected"));
                    button.setText(L.s(MainActivity.this, fOk ? "continue_btn" : "try_again"));
                    button.setEnabled(true);
                    button.setAlpha(1f);
                });
            });
        };
        button.setOnClickListener(v -> {
            if (granted[0]) {
                store.setRoot(true);
                root.animate().alpha(0f).setDuration(220).withEndAction(() -> {
                    stage.removeView(root);
                    openPage("prefs_main", APP_NAME, null);
                }).start();
            } else check.run();
        });
        stage.addView(root, Ui.match());
        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            v.setAlpha(0f); v.setTranslationY(ui.dp(24));
            v.animate().alpha(1f).translationY(0).setStartDelay(80 + i * 60L).setDuration(460).setInterpolator(EASE).start();
        }
    }
}

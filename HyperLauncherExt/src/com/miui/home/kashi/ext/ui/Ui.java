package com.miui.home.kashi.ext.ui;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** Small HyperOS / miuix-style widget kit built on plain views (cards, switches, sliders, dropdowns). */
public final class Ui {
    public final Context ctx;
    public final boolean dark;
    public final int bg, card, text, summary, section, primary, track, pressed;

    public Ui(Context ctx) {
        this.ctx = ctx;
        dark = (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg = dark ? 0xFF000000 : 0xFFF7F7F7;
        card = dark ? 0xFF242424 : 0xFFFFFFFF;
        text = dark ? 0xFFE6E6E6 : 0xFF000000;
        summary = dark ? 0xFF8C8C8C : 0xFF8C8C8C;
        section = dark ? 0xFF8C8C8C : 0xFF8C8C8C;
        primary = dark ? 0xFF277AF7 : 0xFF3482FF;
        track = dark ? 0xFF3A3A3A : 0xFFE6E6E6;
        pressed = dark ? 0x1FFFFFFF : 0x0F000000;
    }

    public int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics())); }

    public GradientDrawable round(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    public TextView label(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** A titled group: grey section caption + rounded card. Children go into the returned card. */
    public LinearLayout group(LinearLayout page, String title) {
        if (title != null) {
            TextView t = label(title, 13, section, false);
            t.setPadding(dp(28), dp(18), dp(28), dp(8));
            page.addView(t);
        }
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(card, 16));
        c.setClipToOutline(true);
        c.setLayoutTransition(new LayoutTransition());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), title == null ? dp(12) : 0, dp(12), 0);
        page.addView(c, lp);
        return c;
    }

    private LinearLayout row(LinearLayout parent, String title, String sum, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(dp(64));
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (click != null) {
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed), null, new ColorDrawable(Color.WHITE)));
            r.setOnClickListener(click);
            pressScale(r);
        }
        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = label(title, 16, text, true);
        texts.addView(t);
        if (sum != null) {
            TextView s = label(sum, 13, summary, false);
            s.setPadding(0, dp(2), 0, 0);
            s.setTag("summary");
            texts.addView(s);
        }
        r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(r, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    public static final android.view.animation.PathInterpolator EASE = new android.view.animation.PathInterpolator(0.25f, 1f, 0.5f, 1f);

    /** Subtle press feedback: the row shrinks a little while touched (HyperOS list style). */
    public static void pressScale(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: view.animate().scaleX(.97f).scaleY(.97f).setDuration(120).setInterpolator(EASE).start(); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: view.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start(); break;
            }
            return false;
        });
    }

    public static void setSummary(View row, String s) {
        View v = row.findViewWithTag("summary");
        if (v instanceof TextView) ((TextView) v).setText(s);
    }

    public Toggle switchRow(LinearLayout parent, String title, String sum, boolean on, Toggle.Listener l) {
        Toggle tg = new Toggle(this);
        LinearLayout r = row(parent, title, sum, v -> tg.toggle());
        tg.setChecked(on, false);
        tg.listener = l;
        r.addView(tg, new LinearLayout.LayoutParams(dp(49), dp(28)));
        tg.row = r;
        return tg;
    }

    public View arrowRow(LinearLayout parent, String title, String sum, String value, View.OnClickListener click) {
        LinearLayout r = row(parent, title, sum, click);
        if (value != null) {
            TextView v = label(value, 14, summary, false);
            v.setTag("value");
            v.setMaxWidth(dp(140));
            v.setSingleLine(true);
            v.setEllipsize(TextUtils.TruncateAt.END);
            v.setPadding(dp(8), 0, dp(4), 0);
            r.addView(v);
        }
        r.addView(new Chevron(this, false), new LinearLayout.LayoutParams(dp(14), dp(14)));
        return r;
    }

    public static void setValue(View row, String s) {
        View v = row.findViewWithTag("value");
        if (v instanceof TextView) ((TextView) v).setText(s);
    }

    /** miuix OverlayDropdownPreference: value on the right, popup list anchored to the row. */
    public View dropdownRow(LinearLayout parent, String title, String sum, String[] items, int selected, IntConsumer onSelect) {
        final int[] sel = {selected};
        final LinearLayout[] holder = new LinearLayout[1];
        LinearLayout r = row(parent, title, sum, v -> showPopup(holder[0], items, sel[0], i -> {
            sel[0] = i;
            setValue(holder[0], items[i]);
            onSelect.accept(i);
        }));
        holder[0] = r;
        TextView v = label(items[Math.max(0, Math.min(items.length - 1, selected))], 14, summary, false);
        v.setTag("value");
        v.setPadding(dp(8), 0, dp(6), 0);
        r.addView(v);
        r.addView(new Chevron(this, true), new LinearLayout.LayoutParams(dp(10), dp(16)));
        return r;
    }

    public void showPopup(View anchor, String[] items, int selected, IntConsumer onSelect) {
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(8));
        list.setBackground(round(card, 18));
        list.setElevation(dp(12));
        PopupWindow pw = new PopupWindow(list, dp(200), -2, true);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            TextView t = label(items[i], 16, i == selected ? primary : text, i == selected);
            t.setPadding(dp(20), dp(14), dp(20), dp(14));
            t.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed), null, new ColorDrawable(Color.WHITE)));
            t.setOnClickListener(v -> { pw.dismiss(); onSelect.accept(idx); });
            list.addView(t, new LinearLayout.LayoutParams(-1, -2));
        }
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setElevation(dp(12));
        pw.setOutsideTouchable(true);
        pw.setAnimationStyle(0);
        pw.showAsDropDown(anchor, anchor.getWidth() - dp(212), -dp(8), Gravity.NO_GRAVITY);
        list.setPivotX(dp(200)); list.setPivotY(0);
        list.setScaleX(.8f); list.setScaleY(.8f); list.setAlpha(0f);
        list.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260).setInterpolator(EASE).start();
    }

    /** miuix ParameterIntSlide: title + value on one line, thick rounded slider below. */
    public Slider sliderRow(LinearLayout parent, String title, int min, int max, int value, String unit, IntConsumer onDone) {
        return sliderRow(parent, title, min, max, value, Integer.MIN_VALUE, unit, onDone);
    }

    /** Slider row: title, reset glyph (when def is set and differs), tappable value (opens a number dialog), - slider +. */
    public Slider sliderRow(LinearLayout parent, String title, int min, int max, int value, int def, String unit, IntConsumer onDone) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(12), dp(14));
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(label(title, 16, text, true), new LinearLayout.LayoutParams(0, -2, 1));
        Glyph reset = new Glyph(this, Glyph.RESET);
        head.addView(reset, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView val = label("", 14, primary, true);
        val.setPadding(dp(10), dp(6), dp(10), dp(6));
        val.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed), round(track, 12), null));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-2, -2);
        vlp.leftMargin = dp(4);
        head.addView(val, vlp);
        box.addView(head);
        Slider s = new Slider(this, min, max, value);
        s.def = def; s.unit = unit;
        s.onChange = v -> {
            val.setText(s.format != null ? s.format.apply(v) : v + unit);
            reset.setVisibility(def != Integer.MIN_VALUE && v != Math.max(min, Math.min(max, def)) ? View.VISIBLE : View.INVISIBLE);
        };
        s.onDone = onDone;
        s.onChange.accept(s.value);
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Glyph minus = new Glyph(this, Glyph.MINUS), plus = new Glyph(this, Glyph.PLUS);
        bar.addView(minus, new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(28), 1);
        slp.leftMargin = dp(8); slp.rightMargin = dp(8);
        bar.addView(s, slp);
        bar.addView(plus, new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = dp(8);
        box.addView(bar, blp);
        minus.setOnClickListener(v -> s.commit(s.value - 1));
        plus.setOnClickListener(v -> s.commit(s.value + 1));
        minus.setOnLongClickListener(v -> repeat(v, () -> s.commit(s.value - 1)));
        plus.setOnLongClickListener(v -> repeat(v, () -> s.commit(s.value + 1)));
        reset.setOnClickListener(v -> s.commit(def));
        val.setOnClickListener(v -> numberDialog(title, s));
        pressScale(minus); pressScale(plus); pressScale(reset);
        parent.addView(box, new LinearLayout.LayoutParams(-1, -2));
        s.row = box;
        return s;
    }

    /** Auto-repeat while a -/+ button stays pressed. */
    private boolean repeat(View v, Runnable step) {
        Runnable[] tick = new Runnable[1];
        tick[0] = () -> { if (v.isPressed()) { step.run(); v.postDelayed(tick[0], 70); } };
        v.post(tick[0]);
        return true;
    }

    /** Small dialog with a number field; OK clamps into the slider range and commits. */
    public void numberDialog(String title, Slider s) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(16));
        root.setBackground(round(bg, 28));
        TextView t = label(title, 18, text, true);
        root.addView(t);
        TextView hint = label(com.miui.home.kashi.ext.L.s(ctx, "range", s.min + " \u2026 " + s.max + (s.unit.isEmpty() ? "" : " " + s.unit.trim())), 13, summary, false);
        hint.setPadding(0, dp(4), 0, dp(14));
        root.addView(hint);
        android.widget.EditText e = new android.widget.EditText(ctx);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | (s.min < 0 ? android.text.InputType.TYPE_NUMBER_FLAG_SIGNED : 0));
        e.setText(String.valueOf(s.value));
        e.setSelectAllOnFocus(true);
        e.setTextColor(text);
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setBackground(round(card, 14));
        e.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(e, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout btns = new LinearLayout(ctx);
        btns.setGravity(Gravity.END);
        btns.setPadding(0, dp(16), 0, 0);
        TextView cancel = label(com.miui.home.kashi.ext.L.s(ctx, "cancel"), 15, summary, true), ok = label(com.miui.home.kashi.ext.L.s(ctx, "ok"), 15, primary, true);
        for (TextView b : new TextView[] {cancel, ok}) {
            b.setPadding(dp(18), dp(10), dp(18), dp(10));
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed), round(0, 18), round(0xff000000, 18)));
            btns.addView(b);
        }
        root.addView(btns, new LinearLayout.LayoutParams(-1, -2));
        Runnable apply = () -> {
            try { s.commit(Integer.parseInt(e.getText().toString().trim())); d.dismiss(); }
            catch (NumberFormatException x) { e.setError(com.miui.home.kashi.ext.L.s(ctx, "whole_number")); }
        };
        cancel.setOnClickListener(v -> d.dismiss());
        ok.setOnClickListener(v -> apply.run());
        e.setOnEditorActionListener((v, id, ev) -> { apply.run(); return true; });
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            w.setLayout(ctx.getResources().getDisplayMetrics().widthPixels - dp(48), -2);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        d.show();
        e.requestFocus();
    }

    /** Round button glyphs for slider rows: minus, plus, reset. */
    public static final class Glyph extends View {
        public static final int MINUS = 0, PLUS = 1, RESET = 2;
        public final Ui ui;
        public final int kind;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public Glyph(Ui ui, int kind) {
            super(ui.ctx);
            this.ui = ui; this.kind = kind;
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(ui.dp(2));
            setBackground(new RippleDrawable(ColorStateList.valueOf(ui.pressed), kind == RESET ? null : ui.round(ui.track, 18), ui.round(0xff000000, 18)));
            setContentDescription(com.miui.home.kashi.ext.L.s(ui.ctx, kind == MINUS ? "decrease" : kind == PLUS ? "increase" : "reset_default"));
        }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, a = Math.min(w, h) * .2f;
            p.setStyle(Paint.Style.STROKE);
            p.setColor(kind == RESET ? ui.summary : ui.text);
            if (kind == RESET) {
                float r = Math.min(w, h) * .24f;
                c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), 200, 280, false, p);
                p.setStyle(Paint.Style.FILL);
                float hx = cx + r * (float) Math.cos(Math.toRadians(200)), hy = cy + r * (float) Math.sin(Math.toRadians(200));
                Path head = new Path();
                head.moveTo(hx - ui.dp(3.5f), hy - ui.dp(3));
                head.lineTo(hx + ui.dp(3.5f), hy - ui.dp(2));
                head.lineTo(hx - ui.dp(0.5f), hy + ui.dp(4));
                head.close();
                c.drawPath(head, p);
                return;
            }
            c.drawLine(cx - a, cy, cx + a, cy, p);
            if (kind == PLUS) c.drawLine(cx, cy - a, cx, cy + a, p);
        }
    }

    /** Full-width bottom-sheet style dialog holding a card list. */
    public Dialog sheet(String title, String[] items, String[] sub, android.graphics.drawable.Drawable[] icons, int selected, IntConsumer onSelect) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(20), dp(12), dp(24));
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(bg);
        float r = dp(28);
        sheetBg.setCornerRadii(new float[] {r, r, r, r, 0, 0, 0, 0});
        root.setBackground(sheetBg);
        TextView t = label(title, 18, text, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, dp(16));
        root.addView(t, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(card, 16));
        c.setClipToOutline(true);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            LinearLayout rw = row(c, items[i], sub == null ? null : sub[i], v -> { d.dismiss(); onSelect.accept(idx); });
            if (icons != null) {
                android.widget.ImageView iv = new android.widget.ImageView(ctx);
                if (icons[i] != null) iv.setImageDrawable(icons[i]);
                else iv.setBackground(round(track, 12));
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(40), dp(40));
                ilp.setMarginEnd(dp(14));
                rw.addView(iv, 0, ilp);
            }
            if (i == selected) {
                TextView mark = label("✓", 18, primary, true);
                rw.addView(mark);
            }
        }
        ScrollView sv = new ScrollView(ctx);
        sv.addView(c);
        root.addView(sv, new LinearLayout.LayoutParams(-1, -2));
        d.setContentView(root);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setGravity(Gravity.BOTTOM);
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        w.setDimAmount(0.3f);
        w.setWindowAnimations(0);
        d.setOnShowListener(x -> {
            root.setTranslationY(root.getHeight() > 0 ? root.getHeight() : dp(400));
            root.animate().translationY(0).setDuration(380).setInterpolator(EASE).start();
        });
        return d;
    }

    // ------------------------------------------------------------------ widgets

    /** miuix switch: rounded track, white knob, animated. */
    public static final class Toggle extends View {
        public interface Listener { void onChange(boolean on); }
        public final Ui ui;
        public Listener listener;
        public View row;
        public boolean checked;
        public float pos;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public Toggle(Ui ui) { super(ui.ctx); this.ui = ui; }

        public void setChecked(boolean on, boolean animate) {
            checked = on;
            if (!animate) { pos = on ? 1 : 0; invalidate(); return; }
            ValueAnimator a = ValueAnimator.ofFloat(pos, on ? 1 : 0);
            a.setDuration(320);
            a.setInterpolator(new android.view.animation.OvershootInterpolator(1.6f));
            a.addUpdateListener(an -> { pos = (float) an.getAnimatedValue(); invalidate(); });
            a.start();
        }

        public void toggle() {
            setChecked(!checked, true);
            if (listener != null) listener.onChange(checked);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) toggle();
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setColor(blend(ui.track, ui.primary, Math.max(0f, Math.min(1f, pos))));
            c.drawRoundRect(new RectF(0, 0, w, h), h / 2, h / 2, p);
            float inset = ui.dp(4), d = h - inset * 2;
            float x = inset + (w - inset * 2 - d) * pos;
            p.setColor(Color.WHITE);
            c.drawOval(new RectF(x, inset, x + d, inset + d), p);
        }

        public static int blend(int a, int b, float t) {
            int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bgc = (b >> 8) & 255, bb = b & 255;
            return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bgc - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
        }
    }

    /** miuix slider: 28dp rounded track filled from the left, no thumb. */
    public static final class Slider extends View {
        public final Ui ui;
        public final int min, max;
        public int value;
        public IntConsumer onChange, onDone;
        public java.util.function.IntFunction<String> format;
        public View row;
        public int def = Integer.MIN_VALUE;
        public String unit = "";
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public Slider(Ui ui, int min, int max, int value) {
            super(ui.ctx);
            this.ui = ui; this.min = min; this.max = max;
            this.value = Math.max(min, Math.min(max, value));
        }

        public void set(int v) { value = Math.max(min, Math.min(max, v)); if (onChange != null) onChange.accept(value); invalidate(); }

        /** Sets the value and saves it, as if the finger was lifted there (buttons, dialog, reset). */
        public void commit(int v) {
            int old = value;
            set(v);
            if (value != old) performHapticFeedback(4);
            if (onDone != null) onDone.accept(value);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float f = Math.max(0, Math.min(1, e.getX() / Math.max(1, getWidth())));
            int v = Math.round(min + f * (max - min));
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN: getParent().requestDisallowInterceptTouchEvent(true); // fall through
                case MotionEvent.ACTION_MOVE:
                    if (v != value) { value = v; if (onChange != null) onChange.accept(v); performHapticFeedback(4); invalidate(); }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (onDone != null) onDone.accept(value);
                    return true;
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = h / 2;
            p.setColor(ui.track);
            c.drawRoundRect(new RectF(0, 0, w, h), r, r, p);
            float f = max == min ? 0 : (value - min) / (float) (max - min);
            float fw = Math.max(h, w * f);
            p.setColor(ui.primary);
            c.save();
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, w, h), r, r, Path.Direction.CW);
            c.clipPath(clip);
            c.drawRoundRect(new RectF(0, 0, fw, h), r, r, p);
            c.restore();
        }
    }

    /** Small chevron: right arrow, or up/down pair for dropdowns. */
    public static final class Chevron extends View {
        public final Ui ui;
        public final boolean updown;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public Chevron(Ui ui, boolean updown) {
            super(ui.ctx);
            this.ui = ui; this.updown = updown;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(ui.dp(1.6f));
            p.setColor(ui.summary);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            Path path = new Path();
            if (updown) {
                path.moveTo(w * .15f, h * .38f); path.lineTo(w * .5f, h * .15f); path.lineTo(w * .85f, h * .38f);
                path.moveTo(w * .15f, h * .62f); path.lineTo(w * .5f, h * .85f); path.lineTo(w * .85f, h * .62f);
            } else {
                path.moveTo(w * .35f, h * .15f); path.lineTo(w * .7f, h * .5f); path.lineTo(w * .35f, h * .85f);
            }
            c.drawPath(path, p);
        }
    }

    /** Round icon button with a circular-arrow "restart" glyph. */
    public static final class RestartButton extends View {
        public final Ui ui;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public boolean highlight;

        public RestartButton(Ui ui) {
            super(ui.ctx);
            this.ui = ui;
            setBackground(new RippleDrawable(ColorStateList.valueOf(ui.pressed), ui.round(ui.card, 22), null));
            setElevation(ui.dp(1));
        }

        public void setHighlight(boolean on) { highlight = on; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) * .24f, cx = w / 2, cy = h / 2;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(ui.dp(2));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(highlight ? ui.primary : ui.text);
            c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -60, 300, false, p);
            p.setStyle(Paint.Style.FILL);
            Path head = new Path();
            float hx = cx + r * (float) Math.cos(Math.toRadians(-60)), hy = cy + r * (float) Math.sin(Math.toRadians(-60));
            head.moveTo(hx + ui.dp(4), hy - ui.dp(1));
            head.lineTo(hx - ui.dp(2), hy - ui.dp(4));
            head.lineTo(hx - ui.dp(1), hy + ui.dp(3));
            head.close();
            c.drawPath(head, p);
            if (highlight) {
                p.setColor(0xFFFF3B30);
                c.drawCircle(w - ui.dp(11), ui.dp(11), ui.dp(4), p);
            }
        }
    }

    /** Back chevron for sub pages. */
    public static final class BackArrow extends View {
        public final Ui ui;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public BackArrow(Ui ui) {
            super(ui.ctx);
            this.ui = ui;
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(ui.dp(2)); p.setColor(ui.text);
            setBackground(new RippleDrawable(ColorStateList.valueOf(ui.pressed), ui.round(ui.card, 20), null));
        }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            Path path = new Path();
            path.moveTo(w * .56f, h * .3f); path.lineTo(w * .38f, h * .5f); path.lineTo(w * .56f, h * .7f);
            c.drawPath(path, p);
        }
    }

    /** Collapsed-bar background: draws the scrolled content underneath, blurred, under a translucent tint. */
    public static final class BlurBar extends View {
        public final Ui ui;
        public final ScrollView scroll;
        public final View content;
        public final Paint tint = new Paint();
        public final Paint line = new Paint();

        public BlurBar(Ui ui, ScrollView scroll, View content) {
            super(ui.ctx);
            this.ui = ui; this.scroll = scroll; this.content = content;
            tint.setColor((ui.bg & 0x00FFFFFF) | 0xB8000000);
            line.setColor(ui.dark ? 0x22FFFFFF : 0x14000000);
            if (android.os.Build.VERSION.SDK_INT >= 31)
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(ui.dp(24), ui.dp(24), android.graphics.Shader.TileMode.CLAMP));
            setAlpha(0);
        }

        @Override protected void onDraw(Canvas c) {
            c.drawColor(ui.bg);
            c.save();
            c.clipRect(0, 0, getWidth(), getHeight());
            c.translate(0, -scroll.getScrollY());
            content.draw(c);
            c.restore();
            c.drawRect(0, 0, getWidth(), getHeight(), tint);
        }

        @Override protected void dispatchDraw(Canvas c) {
            super.dispatchDraw(c);
            c.drawRect(0, getHeight() - 1, getWidth(), getHeight(), line);
        }
    }

    /** Requirement row with a coloured status dot and a status line. */
    public static final class StatusRow {
        public static final int IDLE = 0, BUSY = 1, OK = 2, FAIL = 3, WARN = 4;
        public final Ui ui; final TextView status; final View dot; final String idleText;
        public StatusRow(Ui ui, TextView status, View dot, String idleText) { this.ui = ui; this.status = status; this.dot = dot; this.idleText = idleText; }
        public void set(int state, String text) {
            int c = state == OK ? 0xFF34C759 : state == FAIL ? 0xFFFF3B30 : state == WARN ? 0xFFFF9500 : state == BUSY ? ui.primary : ui.track;
            dot.setBackground(ui.round(c, 6));
            status.setText(text != null ? text : state == BUSY ? com.miui.home.kashi.ext.L.s(ui.ctx, "checking") : idleText);
            status.setTextColor(state == FAIL ? 0xFFFF3B30 : state == WARN ? 0xFFFF9500 : ui.summary);
        }
    }

    public StatusRow statusRow(LinearLayout parent, String title, String idle) {
        LinearLayout r = row(parent, title, idle, null);
        View dot = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(12), dp(12));
        lp.setMarginStart(dp(12));
        r.addView(dot, lp);
        StatusRow s = new StatusRow(this, (TextView) r.findViewWithTag("summary"), dot, idle);
        s.set(StatusRow.IDLE, null);
        return s;
    }

    public static FrameLayout.LayoutParams frame(int w, int h, int gravity) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.gravity = gravity;
        return lp;
    }

    public static void hide(View v, boolean hidden) { if (v != null) v.setVisibility(hidden ? View.GONE : View.VISIBLE); }

    public static ViewGroup.LayoutParams match() { return new ViewGroup.LayoutParams(-1, -1); }
}

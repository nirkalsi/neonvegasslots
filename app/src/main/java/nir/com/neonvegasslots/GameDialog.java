package nir.com.neonvegasslots;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Themed, content-sized popup for the game: rounded card on a dimmed backdrop,
 * clear title/body hierarchy, full-width wrap-safe buttons, scrolls when tall.
 * Visual only - no economy, payout or purchase logic lives here.
 */
final class GameDialog {

    /** Color set for one dialog skin. */
    static final class Palette {
        final int panelBg, panelLine, neon, title, soft, accent, accentText;
        Palette(int panelBg, int panelLine, int neon, int title, int soft, int accent, int accentText) {
            this.panelBg = panelBg; this.panelLine = panelLine; this.neon = neon;
            this.title = title; this.soft = soft; this.accent = accent; this.accentText = accentText;
        }
    }

    static Palette fromTheme(SlotArt.Theme th) {
        return new Palette(th.panelBg, th.panelLine, th.neon, th.title, th.soft, th.spinBg, Color.WHITE);
    }

    static final class Btn {
        final String label; final boolean primary; final Runnable onClick;
        private Btn(String label, boolean primary, Runnable onClick) { this.label = label; this.primary = primary; this.onClick = onClick; }
        static Btn primary(String label, Runnable r) { return new Btn(label, true, r); }
        static Btn secondary(String label, Runnable r) { return new Btn(label, false, r); }
    }

    static final class Builder {
        private final Activity activity;
        private final Palette theme;
        private String title; private CharSequence body; private View content;
        private boolean cancelable = true;
        private final List<Btn> buttons = new ArrayList<>();

        Builder(Activity a, Palette p) { activity = a; theme = p; }
        Builder title(String t) { title = t; return this; }
        Builder body(CharSequence b) { body = b; return this; }
        Builder content(View v) { content = v; return this; }
        Builder cancelable(boolean c) { cancelable = c; return this; }
        Builder add(Btn b) { buttons.add(b); return this; }

        Dialog show() { Dialog d = build(); d.show(); return d; }

        Dialog build() {
            final Dialog d = new Dialog(activity);
            float density = activity.getResources().getDisplayMetrics().density;
            int pad = (int) (20 * density), gap = (int) (12 * density);

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(pad, pad, pad, pad);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(theme.panelBg);
            bg.setCornerRadius(22 * density);
            bg.setStroke((int) (1.5f * density), withAlpha(theme.neon, 0x66));
            card.setBackground(bg);

            if (title != null) {
                TextView t = new TextView(activity);
                t.setText(title); t.setTextSize(20f); t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setTextColor(theme.title); t.setSingleLine(false);
                card.addView(t);
            }
            if (body != null) {
                TextView b = new TextView(activity);
                b.setText(body); b.setTextSize(15f); b.setTextColor(theme.soft);
                b.setLineSpacing(0, 1.15f); b.setSingleLine(false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = (int) (8 * density);
                card.addView(b, lp);
            }
            if (content != null) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = gap;
                card.addView(content, lp);
            }
            for (final Btn btn : buttons) {
                Button b = new Button(activity);
                b.setText(btn.label); b.setTextSize(15f); b.setTypeface(Typeface.DEFAULT_BOLD);
                b.setSingleLine(false); // wrap long labels instead of clipping
                b.setMinHeight((int) (48 * density)); b.setMinimumHeight((int) (48 * density));
                b.setAllCaps(false);
                if (btn.primary) {
                    GradientDrawable pb = new GradientDrawable();
                    pb.setColor(theme.accent); pb.setCornerRadius(14 * density);
                    b.setBackground(pb); b.setTextColor(theme.accentText);
                } else {
                    GradientDrawable sb = new GradientDrawable();
                    sb.setColor(withAlpha(theme.panelLine, 0x33)); sb.setCornerRadius(14 * density);
                    sb.setStroke((int) (1 * density), withAlpha(theme.neon, 0x55));
                    b.setBackground(sb); b.setTextColor(theme.neon);
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = gap;
                card.addView(b, lp);
                b.setOnClickListener(v -> { d.dismiss(); if (btn.onClick != null) btn.onClick.run(); });
            }

            ScrollView sv = new ScrollView(activity);
            sv.addView(card);
            d.setContentView(sv);
            d.setCancelable(cancelable);
            d.setCanceledOnTouchOutside(cancelable);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams attrs = w.getAttributes();
                attrs.dimAmount = 0.6f;
                w.setAttributes(attrs);
            }
            d.setOnShowListener(x -> {
                int sw = activity.getResources().getDisplayMetrics().widthPixels;
                int sh = activity.getResources().getDisplayMetrics().heightPixels;
                int width = (int) (sw * 0.88f);
                d.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
                sv.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                           View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                int max = (int) (sh * 0.78f);
                if (sv.getMeasuredHeight() > max) d.getWindow().setLayout(width, max);
            });
            return d;
        }
    }

    static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }
}

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

    /** Popup queue state (main thread only). */
    static boolean showing = false;
    static android.app.Dialog cur;
    /** True while a popup is on screen. Dialog dismiss callbacks arrive late, so also check the dialog itself. */
    static boolean isOpen() { if (showing && (cur == null || !cur.isShowing())) showing = false; return showing; }
    /** Gameplay actions (spins, merges, taps on the board) since the last popup closed. Starts at 1 so the first popup is not blocked. */
    static int actionsSinceClose = 1;
    static void noteAction() { actionsSinceClose++; }
    static java.util.function.BooleanSupplier busy;
    static final java.util.ArrayDeque<Builder> pending = new java.util.ArrayDeque<>();
    private static boolean drainScheduled = false;
    private static int queueEpoch = 0;
    static void scheduleDrain(Activity a) {
        if (drainScheduled) return;
        drainScheduled = true;
        final int epoch = queueEpoch;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (epoch != queueEpoch) return; // old Activity cannot drain or clear its replacement
            drainScheduled = false;
            if (a.isFinishing() || a.isDestroyed()) { pending.clear(); showing = false; return; }
            Builder b = pending.peek();
            if (b == null) return;
            if (isOpen() || (busy != null && busy.getAsBoolean()) || (b.auto && actionsSinceClose < 1)) { scheduleDrain(a); return; }
            pending.poll().show();
        }, 700);
    }
    static void resetQueue() { queueEpoch++; showing = false; cur = null; actionsSinceClose = 1; pending.clear(); drainScheduled = false; }

    static final class Builder {
        private final Activity activity;
        private final Palette theme;
        private String title; private CharSequence body; private View content;
        private boolean cancelable = true, auto = false;
        private final List<Btn> buttons = new ArrayList<>();

        Builder(Activity a, Palette p) { activity = a; theme = p; }
        Builder title(String t) { title = t; return this; }
        Builder body(CharSequence b) { body = b; return this; }
        Builder content(View v) { content = v; return this; }
        private boolean pinned=false;
        Builder pinnedChrome(){pinned=true;return this;}
        Builder cancelable(boolean c) { cancelable = c; return this; }
        /** Popup the app opens on its own (not from a tap): it also waits for at least one gameplay action since the last popup closed. */
        Builder auto() { auto = true; return this; }
        Builder add(Btn b) { buttons.add(b); return this; }

        Dialog show() {
            Dialog d = build();
            if (activity.isFinishing() || activity.isDestroyed()) return d; // never attach a window to a dead activity (BadTokenException seen on Test Lab)
            // Popup queue: one popup at a time, never while the reels spin; the rest wait their turn.
            if (isOpen() || (busy != null && busy.getAsBoolean()) || (auto && actionsSinceClose < 1)) { pending.add(this); scheduleDrain(activity); return d; }
            showing = true; cur = d;
            d.setOnDismissListener(x -> { if (cur == d) { showing = false; actionsSinceClose = 0; } scheduleDrain(activity); });
            d.show();
            View content = d.getWindow() != null ? d.getWindow().getDecorView() : null;
            if (content != null) {
                content.setAlpha(0f);
                content.setScaleX(0.88f);
                content.setScaleY(0.88f);
                content.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(230)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .start();
            }
            return d;
        }

        Dialog build() {
            final Dialog d = new Dialog(activity);
            float density = activity.getResources().getDisplayMetrics().density;
            int pad = (int) (20 * density), gap = (int) (12 * density);

            LinearLayout card = new LinearLayout(activity);
            ScrollView pinSv=null;LinearLayout.LayoutParams pinLp=null;
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(pad, pad, pad, pad);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(theme.panelBg);
            bg.setCornerRadius(22 * density);
            bg.setStroke((int) (1.5f * density), withAlpha(theme.neon, 0x66));
            card.setBackground(bg);

            if (title != null) {
                LinearLayout header = new LinearLayout(activity);
                header.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable hb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{withAlpha(theme.neon, 0x2E), withAlpha(theme.neon, 0x00)});
                hb.setCornerRadii(new float[]{18*density,18*density, 18*density,18*density, 0,0, 0,0});
                header.setBackground(hb);
                int hpad = (int) (14 * density);
                header.setPadding(hpad, (int) (12 * density), hpad, (int) (10 * density));
                LinearLayout titleRow = new LinearLayout(activity);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                TextView t = new TextView(activity);
                t.setText(title); t.setTextSize(20f); t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setTextColor(theme.title); t.setSingleLine(false);
                titleRow.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
                if (cancelable) {
                    TextView x = new TextView(activity);
                    x.setText("\u2715"); x.setTextSize(17f);
                    x.setTextColor(withAlpha(theme.soft, 0xCC));
                    int xp = (int) (8 * density);
                    x.setPadding(xp, 0, xp, 0);
                    x.setContentDescription("Close");
                    x.setOnClickListener(v -> d.dismiss());
                    titleRow.addView(x);
                }
                header.addView(titleRow);
                View divider = new View(activity);
                GradientDrawable db = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{withAlpha(theme.neon, 0x00), withAlpha(theme.neon, 0xBB), withAlpha(theme.neon, 0x00)});
                divider.setBackground(db);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, (int) (2 * density));
                dlp.topMargin = (int) (8 * density);
                header.addView(divider, dlp);
                LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
                hlp.setMargins(-pad / 2, -pad / 3, -pad / 2, 0);
                card.addView(header, hlp);
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
                if(content.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)content.getParent()).removeView(content);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = gap;
                if(pinned){pinSv=new ScrollView(activity);pinSv.addView(content);pinLp=lp;card.addView(pinSv,lp);}else card.addView(content, lp);
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

            boolean pin=pinned&&pinSv!=null;ScrollView sv=null;
            if(pin)d.setContentView(card);else{sv=new ScrollView(activity);sv.addView(card);d.setContentView(sv);}
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
            // size eagerly at build: 88% width, wrap height, cap at 78% of screen with scroll
            int sw = activity.getResources().getDisplayMetrics().widthPixels;
            int sh = activity.getResources().getDisplayMetrics().heightPixels;
            int width = (int) (sw * 0.88f);
            View measured=pin?card:sv;
            measured.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                       View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int max = (int) (sh * 0.78f);
            if(pin&&measured.getMeasuredHeight()>max){pinLp.height=0;pinLp.weight=1;pinSv.setLayoutParams(pinLp);}
            if (w != null) {
                if (measured.getMeasuredHeight() > max) w.setLayout(width, max);
                else w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            }
            return d;
        }
    }

    static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    /** Original illustrated reward hero: gift box with coin burst, rays and sparkles. Visual only. */
    static View rewardHero(android.content.Context c, Palette theme) {
        final float density = c.getResources().getDisplayMetrics().density;
        final int neon = theme.neon, title = theme.title, line = theme.panelLine;
        View v = new View(c) {
            @Override protected void onDraw(android.graphics.Canvas cv) {
                float w = getWidth(), h = getHeight(), cx = w / 2, cy = h * 0.58f;
                android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                // soft radial glow
                for (int i = 3; i >= 1; i--) {
                    pt.setColor(withAlpha(neon, 0x14 + i * 2));
                    cv.drawCircle(cx, cy, h * 0.11f * i, pt);
                }
                // rays
                pt.setStyle(android.graphics.Paint.Style.STROKE);
                pt.setStrokeWidth(2 * density);
                pt.setColor(withAlpha(neon, 0x55));
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + Math.PI / 12;
                    float r1 = h * 0.28f, r2 = h * 0.36f;
                    cv.drawLine(cx + (float) (r1 * Math.cos(a)), cy + (float) (r1 * Math.sin(a)),
                                cx + (float) (r2 * Math.cos(a)), cy + (float) (r2 * Math.sin(a)), pt);
                }
                pt.setStyle(android.graphics.Paint.Style.FILL);
                // coin burst behind the gift
                int[] gold = {0xFFFFD54A, 0xFFFFC93C, 0xFFFFE082};
                float[][] coins = {{-0.30f, -0.32f, 0.13f}, {0.28f, -0.37f, 0.11f}, {0.02f, -0.42f, 0.13f}};
                for (int i = 0; i < coins.length; i++) {
                    float px = cx + coins[i][0] * w, py = cy + coins[i][1] * h, r = coins[i][2] * h;
                    pt.setColor(gold[i]); cv.drawCircle(px, py, r, pt);
                    pt.setColor(0xFFB8860B); pt.setStyle(android.graphics.Paint.Style.STROKE);
                    pt.setStrokeWidth(1.5f * density); cv.drawCircle(px, py, r * 0.62f, pt);
                    pt.setStyle(android.graphics.Paint.Style.FILL);
                    pt.setColor(0x99FFFFFF); cv.drawCircle(px - r * .3f, py - r * .3f, r * .22f, pt);
                }
                // gift box
                float bw = w * 0.34f, bh = h * 0.30f, bx = cx - bw / 2, by = cy - bh / 2 + h * 0.08f;
                android.graphics.RectF box = new android.graphics.RectF(bx, by, bx + bw, by + bh);
                pt.setColor(title); cv.drawRoundRect(box, 6 * density, 6 * density, pt);
                android.graphics.RectF lid = new android.graphics.RectF(bx - bw * 0.07f, by - bh * 0.22f, bx + bw * 1.07f, by + bh * 0.06f);
                pt.setColor(neon); cv.drawRoundRect(lid, 6 * density, 6 * density, pt);
                pt.setColor(withAlpha(line, 0xEE));
                cv.drawRect(cx - bw * 0.07f, by - bh * 0.22f, cx + bw * 0.07f, by + bh, pt); // ribbon
                // bow
                pt.setColor(neon);
                cv.drawCircle(cx - bw * 0.12f, by - bh * 0.28f, bw * 0.10f, pt);
                cv.drawCircle(cx + bw * 0.12f, by - bh * 0.28f, bw * 0.10f, pt);
                // sparkles
                pt.setColor(0xFFFFFFFF);
                float[][] sp = {{-0.42f, -0.06f}, {0.40f, 0.06f}, {-0.16f, -0.46f}, {0.18f, -0.14f}};
                for (float[] s : sp) {
                    float px = cx + s[0] * w, py = cy + s[1] * h, r = density * 2.2f;
                    cv.drawLine(px - r, py, px + r, py, pt);
                    cv.drawLine(px, py - r, px, py + r, pt);
                }
            }
        };
        v.setMinimumHeight((int) (120 * density));
        return v;
    }

    /** Rounded track + themed fill progress bar for mission/quest rows. Visual only. */
    static View progressBar(android.content.Context c, Palette theme, long progress, long goal) {
        final float density = c.getResources().getDisplayMetrics().density;
        final float frac = goal <= 0 ? 0f : Math.min(1f, progress / (float) goal);
        final int track = withAlpha(theme.panelLine, 0x55), fill = theme.neon;
        View v = new View(c) {
            @Override protected void onDraw(android.graphics.Canvas cv) {
                float h = getHeight(), w = getWidth();
                android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                android.graphics.RectF r = new android.graphics.RectF(0, h * 0.28f, w, h * 0.72f);
                pt.setColor(track); cv.drawRoundRect(r, h / 2, h / 2, pt);
                if (frac > 0) {
                    r.set(0, h * 0.28f, Math.max(h * 0.72f, w * frac), h * 0.72f);
                    pt.setColor(fill); cv.drawRoundRect(r, h / 2, h / 2, pt);
                }
            }
        };
        v.setMinimumHeight((int) (10 * density));
        return v;
    }
}

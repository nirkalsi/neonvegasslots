package nir.com.neonvegasslots;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

/** One-shot claim celebration: gold coins arc from the Claim button to the coin balance, with a rising "+N". Original art, no timers kept after it ends. */
final class ClaimBurst extends View {
    private static final int N = 14;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private final long reward;
    private float sx, sy, tx, ty;
    private float t = 0f;

    ClaimBurst(Context c, long reward) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        this.reward = reward;
        setClickable(false);
    }

    void setPoints(float sx, float sy, float tx, float ty) { this.sx = sx; this.sy = sy; this.tx = tx; this.ty = ty; }
    /** Test hook and animator target: progress 0..1. */
    void setProgress(float v) { t = v; invalidate(); }

    /** Adds the overlay to the root of the window that holds {@code from} and plays it once. */
    static void play(View from, long reward, float targetScreenX, float targetScreenY) {
        ViewGroup root = (ViewGroup) from.getRootView();
        ClaimBurst b = new ClaimBurst(from.getContext(), reward);
        root.addView(b, new ViewGroup.LayoutParams(-1, -1));
        int[] fl = new int[2], rl = new int[2];
        from.getLocationOnScreen(fl); root.getLocationOnScreen(rl);
        b.setPoints(fl[0] - rl[0] + from.getWidth() / 2f, fl[1] - rl[1] + from.getHeight() / 2f,
                    targetScreenX - rl[0], targetScreenY - rl[1]);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(1100); a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.addUpdateListener(v -> b.setProgress((float) v.getAnimatedValue()));
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator an) { root.removeView(b); }
        });
        a.start();
    }

    @Override protected void onDraw(Canvas cv) {
        if (t <= 0f || t >= 1f) return;
        for (int i = 0; i < N; i++) {
            float delay = i * 0.035f;
            float u = Math.max(0f, Math.min(1f, (t - delay) / (1f - 0.035f * N)));
            if (u <= 0f) continue;
            float ang = (float) (i * 2.399963); // golden angle spread, deterministic
            float burst = Math.min(1f, u * 3f);       // first third: pop outward
            float ox = (float) Math.cos(ang) * 46 * d * (0.5f + (i % 3) * 0.25f) * burst;
            float oy = (float) Math.sin(ang) * 30 * d * burst - 20 * d * burst;
            float fly = Math.max(0f, (u - 0.33f) / 0.67f);
            fly = fly * fly;
            float x = sx + ox + (tx - sx - ox) * fly;
            float y = sy + oy + (ty - sy - oy) * fly;
            float r = (6.5f + (i % 3)) * d * (1f - 0.35f * fly);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFC83A); p.setAlpha((int) (255 * (1f - Math.max(0f, fly - 0.85f) / 0.15f)));
            cv.drawCircle(x, y, r, p);
            p.setColor(0xFFE09A12); p.setAlpha(255); cv.drawCircle(x, y, r * 0.55f, p);
            p.setColor(0xFFFFF0AA); cv.drawCircle(x - r * 0.28f, y - r * 0.28f, r * 0.22f, p);
        }
        float tt = Math.min(1f, t / 0.5f);
        p.setTextAlign(Paint.Align.CENTER); p.setFakeBoldText(true); p.setTextSize(22 * d);
        p.setShadowLayer(4 * d, 0, 0, 0xFF000000);
        p.setColor(0xFFFFE27A); p.setAlpha((int) (255 * (1f - Math.max(0f, t - 0.6f) / 0.4f)));
        float scale = 0.6f + 0.4f * (float) Math.sin(Math.min(1f, tt) * Math.PI / 2);
        cv.save(); cv.scale(scale, scale, sx, sy - 34 * d - 26 * d * t);
        cv.drawText("+" + reward, sx, sy - 34 * d - 26 * d * t, p);
        cv.restore();
        p.clearShadowLayer(); p.setFakeBoldText(false); p.setAlpha(255);
    }
}

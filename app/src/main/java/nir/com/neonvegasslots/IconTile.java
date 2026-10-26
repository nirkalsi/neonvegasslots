package nir.com.neonvegasslots;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Round glossy icon tile for the top bar (original art). Icon-only with a content description; draws a claim-ready badge and pulse ring. */
final class IconTile extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Drawable icon;
    private final int top, bot, rim;
    private final float d;
    private int badge = 0;
    private float phase = 0f;
    private ValueAnimator pulse;
    private final String baseDesc;

    IconTile(Context c, String key, int accent, int top, int bot, int rim, String desc, float iconDp) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        this.top = top; this.bot = bot; this.rim = rim;
        icon = TopIcons.icon(c, key, accent, (int) iconDp);
        baseDesc = desc;
        setContentDescription(desc);
        setClickable(true); setFocusable(true);
    }

    void setBadge(int n) {
        if (n == badge) return;
        boolean wasZero = badge == 0;
        badge = n;
        setContentDescription(n > 0 ? baseDesc + ", " + n + " ready to claim" : baseDesc);
        if (n > 0) {
            if (wasZero) bounce();
            if (Splash.pulse && pulse == null) {
                pulse = ValueAnimator.ofFloat(0f, 1f);
                pulse.setDuration(1300); pulse.setRepeatCount(ValueAnimator.INFINITE);
                pulse.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
                pulse.start();
            }
        } else if (pulse != null) { pulse.cancel(); pulse = null; phase = 0f; }
        invalidate();
    }
    int getBadge() { return badge; }
    /** Test hook: freeze the pulse ring at a given phase (0..1). */
    void setPhase(float ph) { phase = ph; invalidate(); }

    void bounce() {
        if (!Splash.pulse) return;
        ObjectAnimator sx = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.28f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.28f, 1f);
        for (ObjectAnimator a : new ObjectAnimator[]{sx, sy}) { a.setDuration(520); a.setInterpolator(new OvershootInterpolator(2f)); a.start(); }
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulse != null) { pulse.cancel(); pulse = null; }
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h) - 16 * d; // margin leaves room for the pulse ring and badge
        float cx = w / 2f, cy = h / 2f;
        rf.set(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
        float rad = s * 0.30f;
        if (badge > 0) { // claim-ready: expanding gold ring
            float r = s / 2 + phase * 7 * d;
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f * d);
            p.setShader(null); p.setColor(0xFFFFD84A); p.setAlpha((int) (200 * (1 - phase)));
            rf.inset(-phase * 7 * d, -phase * 7 * d);
            cv.drawRoundRect(rf, rad + phase * 7 * d, rad + phase * 7 * d, p);
            rf.set(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
        }
        p.setStyle(Paint.Style.FILL); p.setColor(0x66000000);
        rf.offset(0, 2 * d); cv.drawRoundRect(rf, rad, rad, p); rf.offset(0, -2 * d);
        p.setColor(0xFF000000); // opaque base so the gradient is not tinted by earlier alpha
        p.setShader(new LinearGradient(0, rf.top, 0, rf.bottom, top, bot, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rf, rad, rad, p);
        p.setShader(new LinearGradient(0, rf.top, 0, rf.centerY(), 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        RectF g = new RectF(rf.left + 3 * d, rf.top + 2 * d, rf.right - 3 * d, rf.centerY());
        cv.drawRoundRect(g, rad * 0.8f, rad * 0.8f, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2f * d); p.setColor(rim);
        cv.drawRoundRect(rf, rad, rad, p);
        int isz = icon.getIntrinsicWidth();
        int ix = (int) (cx - isz / 2f), iy = (int) (cy - isz / 2f);
        icon.setBounds(ix, iy, ix + isz, iy + isz);
        icon.draw(cv);
        if (badge > 0) {
            float br = 8.5f * d, bx = rf.right - 1.5f * d, by = rf.top + 1.5f * d;
            p.setStyle(Paint.Style.FILL); p.setColor(0xFFFFFFFF); cv.drawCircle(bx, by, br + 1.5f * d, p);
            p.setColor(0xFFE53935); cv.drawCircle(bx, by, br, p);
            p.setColor(0xFFFFFFFF); p.setTextAlign(Paint.Align.CENTER); p.setFakeBoldText(true); p.setTextSize(11 * d);
            cv.drawText(String.valueOf(Math.min(badge, 9)), bx, by + 4 * d, p);
            p.setFakeBoldText(false);
        }
    }
}

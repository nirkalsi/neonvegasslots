package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Launch splash: original drawn art (gold coin, glow, title). Overlay removed by MainActivity after a short fade. */
final class Splash extends View {
    /** Tests switch this off so screen renders show the game, not the overlay. */
    static boolean enabled = true;
    /** Idle pulse on the SHOP button; tests switch it off (an endless animator keeps a paused test looper busy). */
    static boolean pulse = true;
    static final long SHOW_MS = 1300;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long t0 = android.os.SystemClock.uptimeMillis();

    Splash(Context c) { super(c); setClickable(true); }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF2E1560, 0xFF0B0418, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        float cx = w / 2f, cy = h * 0.36f, r = Math.min(w, h) * 0.20f;
        p.setShader(new RadialGradient(cx, cy, r * 2.2f, 0x66FFB300, 0x00FFB300, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r * 2.2f, p);
        p.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFFFFE08A, 0xFFB8860B, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * 0.12f); p.setColor(0xFF8A5A00);
        c.drawCircle(cx, cy, r * 0.74f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF6B3F00); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(r * 0.95f);
        c.drawText("7", cx, cy + r * 0.34f, p);
        p.setColor(0x88FFFFFF); c.drawCircle(cx - r * 0.45f, cy - r * 0.5f, r * 0.12f, p);

        p.setColor(0xFFFFD36B); p.setTextSize(Math.min(w * 0.095f, 40 * d));
        c.drawText("NEON VEGAS", cx, h * 0.63f, p);
        p.setColor(0xFFFFFFFF); p.setTextSize(Math.min(w * 0.06f, 26 * d));
        c.drawText("SLOTS", cx, h * 0.63f + Math.min(w * 0.08f, 34 * d), p);
        p.setTypeface(Typeface.DEFAULT); p.setColor(0xCCFFFFFF); p.setTextSize(Math.min(w * 0.034f, 14 * d));
        c.drawText("Simulated slots. Virtual coins, no cash value.", cx, h * 0.80f, p);

        float bw = w * 0.5f, bh = 5 * d, bx = cx - bw / 2f, by = h * 0.86f;
        p.setColor(0x33FFFFFF); c.drawRoundRect(new RectF(bx, by, bx + bw, by + bh), bh, bh, p);
        float f = Math.min(1f, (android.os.SystemClock.uptimeMillis() - t0) / (float) SHOW_MS);
        p.setColor(0xFFFFB300); c.drawRoundRect(new RectF(bx, by, bx + bw * f, by + bh), bh, bh, p);
        if (f < 1f) postInvalidateOnAnimation();
    }
}

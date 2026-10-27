package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;

/** Glossy top-row buttons: rounded gradient tile, themed rim, and a small drawn icon per action (original art). */
final class TopIcons {
    private TopIcons() {}

    static Drawable tile(float d, int top, int bot, int rim) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bot});
        base.setCornerRadius(14 * d); base.setStroke(Math.max(1, (int) (1.5f * d)), rim);
        GradientDrawable gloss = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x55FFFFFF, 0x00FFFFFF});
        gloss.setCornerRadius(14 * d);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{base, gloss});
        ld.setLayerInset(1, (int) (3 * d), (int) (2 * d), (int) (3 * d), (int) (24 * d));
        GradientDrawable pressed = new GradientDrawable(); pressed.setCornerRadius(14 * d); pressed.setColor(0x66000000);
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed}, new LayerDrawable(new Drawable[]{ld, pressed}));
        sl.addState(new int[]{}, ld);
        return sl;
    }

    static Drawable icon(Context c, String key, int accent) { return icon(c, key, accent, 22); }

    static Drawable icon(Context c, String key, int accent, int sizeDp) {
        float d = c.getResources().getDisplayMetrics().density;
        int s = (int) (sizeDp * d);
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float u = s / 22f;
        p.setStyle(Paint.Style.FILL); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        switch (key) {
            case "FREE COINS": {
                p.setColor(0xFFFFD166); cv.drawCircle(11 * u, 11 * u, 9 * u, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f * u); p.setColor(0xFFB8860B); cv.drawCircle(11 * u, 11 * u, 6 * u, p);
                p.setStyle(Paint.Style.FILL); p.setColor(0xFFFFF3C4); cv.drawCircle(8.5f * u, 8f * u, 2f * u, p);
                p.setColor(0xFF163047);cv.drawRoundRect(new RectF(11*u,12*u,22*u,21*u),2*u,2*u,p);
                p.setColor(0xFFFFFFFF);Path play=new Path();play.moveTo(14*u,13.5f*u);play.lineTo(19*u,16.5f*u);play.lineTo(14*u,19.5f*u);play.close();cv.drawPath(play,p);
                break; }
            case "MISSIONS": {
                p.setColor(0xFFFFFFFF); cv.drawRoundRect(new RectF(4 * u, 2 * u, 18 * u, 20 * u), 3 * u, 3 * u, p);
                p.setColor(accent); cv.drawRoundRect(new RectF(8 * u, 1 * u, 14 * u, 5 * u), 1.5f * u, 1.5f * u, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * u);
                p.setColor(0xFF2E7D32); cv.drawLine(7 * u, 10 * u, 8.5f * u, 12 * u, p); cv.drawLine(8.5f * u, 12 * u, 11.5f * u, 8.5f * u, p);
                p.setColor(0xFF607D8B); cv.drawLine(7 * u, 16 * u, 15 * u, 16 * u, p);
                break; }
            case "SHOP": {
                p.setColor(0xFFFFFFFF);
                Path b = new Path(); b.moveTo(4 * u, 8 * u); b.lineTo(18 * u, 8 * u); b.lineTo(17 * u, 20 * u); b.lineTo(5 * u, 20 * u); b.close(); cv.drawPath(b, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2f * u); p.setColor(0xFFFFFFFF);
                cv.drawArc(new RectF(7 * u, 2 * u, 15 * u, 12 * u), 180, 180, false, p);
                p.setStyle(Paint.Style.FILL);
                break; }
            case "MORE GAMES": {
                p.setColor(0xFFFFFFFF); cv.drawRoundRect(new RectF(1 * u, 5 * u, 21 * u, 17 * u), 6 * u, 6 * u, p);
                p.setColor(0xFF37474F); cv.drawRect(5 * u, 10 * u, 11 * u, 12 * u, p); cv.drawRect(7 * u, 8 * u, 9 * u, 14 * u, p);
                p.setColor(0xFFE53935); cv.drawCircle(15 * u, 13 * u, 1.7f * u, p);
                p.setColor(0xFF1E88E5); cv.drawCircle(18 * u, 10 * u, 1.7f * u, p);
                break; }
            default: { // NO ADS
                p.setColor(0xFFFFFFFF); cv.drawRoundRect(new RectF(2 * u, 6 * u, 20 * u, 16 * u), 3 * u, 3 * u, p);
                p.setColor(0xFF455A64); p.setTextSize(7 * u); p.setFakeBoldText(true); p.setTextAlign(Paint.Align.CENTER); cv.drawText("AD", 11 * u, 13.2f * u, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.2f * u); p.setColor(0xFFE53935);
                cv.drawCircle(11 * u, 11 * u, 9.5f * u, p); cv.drawLine(4.5f * u, 17.5f * u, 17.5f * u, 4.5f * u, p);
            }
        }
        return new BitmapDrawable(c.getResources(), bm);
    }
}

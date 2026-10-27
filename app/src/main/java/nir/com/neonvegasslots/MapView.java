package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

/** Full progression map shown in a dialog: tap an unlocked stop to apply its theme. */
final class MapView extends View {
    private final SlotsView g;
    MapView(Context c, SlotsView g) { super(c); this.g = g; }
    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, (int) (w * 0.72f));
    }
    @Override protected void onDraw(Canvas c) { g.drawFullMap(c, getWidth(), getHeight()); }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return true;
        float w = getWidth(), x = e.getX(), y = e.getY();
        float ny = Math.min(getHeight(), w * .68f) * .38f;
        if (Math.abs(y - ny) <= w * .12f) {
            int best = -1; float bd = Float.MAX_VALUE;
            for (int i = 0; i < SlotArt.THEMES.length; i++) { float d = Math.abs(x - SlotArt.nodeX(w, i)); if (d < bd) { bd = d; best = i; } }
            if (best >= 0 && bd <= w * .11f) { g.selectTheme(best); invalidate(); g.invalidate(); }
        }
        return true;
    }
}

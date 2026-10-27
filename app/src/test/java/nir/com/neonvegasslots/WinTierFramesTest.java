package nir.com.neonvegasslots;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** SYNTHETIC frame evidence of the BIG / MEGA / EPIC win celebrations (Robolectric render, not a device). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-420dpi")
public class WinTierFramesTest {
  private void set(Object o, String n, Object v) throws Exception { Field f = SlotsView.class.getDeclaredField(n); f.setAccessible(true); if (v instanceof Long) f.setLong(o, (Long) v); else f.setInt(o, (Integer) v); }
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test public void tierFrames() throws Exception {
    final long[] now = {100000};
    SlotsView v = new SlotsView(RuntimeEnvironment.getApplication()) { @Override long nowMillis() { return now[0]; } };
    v.restore("5000;0;5;0;0;10;3");
    long bet = SlotsView.betAmount(5); long[] wins = {30 * bet, 80 * bet, 200 * bet};
    for (int tier = 0; tier < 3; tier++) {
      for (int k = 0; k < 4; k++) {
        float frac = new float[]{0.1f, 0.3f, 0.6f, 0.9f}[k];
        long ms = SlotsView.TIER_MS[tier]; long start = now[0] - (long) (frac * ms);
        set(v, "bigWinTier", tier); set(v, "bigWinMs", ms); set(v, "bigWinStartAt", start); set(v, "bigWinUntil", start + ms);
        set(v, "bigWinAmount", wins[tier]); set(v, "winStartAt", start); set(v, "winFlashUntil", start + ms); set(v, "lastWin", wins[tier]);
        v.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1110, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 540, 1110);
        Bitmap b = Bitmap.createBitmap(540, 1110, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        try (FileOutputStream f = new FileOutputStream("/tmp/renders/neonvegasslots-win-t" + tier + "-f" + k + ".png")) { b.compress(Bitmap.CompressFormat.PNG, 100, f); }
      }
    }
  }
}

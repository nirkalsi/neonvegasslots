package nir.com.neonvegasslots;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** SYNTHETIC still of the idle game screen (Robolectric render, not a device). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-420dpi")
public class ShotTest {
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test public void idleShot() throws Exception {
    SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
    v.restore("50000;0;3;0;0;1;0");
    v.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1110, View.MeasureSpec.EXACTLY));
    v.layout(0, 0, 540, 1110);
    Bitmap bm = Bitmap.createBitmap(540, 1110, Bitmap.Config.ARGB_8888);
    v.draw(new Canvas(bm));
    new java.io.File("/tmp/vf").mkdirs();
    try (FileOutputStream f = new FileOutputStream("/tmp/vf/shot-neonvegasslots.png")) { bm.compress(Bitmap.CompressFormat.PNG, 90, f); }
  }

  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test public void winShot() throws Exception {
    SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
    v.restore("50000;0;3;0;0;1;0");
    java.lang.reflect.Field lw = SlotsView.class.getDeclaredField("lastWin"); lw.setAccessible(true); lw.setLong(v, 1200L);
    java.lang.reflect.Field gf = SlotsView.class.getDeclaredField("grid"); gf.setAccessible(true); int[][] gr = (int[][]) gf.get(v);
    java.lang.reflect.Field wc = SlotsView.class.getDeclaredField("winCell"); wc.setAccessible(true); boolean[][] wcell = (boolean[][]) wc.get(v);
    java.lang.reflect.Field wf = SlotsView.class.getDeclaredField("winFlashUntil"); wf.setAccessible(true); wf.setLong(v, System.currentTimeMillis() + 60000L);
    for (int c = 0; c < 5; c++) gr[1][c] = c < 3 ? 2 : 4;
    for (int c = 0; c < 3; c++) wcell[1][c] = true;
    v.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1110, View.MeasureSpec.EXACTLY));
    v.layout(0, 0, 540, 1110);
    Bitmap bm = Bitmap.createBitmap(540, 1110, Bitmap.Config.ARGB_8888);
    v.draw(new Canvas(bm));
    try (FileOutputStream f = new FileOutputStream("/tmp/vf/shot-neonvegasslots-win.png")) { bm.compress(Bitmap.CompressFormat.PNG, 90, f); }
  }

  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test public void mapShot() throws Exception {
    SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
    v.restore("50000;0;3;0;0;7;0");
    MapView mv = new MapView(RuntimeEnvironment.getApplication(), v);
    mv.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    mv.layout(0, 0, 540, mv.getMeasuredHeight());
    Bitmap bm = Bitmap.createBitmap(540, mv.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    mv.draw(new Canvas(bm));
    try (FileOutputStream f = new FileOutputStream("/tmp/vf/map-neonvegasslots.png")) { bm.compress(Bitmap.CompressFormat.PNG, 90, f); }
  }
}

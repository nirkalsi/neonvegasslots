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
}

package nir.com.neonvegasslots;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;

/** Pixel-render smoke: game view + full screen + reward/daily/missions/shop overlays. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RenderSmokeTest {

    private void renderView(View v, int w, int h, String path) throws Exception {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        FileOutputStream fos = new FileOutputStream(path);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        assertTrue(path + " too small", new File(path).length() > 5000);
    }

    private MainActivity activity() {
        return Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
    }

    private void callPrivate(Object o, String name) throws Exception {
        Method m = o.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(o);
    }

    private void renderLatestDialog(String tag) throws Exception {
        AlertDialog d = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("no dialog for " + tag, d);
        renderView(d.getWindow().getDecorView(), 1000, 1700, "/tmp/renders/" + tag + ".png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderGameView1080() throws Exception {
        renderView(new SlotsView(RuntimeEnvironment.getApplication()), 1080, 2243, "/tmp/renders/neonvegasslots-game-1080x2243.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderGameView720() throws Exception {
        renderView(new SlotsView(RuntimeEnvironment.getApplication()), 720, 1480, "/tmp/renders/neonvegasslots-game-720x1480.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderFullScreen1080() throws Exception {
        MainActivity a = activity();
        renderView(a.findViewById(android.R.id.content), 1080, 2243, "/tmp/renders/neonvegasslots-full-1080x2243.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderDailyBonus() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "maybeDailyBonus");
        renderLatestDialog("neonvegasslots-daily");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderMissions() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showMissions");
        renderLatestDialog("neonvegasslots-missions");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderShop() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showShop");
        renderLatestDialog("neonvegasslots-shop");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderOutOfCoinsRewardPrompt() throws Exception {
        MainActivity a = activity();
        a.onOutOfCoins();
        renderLatestDialog("neonvegasslots-outofcoins");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderSecondTheme720() throws Exception {
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
        v.restore("5000;0;1;0;0;3;1");
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-theme2-720x1480.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderTopTheme720() throws Exception {
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
        v.restore("5000;0;1;0;0;10;3");
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-theme4-720x1480.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderFullScreen720() throws Exception {
        MainActivity a = activity();
        renderView(a.findViewById(android.R.id.content), 720, 1480, "/tmp/renders/neonvegasslots-full-720x1480.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderMotionFrames() throws Exception {
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
        v.restore("5000;0;1;0;0;10;3");
        v.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(1480, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 720, 1480);
        long now = System.currentTimeMillis();
        java.lang.reflect.Field f;
        f = SlotsView.class.getDeclaredField("spinning"); f.setAccessible(true); f.setBoolean(v, true);
        f = SlotsView.class.getDeclaredField("stopAt"); f.setAccessible(true);
        f.set(v, new long[]{now + 4000, now + 4000, now + 4000, now + 4000, now + 4000});
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-frame-spin.png");
        f.set(v, new long[]{now - 10, now - 6, now - 2, now + 4000, now + 4000}); // 3 columns at bounce peak
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-frame-settle.png");
        f = SlotsView.class.getDeclaredField("spinning"); f.setAccessible(true); f.setBoolean(v, false);
        f = SlotsView.class.getDeclaredField("lastWin"); f.setAccessible(true); f.setLong(v, 500);
        f = SlotsView.class.getDeclaredField("winStartAt"); f.setAccessible(true); f.setLong(v, now - 350);
        f = SlotsView.class.getDeclaredField("winFlashUntil"); f.setAccessible(true); f.setLong(v, now + 1300);
        f = SlotsView.class.getDeclaredField("levelFlashUntil"); f.setAccessible(true); f.setLong(v, now + 1600);
        f = SlotsView.class.getDeclaredField("winCell"); f.setAccessible(true);
        boolean[][] wc = (boolean[][]) f.get(v);
        for (int c = 0; c < 5; c++) wc[1][c] = true;
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-frame-win.png");
    }
}

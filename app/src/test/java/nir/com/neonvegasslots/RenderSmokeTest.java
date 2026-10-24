package nir.com.neonvegasslots;

import static org.junit.Assert.*;
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
import android.app.Dialog;
import org.robolectric.shadows.ShadowDialog;

/** Pixel-render smoke: game view + full screen + reward/daily/missions/shop overlays. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-420dpi")
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

    private Dialog latestDialog() {
        java.util.List<Dialog> ds = ShadowDialog.getShownDialogs();
        assertFalse("no dialog shown", ds.isEmpty());
        return ds.get(ds.size() - 1);
    }

    private void renderLatestDialog(String tag) throws Exception {
        renderLatestDialog(tag, 1000, 1700);
    }

    private void renderLatestDialog(String tag, int w, int h) throws Exception {
        Dialog d = latestDialog();
        assertNotNull("no dialog for " + tag, d);
        renderView(d.getWindow().getDecorView(), w, h, "/tmp/renders/" + tag + ".png");
    }

    /** Composed render: activity content + dim + dialog decor at its real window size/position. */
    private void renderComposed(MainActivity a, Dialog d, String path) throws Exception {
        // One coherent device space: activity, display metrics and dialog window
        // attributes all come from the same configured display (see @Config qualifiers).
        android.util.DisplayMetrics dm = a.getResources().getDisplayMetrics();
        int w = dm.widthPixels, h = dm.heightPixels;
        View root = a.findViewById(android.R.id.content);
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                     View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        root.draw(cv);
        android.view.WindowManager.LayoutParams lp = d.getWindow().getAttributes();
        assertTrue("dialog must dim the game behind it",
            (lp.flags & android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0);
        cv.drawColor(0x99000000); // dimAmount 0.6
        int dw = lp.width > 0 ? lp.width : (int) (w * 0.88f);
        assertEquals("dialog width must be 88% of the same display the activity renders on",
            (int) (w * 0.88f), dw);
        View dv = d.getWindow().getDecorView();
        int maxH = (int) (h * 0.78f);
        dv.measure(View.MeasureSpec.makeMeasureSpec(dw, View.MeasureSpec.EXACTLY),
                   View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int dh = Math.min(dv.getMeasuredHeight(), maxH);
        if (lp.height > 0) dh = Math.min(lp.height, maxH);
        dv.layout(0, 0, dw, dh);
        // Position from the actual window gravity + offsets, the way WindowManager does.
        int grav = lp.gravity != 0 ? lp.gravity : android.view.Gravity.CENTER;
        android.graphics.Rect container = new android.graphics.Rect(0, 0, w, h);
        android.graphics.Rect out = new android.graphics.Rect();
        android.view.Gravity.apply(grav, dw, dh, container, out);
        cv.save();
        cv.translate(out.left + lp.x, out.top + lp.y);
        dv.draw(cv);
        cv.restore();
        FileOutputStream fos = new FileOutputStream(path);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        assertTrue(path + " too small", new File(path).length() > 5000);
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderShopComposed() throws Exception {
        MainActivity a = activity();
        injectTestPrices(a);
        callPrivate(a, "showShop");
        Dialog d = latestDialog();
        int expectedW = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.88f);
        assertEquals("dialog width must be 88% of screen", expectedW, d.getWindow().getAttributes().width);
        renderComposed(a, d, "/tmp/renders/neonvegasslots-shop-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderDailyComposed() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "maybeDailyBonus");
        renderComposed(a, latestDialog(), "/tmp/renders/neonvegasslots-daily-composed.png");
    }

    @Test public void tallDialogIsCappedAndScrollable() throws Exception {
        MainActivity a = activity();
        android.widget.LinearLayout tall = new android.widget.LinearLayout(a);
        tall.setOrientation(android.widget.LinearLayout.VERTICAL);
        for (int i = 0; i < 60; i++) { android.widget.TextView t = new android.widget.TextView(a); t.setText("Row " + i); t.setTextSize(16f); tall.addView(t); }
        Dialog d = new GameDialog.Builder(a, GameDialog.fromTheme(SlotArt.THEMES[0]))
            .title("Tall").content(tall).add(GameDialog.Btn.secondary("Close", null)).show();
        int sh = a.getResources().getDisplayMetrics().heightPixels;
        assertEquals("tall dialog height capped at 78% of screen", (int) (sh * 0.78f), d.getWindow().getAttributes().height);
        d.dismiss();
    }


    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderMissionsComposed() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showMissions");
        renderComposed(a, latestDialog(), "/tmp/renders/neonvegasslots-missions-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderOutOfCoinsComposed() throws Exception {
        MainActivity a = activity();
        BillingManager bm = injectTestPrices(a);
        bm.testPrices.put("remove_ads", "$4.99"); // show the scoped remove-ads entry with a live price
        a.onOutOfCoins();
        renderComposed(a, latestDialog(), "/tmp/renders/neonvegasslots-outofcoins-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderRemoveAdsComposed() throws Exception {
        MainActivity a = activity();
        BillingManager bm = injectTestPrices(a);
        bm.testPrices.put("remove_ads", "$4.99");
        java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("offerRemoveAds", String.class);
        m.setAccessible(true);
        m.invoke(a, "test");
        renderComposed(a, latestDialog(), "/tmp/renders/neonvegasslots-removeads-composed.png");
    }


    @Test public void removeAdsOfferHiddenWithoutLivePrice() throws Exception {
        MainActivity a = activity();
        java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("offerRemoveAds", String.class);
        m.setAccessible(true);
        m.invoke(a, "test");
        assertTrue("no remove-ads offer without a live localized price", ShadowDialog.getShownDialogs().isEmpty());
    }

    @Test public void outOfCoinsOmitsRemoveAdsWithoutPrice() throws Exception {
        MainActivity a = activity();
        a.onOutOfCoins();
        assertNull("no remove-ads button without a live price",
            findButton(latestDialog().getWindow().getDecorView(), "Remove banner & interstitial ads"));
    }

    private android.widget.Button findButton(View root, String text) {
        java.util.ArrayList<View> stack = new java.util.ArrayList<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            View v = stack.remove(0);
            if (v instanceof android.widget.Button && text.equals(((android.widget.Button) v).getText().toString()))
                return (android.widget.Button) v;
            if (v instanceof android.view.ViewGroup)
                for (int i = 0; i < ((android.view.ViewGroup) v).getChildCount(); i++)
                    stack.add(((android.view.ViewGroup) v).getChildAt(i));
        }
        return null;
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
    @Test public void renderTrailTrophies720() throws Exception {
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
        v.restore("5000;0;1;0;0;12;0;15;20261024"); // 4 trophies: full DOWNTOWN set (star badge) + 1 SUNSET
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-trail-trophies-720x1480.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderTrailEmptyTrophies720() throws Exception {
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication());
        v.restore("5000;0;1;0;0;3;1;0;");
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-trail-empty-720x1480.png");
    }

    private BillingManager injectTestPrices(MainActivity a) throws Exception {
        java.lang.reflect.Field f = MainActivity.class.getDeclaredField("billing");
        f.setAccessible(true);
        BillingManager bm = (BillingManager) f.get(a);
        bm.testPrices.put("coins_small", "$0.99");
        bm.testPrices.put("coins_medium", "₪10.90");
        bm.testPrices.put("coins_large", "₪109.90");
        bm.testPrices.put("coins_mega", "€1,234.56 long-price-wrap-check");
        return bm;
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderLoadedShopSyntheticPrices() throws Exception {
        MainActivity a = activity();
        injectTestPrices(a);
        callPrivate(a, "showShop");
        renderLatestDialog("neonvegasslots-shop-loaded");
    }

    @Test public void loadingShopSwapsToLoadedOnlyWhileVisible() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showShop"); // no products -> loading dialog
        Dialog d1 = latestDialog();
        assertTrue(d1.isShowing());
        // products arrive while loading dialog visible -> swaps to loaded shop
        injectTestPrices(a);
        a.onProductsReady();
        Dialog d2 = latestDialog();
        assertNotSame(d1, d2);
        assertFalse("loading dialog dismissed", d1.isShowing());
        assertTrue("loaded shop showing", d2.isShowing());
        d2.dismiss();
    }

    @Test public void dismissedLoadingShopNeverAutoReopens() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showShop");
        Dialog d1 = latestDialog();
        d1.dismiss(); // user closes the loading dialog
        injectTestPrices(a);
        a.onProductsReady();
        assertSame("no new dialog after dismiss", d1, latestDialog());
    }

    @Test public void tryAgainDoesNotStackDialogs() throws Exception {
        MainActivity a = activity();
        callPrivate(a, "showShop");
        findButton(latestDialog().getWindow().getDecorView(), "TRY AGAIN").performClick(); // TRY AGAIN
        findButton(latestDialog().getWindow().getDecorView(), "TRY AGAIN").performClick(); // TRY AGAIN again
        int showing = 0;
        for (Dialog d : ShadowDialog.getShownDialogs()) if (d.isShowing()) showing++;
        assertEquals("exactly one dialog showing", 1, showing);
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderShopNarrowLargeFont() throws Exception {
        MainActivity a = activity();
        injectTestPrices(a);
        RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.35f;
        try {
            callPrivate(a, "showShop");
            renderLatestDialog("neonvegasslots-shop-narrow-largefont", 560, 1240);
        } finally {
            RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.0f;
        }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderDailyNarrow() throws Exception {
        MainActivity a = activity();
        RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.35f;
        try {
            callPrivate(a, "maybeDailyBonus");
            renderLatestDialog("neonvegasslots-daily-narrow", 560, 1240);
        } finally {
            RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.0f;
        }
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

    @Test public void titleGlowIsElapsedTimeDriven() throws Exception {
        final long[] now = {0};
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication()) {
            @Override long nowMillis() { return now[0]; }
        };
        v.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 900, 1600);
        Bitmap bmp = Bitmap.createBitmap(900, 1600, Bitmap.Config.ARGB_8888);
        java.lang.reflect.Field f = SlotsView.class.getDeclaredField("titleGlow");
        f.setAccessible(true);
        v.draw(new Canvas(bmp));
        float g1 = f.getFloat(v);
        v.draw(new Canvas(bmp));
        v.draw(new Canvas(bmp));
        assertEquals("title glow must not advance per redraw", g1, f.getFloat(v), 0.0001f);
        now[0] = 2000;
        v.draw(new Canvas(bmp));
        assertEquals("title glow follows elapsed time", (float) Math.PI, f.getFloat(v), 0.001f);
    }
}

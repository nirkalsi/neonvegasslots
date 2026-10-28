package nir.com.neonvegasslots;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
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
        Splash.enabled = false; Splash.pulse = false;
        Reminders.promptEnabled = false; return Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
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
        dv.measure(View.MeasureSpec.makeMeasureSpec(dw, View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dh, View.MeasureSpec.EXACTLY));
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

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderDailyDaySevenComposed() throws Exception {
        MainActivity a=activity();
        java.lang.reflect.Field f=MainActivity.class.getDeclaredField("sp");f.setAccessible(true);
        java.lang.reflect.Method y=MainActivity.class.getDeclaredMethod("yesterday");y.setAccessible(true);
        ((android.content.SharedPreferences)f.get(a)).edit().putString("daily_last",(String)y.invoke(a)).putInt("daily_streak",6).commit();
        callPrivate(a,"maybeDailyBonus");
        renderComposed(a,latestDialog(),"/tmp/renders/neonvegasslots-daily-day7-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderJourneyComposed() throws Exception {
        MainActivity a=activity();a.onOpenMap();
        Dialog d=latestDialog();renderComposed(a,d,"/tmp/renders/neonvegasslots-journey-composed.png");
        android.widget.ScrollView sv=journeyScroller(d.getWindow().getDecorView());assertNotNull(sv);sv.scrollTo(0,100000);assertTrue("journey shelf must be scrollable",sv.getScrollY()>0);
        renderComposed(a,d,"/tmp/renders/neonvegasslots-journey-bottom-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderJourneyCollectedComposed() throws Exception {
        MainActivity a=activity();java.lang.reflect.Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView game=(SlotsView)f.get(a);
        game.restore("500;0;1;0;0;16;0");java.lang.reflect.Field trophies=SlotsView.class.getDeclaredField("trophyMask");trophies.setAccessible(true);trophies.setInt(game,0b101011);
        a.onOpenMap();Dialog d=latestDialog();renderComposed(a,d,"/tmp/renders/neonvegasslots-journey-unlocked-composed.png");
        android.widget.ScrollView sv=journeyScroller(d.getWindow().getDecorView());sv.scrollTo(0,(int)((42+SlotArt.THEMES.length*154-10)*a.getResources().getDisplayMetrics().density));renderComposed(a,d,"/tmp/renders/neonvegasslots-journey-collected-composed.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderTrophyRevealComposed() throws Exception {
        MainActivity a=activity();java.lang.reflect.Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView game=(SlotsView)f.get(a);game.tryTrophyDrop(.99f,"2026-10-27");assertFalse(GameDialog.isOpen());assertTrue(GameDialog.pending.isEmpty());renderView(a.findViewById(android.R.id.content),a.getResources().getDisplayMetrics().widthPixels,a.getResources().getDisplayMetrics().heightPixels,"/tmp/renders/neonvegasslots-trophy-nonblocking.png");
    }
    @Test public void trophyDropDoesNotQueueOrBlockPlay() throws Exception {
        MainActivity a=activity();java.lang.reflect.Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView game=(SlotsView)f.get(a);long before=game.getBalance();assertTrue(game.tryTrophyDrop(.99f,"2026-10-27"));assertFalse(GameDialog.isOpen());assertTrue(GameDialog.pending.isEmpty());assertEquals(before,game.getBalance());assertEquals(1,Integer.bitCount(game.getTrophyMask()));org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(710));assertFalse(GameDialog.isOpen());assertTrue(GameDialog.pending.isEmpty());
    }

    private TrophyReveal trophyView(View v){if(v instanceof TrophyReveal)return (TrophyReveal)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TrophyReveal t=trophyView(g.getChildAt(i));if(t!=null)return t;}}return null;}

    private android.widget.ScrollView journeyScroller(View v){
        if(v instanceof android.widget.ScrollView)return (android.widget.ScrollView)v;
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.widget.ScrollView s=journeyScroller(g.getChildAt(i));if(s!=null)return s;}}return null;
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
    @Test public void removedPurchaseHasNoOfferEvenWhenOldSkuHasPrice() throws Exception {
        MainActivity a=activity();BillingManager bm=injectTestPrices(a);bm.testPrices.put("remove_ads","$4.99");
        a.onOutOfCoins();assertNull("removed purchase must never be offered",findButton(latestDialog().getWindow().getDecorView(),"Remove banner & interstitial ads"));
        renderComposed(a,latestDialog(),"/tmp/renders/neonvegasslots-no-removeads-offer.png");
    }

    @Test public void removedOfferEntryPointIsAbsent() {
        for(java.lang.reflect.Method m:MainActivity.class.getDeclaredMethods())assertFalse("no obsolete remove-ads purchase entry",m.getName().equals("offerRemoveAds"));
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
    @Test public void renderFullScreenShort720x1040() throws Exception {
        MainActivity a = activity();
        renderView(a.findViewById(android.R.id.content), 720, 1040, "/tmp/renders/neonvegasslots-full-720x1040.png");
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderFullScreenLargeFont720() throws Exception {
        RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.35f;
        try {
            MainActivity a = activity();
            renderView(a.findViewById(android.R.id.content), 720, 1480, "/tmp/renders/neonvegasslots-full-720x1480-font135.png");
        } finally { RuntimeEnvironment.getApplication().getResources().getConfiguration().fontScale = 1.0f; }
    }

    private static android.widget.ScrollView findScroll(View v) {
        if (v instanceof android.widget.ScrollView) return (android.widget.ScrollView) v;
        if (v instanceof android.view.ViewGroup) { android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { android.widget.ScrollView r = findScroll(g.getChildAt(i)); if (r != null) return r; } }
        return null;
    }
    private static SlotsView findGame(View v) {
        if (v instanceof SlotsView) return (SlotsView) v;
        if (v instanceof android.view.ViewGroup) { android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { SlotsView r = findGame(g.getChildAt(i)); if (r != null) return r; } }
        return null;
    }
    private static Object rf(Object o, String n) { try { java.lang.reflect.Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); } catch (Exception e) { return e.toString(); } }
    private static MotionEvent ev(int act, float x, float y) { return MotionEvent.obtain(0, 0, act, x, y, 0); }

    private static void pass(View root, int w, int h) {
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
    }
    private static MotionEvent evT(long down, long t, int act, float x, float y) { return MotionEvent.obtain(down, t, act, x, y, 0); }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void shortScreenScrollContainerKeepsPlayControlsUsable() throws Exception {
        MainActivity a = activity();
        View root = a.findViewById(android.R.id.content);
        int w = 720, h = 1040;
        pass(root, w, h);
        android.widget.ScrollView sc = findScroll(root); SlotsView g = findGame(root);
        assertNotNull(sc); assertNotNull(g);
        android.view.ViewGroup par = (android.view.ViewGroup) sc.getParent();
        android.view.ViewGroup box = (android.view.ViewGroup) par.getChildAt(par.indexOfChild(sc) + 1);
        View ph = new View(a); ph.setBackgroundColor(0xFF7A7A7A); // realistic 50dp banner (131px at 2.625x)
        box.addView(ph, new android.widget.LinearLayout.LayoutParams(-1, 131));
        pass(root, w, h); pass(root, w, h); pass(root, w, h);
        assertTrue("banner placeholder must sit below the scroll area", box.getTop() >= sc.getBottom());
        assertTrue("game must be taller than the viewport: game=" + g.getHeight() + " viewport=" + sc.getHeight(), g.getHeight() > sc.getHeight());
        float gw = g.getWidth(), gh = g.getHeight();
        float gridTop = SlotsView.gridTop(gw,gh), cell = (g.controlsTop()-gridTop-40)/3;
        float ctrlY = g.controlsTop(), btnH = g.controlsHeight();
        sc.scrollTo(0, Math.max(0, (int)Math.ceil(ctrlY+btnH-sc.getHeight())));
        int sy = sc.getScrollY();
        assertTrue("controls bottom reachable after scroll: ctrlBottom=" + (ctrlY + btnH) + " scrollY=" + sy + " vp=" + sc.getHeight(), ctrlY + btnH - sy <= sc.getHeight());
        assertTrue("controls top visible", ctrlY - sy >= 0);
        assertTrue("reel grid remains visible after control scroll", gridTop + cell * 3 - sy > 0 && gridTop - sy > -cell);
        renderView(root, w, h, "/tmp/renders/neonvegasslots-short-initial-banner-720x1040.png");
        float cy = ctrlY + btnH * 0.5f - sc.getScrollY();
        String s0 = g.serialize(); long bal0 = g.getBalance(); long t = 100;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, gw * 0.03f + gw * 0.37f * 0.85f, cy));
        sc.dispatchTouchEvent(evT(t, t + 40, MotionEvent.ACTION_UP, gw * 0.35f, cy));
        assertEquals("BET+ via scroll container", Integer.parseInt(s0.split(";")[2]) + 1, Integer.parseInt(g.serialize().split(";")[2]));
        t = 300;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, gw * 0.03f + gw * 0.37f * 0.10f, cy));
        sc.dispatchTouchEvent(evT(t, t + 40, MotionEvent.ACTION_UP, gw * 0.05f, cy));
        assertEquals("BET- via scroll container", s0.split(";")[2], g.serialize().split(";")[2]);
        t = 500;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, gw * 0.69f, cy));
        sc.dispatchTouchEvent(evT(t, t + 40, MotionEvent.ACTION_UP, gw * 0.69f, cy));
        assertTrue("SPIN via scroll container charges the bet", g.getBalance() < bal0);
        assertFalse("short tap must not start auto", g.isAuto());
        Thread.sleep(2400);
        renderView(root, w, h, "/tmp/renders/neonvegasslots-spin-end.png"); org.robolectric.shadows.ShadowLooper.idleMainLooper(300, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertFalse("spin finished before long press", g.isSpinning());
        java.util.function.Supplier<Float> cyNow = () -> { float gh2 = g.getHeight(); return g.controlsTop() + g.controlsHeight()*.5f - sc.getScrollY(); };
        t = 4000;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, gw * 0.69f, cyNow.get()));
        org.robolectric.shadows.ShadowLooper.idleMainLooper(700, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue("long press directly starts auto (no menu)", g.isAuto());
        sc.dispatchTouchEvent(evT(t, t + 800, MotionEvent.ACTION_UP, gw * 0.69f, cyNow.get()));
        t = 6000;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, gw * 0.69f, cyNow.get()));
        sc.dispatchTouchEvent(evT(t, t + 40, MotionEvent.ACTION_UP, gw * 0.69f, cyNow.get()));
        assertFalse("tap stops auto", g.isAuto());
        pass(root, w, h); pass(root, w, h); pass(root, w, h); // idling the looper re-laid the tree out at the device size; restore the 720x1040 viewport
        sc.scrollTo(0, 0);
        assertTrue("short viewport restored: game=" + g.getHeight() + " vp=" + sc.getHeight(), g.getHeight() > sc.getHeight());
        int before = sc.getScrollY(); t = 8000; float x = gw * 0.5f, y0 = sc.getHeight() * 0.3f;
        sc.dispatchTouchEvent(evT(t, t, MotionEvent.ACTION_DOWN, x, y0));
        for (int i = 1; i <= 10; i++) sc.dispatchTouchEvent(evT(t, t + i * 16, MotionEvent.ACTION_MOVE, x, y0 - i * 30));
        sc.dispatchTouchEvent(evT(t, t + 200, MotionEvent.ACTION_UP, x, y0 - 300));
        assertTrue("finger drag must scroll the container: before=" + before + " after=" + sc.getScrollY(), sc.getScrollY() != before);
        assertFalse("a scroll drag must not leave auto on", g.isAuto());
        renderView(root, w, h, "/tmp/renders/neonvegasslots-short-afterdrag-banner-720x1040.png");
        pass(root, w, h);
        g.stopAuto();
        sc.scrollTo(0, 100000);
        assertEquals("scrolled to the bottom: game=" + g.getHeight() + " vp=" + sc.getHeight(), g.getHeight() - sc.getHeight(), sc.getScrollY());
        root.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)));
        org.robolectric.shadows.ShadowLooper.idleMainLooper(4000, java.util.concurrent.TimeUnit.MILLISECONDS);
        pass(root, w, h);
        sc.scrollTo(0, 100000);
        float gwB = g.getWidth(), ghB = g.getHeight();
        float trailTopB = g.controlsTop() + g.controlsHeight() + gwB * 0.055f, trailBottomB = ghB - gwB * 0.10f;
        java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/dbg-short.txt"), ("game=" + ghB + " vp=" + sc.getHeight() + " scrollY=" + sc.getScrollY() + " trailTop=" + trailTopB + " trailBottom=" + trailBottomB + " trailH=" + (trailBottomB - trailTopB) + " w=" + gwB).getBytes());
        { // the game view itself at its full laid-out height, cropped to the viewport window the user sees at max scroll
            android.graphics.Bitmap full = android.graphics.Bitmap.createBitmap(g.getWidth(), g.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            g.draw(new android.graphics.Canvas(full));
            android.graphics.Bitmap win = android.graphics.Bitmap.createBitmap(full, 0, sc.getScrollY(), g.getWidth(), sc.getHeight());
            java.io.FileOutputStream f2 = new java.io.FileOutputStream("/tmp/renders/neonvegasslots-short-maxscroll-window-720x645.png");
            win.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, f2); f2.close();
        }
        android.graphics.Bitmap bb = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(bb)); // draw without re-measuring so the scroll position is what the user sees
        java.io.FileOutputStream fo = new java.io.FileOutputStream("/tmp/renders/neonvegasslots-short-bottom-banner-720x1040.png");
        bb.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo); fo.close();
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
            Dialog d=latestDialog();android.widget.ScrollView sc=findScroll(d.getWindow().getDecorView());assertNotNull(sc);sc.scrollTo(0,100000);assertTrue(sc.getScrollY()>0);
            renderLatestDialog("neonvegasslots-daily-narrow-bottom",560,1240);
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

    @Test @Config(sdk = 35) public void rootPadsForSystemBarsWhenEdgeToEdge() {
        MainActivity a = activity();
        View content = a.findViewById(android.R.id.content);
        View root = ((android.view.ViewGroup) ((android.view.ViewGroup) content).getChildAt(0)).getChildAt(0);
        android.view.WindowInsets insets = new android.view.WindowInsets.Builder()
            .setInsets(android.view.WindowInsets.Type.systemBars(), android.graphics.Insets.of(6, 88, 6, 42))
            .build();
        content.dispatchApplyWindowInsets(insets);
        assertEquals("status bar inset must pad the top", 88, root.getPaddingTop());
        assertEquals("nav bar inset must pad the bottom", 42, root.getPaddingBottom());
        assertEquals(6, root.getPaddingLeft());
        assertEquals(6, root.getPaddingRight());
    }

    @Test @Config(sdk = 35) @GraphicsMode(GraphicsMode.Mode.NATIVE) // Android 15 runtime + native rasterizer: real edge-to-edge evidence
    public void renderWithSystemBarInsets() throws Exception {
        MainActivity a = activity();
        View content = a.findViewById(android.R.id.content);
        android.view.ViewGroup root = (android.view.ViewGroup) ((android.view.ViewGroup) ((android.view.ViewGroup) content).getChildAt(0)).getChildAt(0);
        android.view.WindowInsets insets = new android.view.WindowInsets.Builder()
            .setInsets(android.view.WindowInsets.Type.systemBars(), android.graphics.Insets.of(0, 88, 0, 132))
            .build();
        content.dispatchApplyWindowInsets(insets);
        assertEquals(88, root.getPaddingTop());
        assertEquals(132, root.getPaddingBottom());
        // mark the banner container with a placeholder of standard banner height so its position is visible
        android.view.ViewGroup bannerBox = (android.view.ViewGroup) root.getChildAt(root.getChildCount() - 1);
        android.widget.TextView ph = new android.widget.TextView(a);
        ph.setText("AD BANNER REGION (test placeholder)");
        ph.setGravity(android.view.Gravity.CENTER);
        ph.setTextColor(0xFFFFFFFF);
        ph.setBackgroundColor(0xFF555555);
        bannerBox.removeAllViews();
        bannerBox.addView(ph, new android.view.ViewGroup.LayoutParams(-1, (int) (50 * a.getResources().getDisplayMetrics().density)));
        android.util.DisplayMetrics dm = a.getResources().getDisplayMetrics();
        int w = dm.widthPixels, h = dm.heightPixels;
        content.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        content.draw(cv);
        android.graphics.Paint p = new android.graphics.Paint();
        p.setColor(0x66FF0000); cv.drawRect(0, 0, w, 88, p);
        p.setColor(0x660000FF); cv.drawRect(0, h - 132, w, h, p);
        p.setColor(0xFFFFFFFF); p.setTextSize(30); p.setTextAlign(android.graphics.Paint.Align.CENTER);
        cv.drawText("INJECTED STATUS BAR 88px", w / 2, 60, p);
        cv.drawText("INJECTED NAV BAR 132px", w / 2, h - 55, p);
        FileOutputStream fos = new FileOutputStream("/tmp/renders/neonvegasslots-insets-composed.png");
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        assertTrue(new File("/tmp/renders/neonvegasslots-insets-composed.png").length() > 5000);
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderMotionSequence() throws Exception {
        final long[] now = {100000};
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication()) {
            @Override long nowMillis() { return now[0]; }
        };
        v.restore("5000;0;1;0;0;10;3");
        v.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(1480, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 720, 1480);
        java.lang.reflect.Field f = SlotsView.class.getDeclaredField("spinning"); f.setAccessible(true); f.setBoolean(v, true);
        f = SlotsView.class.getDeclaredField("stopAt"); f.setAccessible(true);
        long n = now[0];
        f.set(v, new long[]{n + 4000, n + 4000, n + 4000, n + 4000, n + 4000});
        // three successive frames of the same spin must differ (real motion, not a static pose)
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-motion-t0.png");
        Bitmap b1 = Bitmap.createBitmap(720, 1480, Bitmap.Config.ARGB_8888); v.draw(new Canvas(b1));
        now[0] += 130;
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-motion-t1.png");
        Bitmap b2 = Bitmap.createBitmap(720, 1480, Bitmap.Config.ARGB_8888); v.draw(new Canvas(b2));
        now[0] += 130;
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-motion-t2.png");
        assertFalse("spin frame t1 must differ from t0 (scrolling motion)", b1.sameAs(b2));
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderAnticipationFrame() throws Exception {
        final long[] now = {100000};
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication()) {
            @Override long nowMillis() { return now[0]; }
        };
        v.restore("5000;0;1;0;0;10;3");
        v.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(1480, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 720, 1480);
        long n = now[0];
        java.lang.reflect.Field f = SlotsView.class.getDeclaredField("spinning"); f.setAccessible(true); f.setBoolean(v, true);
        f = SlotsView.class.getDeclaredField("stopAt"); f.setAccessible(true);
        f.set(v, new long[]{n - 500, n - 400, n - 300, n - 200, n + 4000}); // first four stopped, last spinning
        f = SlotsView.class.getDeclaredField("grid"); f.setAccessible(true);
        int[][] g = (int[][]) f.get(v);
        g[1][0] = 0; g[1][1] = 0; g[1][2] = 0; g[1][3] = 0; // four-of-a-kind run on the middle line
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-frame-anticipation.png");
        f = SlotsView.class.getDeclaredField("stopAt"); f.setAccessible(true);
        long[] stops = (long[]) f.get(v);
        assertTrue("anticipation extends the last reel", stops[4] >= n + 4000);
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderBigWinFrame() throws Exception {
        final long[] now = {100000};
        SlotsView v = new SlotsView(RuntimeEnvironment.getApplication()) {
            @Override long nowMillis() { return now[0]; }
        };
        v.restore("5000;0;1;0;0;10;3");
        v.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(1480, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 720, 1480);
        long n = now[0];
        java.lang.reflect.Field f = SlotsView.class.getDeclaredField("lastWin"); f.setAccessible(true); f.setLong(v, 1250);
        f = SlotsView.class.getDeclaredField("winStartAt"); f.setAccessible(true); f.setLong(v, n - 800);
        f = SlotsView.class.getDeclaredField("winFlashUntil"); f.setAccessible(true); f.setLong(v, n + 2200);
        f = SlotsView.class.getDeclaredField("bigWinStartAt"); f.setAccessible(true); f.setLong(v, n - 800);
        f = SlotsView.class.getDeclaredField("bigWinUntil"); f.setAccessible(true); f.setLong(v, n + 2200);
        f = SlotsView.class.getDeclaredField("bigWinAmount"); f.setAccessible(true); f.setLong(v, 1250);
        renderView(v, 720, 1480, "/tmp/renders/neonvegasslots-frame-bigwin.png");
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void renderSymbolSkinComposed() throws Exception {
        MainActivity a=activity();java.lang.reflect.Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView g=(SlotsView)f.get(a);
        java.lang.reflect.Field grid=SlotsView.class.getDeclaredField("grid");grid.setAccessible(true);
        grid.set(g,new int[][]{{0,1,2,3,4},{5,0,1,2,3},{4,5,0,1,2}});
        View root=a.findViewById(android.R.id.content);android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();
        renderView(root,dm.widthPixels,dm.heightPixels,"/tmp/renders/neonvegasslots-symbol-skin-composed.png");
        SlotArt art=new SlotArt();art.loadBitmaps(a);Bitmap[] frames=new Bitmap[2];
        for(int k=0;k<2;k++){frames[k]=Bitmap.createBitmap(720,300,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(frames[k]);c.drawColor(0xFF0A0715);
            for(int id=0;id<6;id++)art.symbolAnimated(c,id,60+id*120,150,106,1000+k*350,true);
        }
        boolean different=false;for(int y=0;y<300&&!different;y+=3)for(int x=0;x<720;x+=3)if(frames[0].getPixel(x,y)!=frames[1].getPixel(x,y)){different=true;break;}
        assertTrue("win frames must actually change pixels",different);
        Bitmap sheet=Bitmap.createBitmap(720,600,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(sheet);cv.drawBitmap(frames[0],0,0,null);cv.drawBitmap(frames[1],0,300,null);
        try(FileOutputStream o=new FileOutputStream("/tmp/renders/neonvegasslots-symbol-win-sheet.png")){sheet.compress(Bitmap.CompressFormat.PNG,100,o);}
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(qualifiers="w320dp-h640dp-420dpi")
    @Test public void renderCompactHudNarrow()throws Exception{
        MainActivity a=activity();java.lang.reflect.Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView g=(SlotsView)f.get(a);
        g.restore("50;0;1;"+System.currentTimeMillis()+";0;1;0");
        View root=a.findViewById(android.R.id.content);android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();
        renderView(root,dm.widthPixels,dm.heightPixels,"/tmp/renders/neonvegasslots-compact-hud-narrow.png");
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test public void balanceAnimationDoesNotChangeMoneyOrAdvancePerDraw()throws Exception{
        final long[] clock={10000};SlotsView g=new SlotsView(RuntimeEnvironment.getApplication()){@Override long nowMillis(){return clock[0];}};
        g.layout(0,0,720,1480);Bitmap b=Bitmap.createBitmap(720,1480,Bitmap.Config.ARGB_8888);g.draw(new Canvas(b));
        java.lang.reflect.Field bal=SlotsView.class.getDeclaredField("balance");bal.setAccessible(true);bal.setLong(g,1000);
        java.lang.reflect.Field shown=SlotsView.class.getDeclaredField("shownBalance");shown.setAccessible(true);
        clock[0]+=33;g.draw(new Canvas(b));long first=shown.getLong(g);assertTrue(first>500&&first<1000);
        g.draw(new Canvas(b));assertEquals(first,shown.getLong(g));assertEquals(1000,g.getBalance());
        for(int i=0;i<60;i++){clock[0]+=33;g.draw(new Canvas(b));}assertEquals(1000,shown.getLong(g));assertEquals(1000,g.getBalance());
    }
}

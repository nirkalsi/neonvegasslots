package nir.com.neonvegasslots;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real-device gameplay capture: genuine touch events (spins, bet change) for ~50 s so Test Lab records video. Debug build, sample ad IDs only. */
@RunWith(AndroidJUnit4.class)
public class GameplayCaptureTest {
    static SlotsView find(View v) {
        if (v instanceof SlotsView) return (SlotsView) v;
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) { SlotsView r = find(g.getChildAt(i)); if (r != null) return r; } }
        return null;
    }
    static void tap(Instrumentation in, int[] loc, float x, float y) {
        long t = SystemClock.uptimeMillis();
        in.sendPointerSync(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, loc[0] + x, loc[1] + y, 0));
        SystemClock.sleep(60);
        long u = SystemClock.uptimeMillis();
        in.sendPointerSync(MotionEvent.obtain(t, u, MotionEvent.ACTION_UP, loc[0] + x, loc[1] + y, 0));
    }
    @Test public void playOneMinute() throws Exception {
        final Instrumentation in = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {
            final AtomicReference<SlotsView> sv = new AtomicReference<>();
            final AtomicReference<MainActivity> act = new AtomicReference<>();
            sc.onActivity(a -> { act.set(a); sv.set(find(a.getWindow().getDecorView())); });
            SystemClock.sleep(3500);
            final long beforeSpins=sv.get().getSpinsDone();
            long end = SystemClock.uptimeMillis() + 52000;
            int n = 0;
            while (SystemClock.uptimeMillis() < end) {
                if (!act.get().hasWindowFocus()) { in.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); SystemClock.sleep(900); continue; }
                final SlotsView v = sv.get();
                final int[] loc = new int[2]; final float[] g = new float[3];
                in.runOnMainSync(() -> { v.getLocationOnScreen(loc); g[0] = v.getWidth(); g[1] = v.getHeight(); });
                float w = g[0], h = g[1];
                if(v.pendingLevelReward()>0){
                    final boolean[] ready={false};in.runOnMainSync(()->ready[0]=v.levelMomentReady());
                    if(ready[0])tap(in,loc,w*.50f,h*.43f+w*.34f);
                    SystemClock.sleep(1200);continue;
                }
                float ctrlY = SlotsView.ctrlY(w,h); float btnH = h * 0.07f;
                n++;
                if (n == 3) { tap(in, loc, w * 0.36f, ctrlY + btnH / 2); SystemClock.sleep(900); } // bet up once
                tap(in, loc, w * 0.70f, ctrlY + btnH / 2);
                SystemClock.sleep(3600);
            }
            org.junit.Assert.assertTrue("Capture must contain real completed spins, not idle taps",sv.get().getSpinsDone()>beforeSpins+2);
        }
    }
}

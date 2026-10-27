package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Coin refill without any ad: +5 per full minute under 100, cap 100, includes time away. SYNTHETIC unit evidence. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class RefillTest {
    private SlotsView view() { Context c = RuntimeEnvironment.getApplication(); return new SlotsView(c); }
    @Test public void zeroCoinsRefillFiveEachMinute_andKeepsPartialMinute() {
        SlotsView v = view(); long t0 = 1_000_000L;
        v.setRefillStateForTest(0, t0);
        v.regenAt(t0 + 59_000); assertEquals(0, v.balanceForTest());
        v.regenAt(t0 + 60_000); assertEquals(5, v.balanceForTest());
        v.regenAt(t0 + 150_000); assertEquals(10, v.balanceForTest()); // second minute done at 120s, 30s into the third kept
        assertEquals(t0 + 120_000, v.lastRegenAtForTest());
    }
    @Test public void timeAwayIsCredited_andCappedAt100() {
        SlotsView v = view(); long t0 = 5_000_000L;
        v.setRefillStateForTest(0, t0);
        v.regenAt(t0 + 7 * 60_000); assertEquals(35, v.balanceForTest());
        v.regenAt(t0 + 10 * 3600_000L); assertEquals(100, v.balanceForTest());
    }
    @Test public void spendingFromTheCapDoesNotInstantlyRefill() {
        SlotsView v = view(); long t0 = 9_000_000L;
        v.setRefillStateForTest(100, t0);
        v.regenAt(t0 + 3600_000L); // idle an hour at the cap
        v.setRefillStateForTest(50, v.lastRegenAtForTest()); // player spends down
        v.regenAt(t0 + 3600_000L + 1_000); assertEquals(50, v.balanceForTest());
    }
    @Test public void aboveCapIsNeverReduced() {
        SlotsView v = view(); v.setRefillStateForTest(5000, 0); v.regenAt(10_000_000L); assertEquals(5000, v.balanceForTest());
    }
}

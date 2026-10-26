package nir.com.neonvegasslots;

import static org.junit.Assert.*;
import org.junit.Test;

public class BetLadderTest {
    @Test public void ladderStartsSixAndGrowsWithLevel() {
        assertEquals(6, SlotsView.betCount(1));
        assertEquals(7, SlotsView.betCount(10));
        assertEquals(12, SlotsView.betCount(100));
        assertEquals(25000, SlotsView.betAmount(11));
    }
    @Test public void ladderIsStrictlyAscendingAndUnlocksAscend() {
        for (int i = 1; i < 12; i++) {
            assertTrue(SlotsView.betAmount(i) > SlotsView.betAmount(i - 1));
            assertTrue(SlotsView.BET_UNLOCK[i] >= SlotsView.BET_UNLOCK[i - 1]);
        }
    }
    @Test public void xpDoesNotLetBigBetsFarmLevels() {
        assertEquals(5, SlotsView.xpForBet(5));
        assertEquals(200, SlotsView.xpForBet(200));
        assertTrue(SlotsView.xpForBet(25000) < 3000);
    }
}

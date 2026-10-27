package nir.com.neonvegasslots;
import org.junit.Test;
import static org.junit.Assert.*;
public class WinTierTest {
  @Test public void tiersFollowBetMultiple() {
    assertEquals(0, SlotsView.winTier(25 * 10, 10));
    assertEquals(0, SlotsView.winTier(59 * 10, 10));
    assertEquals(1, SlotsView.winTier(60 * 10, 10));
    assertEquals(1, SlotsView.winTier(149 * 10, 10));
    assertEquals(2, SlotsView.winTier(150 * 10, 10));
    assertTrue(SlotsView.TIER_MS[2] > SlotsView.TIER_MS[1] && SlotsView.TIER_MS[1] > SlotsView.TIER_MS[0]);
  }
}

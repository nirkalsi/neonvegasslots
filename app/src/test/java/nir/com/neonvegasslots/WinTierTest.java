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
  @Test public void presentationUpgradesWithCountNotFinalAmount(){assertEquals(0,SlotsView.presentationTier(10,10,2));assertEquals(1,SlotsView.presentationTier(600,10,2));assertEquals(2,SlotsView.presentationTier(1500,10,2));assertEquals(0,SlotsView.presentationTier(9999,10,0));assertEquals(0,SlotsView.shownTierWin(2000,0,4600));assertEquals(2000,SlotsView.shownTierWin(2000,4600,4600));assertTrue(SlotsView.shownTierWin(2000,1200,4600)<SlotsView.shownTierWin(2000,2400,4600));}
 @Test public void signSwitchesOnOneLetterAtATime(){assertEquals(1,SlotsView.neonLitLetters("BIG WIN!",0));assertEquals(3,SlotsView.neonLitLetters("BIG WIN!",130));assertEquals(8,SlotsView.neonLitLetters("BIG WIN!",1000));}
}

package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.view.MotionEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class RedesignInputTest {
 private static MotionEvent down(float x,float y){return MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,x,y,0);}
 private static float nodeY(){float w=720,h=1480;float top=SlotsView.ctrlY(w,h)+h*.07f+w*.055f;float bottom=h-w*.10f;return top+Math.min(bottom-top,w*.62f)*.40f;}
 @Test public void artDoesNotMoveControlsOrChangeStartingEconomy(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication()); v.layout(0,0,720,1480);
  long original=v.getBalance(); assertEquals(500,original);
  float cy=SlotsView.ctrlY(720,1480)+20;
  v.onTouchEvent(down(720*.69f,cy));
  assertEquals(original-10,v.getBalance());
  v.onTouchEvent(down(720*.69f,cy));
  assertEquals("Second tap during spin must not double-charge",original-10,v.getBalance());
 }
 @Test public void lockedTrailTapDoesNotChangeState(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.layout(0,0,720,1480);
  String before=v.serialize();
  v.onTouchEvent(down(SlotArt.nodeX(720,2),nodeY())); // LAGOON needs Lv 6, we are Lv 1
  assertEquals(before,v.serialize());
  assertEquals(0,v.getThemeIdx());
 }
 @Test public void trailTapAppliesUnlockedThemeAndPersists(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.layout(0,0,720,1480);
  v.restore("500;0;1;0;0;10;0"); // level 10 unlocks every stop
  MapView mv=new MapView(RuntimeEnvironment.getApplication(),v); mv.measure(android.view.View.MeasureSpec.makeMeasureSpec(720,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED)); mv.layout(0,0,720,mv.getMeasuredHeight());
  mv.onTouchEvent(down(SlotArt.nodeX(720,3),Math.min(mv.getHeight(),720*.68f)*.38f)); // the map now lives in a dialog
  assertEquals(3,v.getThemeIdx());
  assertEquals("3",v.serialize().split(";")[6]);
  SlotsView v2=new SlotsView(RuntimeEnvironment.getApplication());
  v2.restore(v.serialize());
  assertEquals("theme must survive save/restore",3,v2.getThemeIdx());
 }
 @Test public void lockedThemeInSaveFallsBackToDefault(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  v.restore("500;0;1;0;0;1;3"); // level 1 may not hold ROYAL (Lv 10)
  assertEquals(0,v.getThemeIdx());
 }
 @Test public void unlockCountFollowsLevel(){
  assertEquals(1,SlotsView.unlockedCount(1));
  assertEquals(1,SlotsView.unlockedCount(2));
  assertEquals(2,SlotsView.unlockedCount(3));
  assertEquals(3,SlotsView.unlockedCount(6));
  assertEquals(4,SlotsView.unlockedCount(10));
 }
 @Test public void selectThemeRejectsLockedAndOutOfRange(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  assertFalse(v.selectTheme(1));
  assertFalse(v.selectTheme(-1));
  assertFalse(v.selectTheme(99));
  assertTrue(v.selectTheme(0));
 }

 @Test public void firstSpinOfDayGuaranteesTrophy(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  assertTrue(v.tryTrophyDrop(0.99f,"2026-10-24")); // roll too high, but first-of-day wins
  assertEquals(1,Integer.bitCount(v.getTrophyMask()));
 }
 @Test public void sameDayAboveChanceDropsNothing(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  assertTrue(v.tryTrophyDrop(0.99f,"2026-10-24"));
  assertFalse("same day, roll above chance must not drop",v.tryTrophyDrop(0.99f,"2026-10-24"));
  assertEquals(1,Integer.bitCount(v.getTrophyMask()));
 }
 @Test public void chanceDropWithinBounds(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  v.tryTrophyDrop(0.99f,"2026-10-24"); // daily guarantee consumed
  assertFalse(v.tryTrophyDrop(0.08f,"2026-10-24")); // exactly at threshold: no drop
  assertTrue(v.tryTrophyDrop(0.079f,"2026-10-24")); // under chance: drop
  assertTrue(SlotsView.TROPHY_CHANCE>=0.05f && SlotsView.TROPHY_CHANCE<=0.15f);
 }
 @Test public void newDayGuaranteesAgain(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  v.tryTrophyDrop(0.99f,"2026-10-24");
  assertTrue(v.tryTrophyDrop(0.99f,"2026-10-25"));
  assertEquals(2,Integer.bitCount(v.getTrophyMask()));
 }
 @Test public void fullShelfStopsDrops(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  v.restore("500;0;1;0;0;10;0;4095;2026-10-24");
  assertFalse(v.tryTrophyDrop(0.0f,"2026-10-24"));
  assertTrue(v.setComplete(0) && v.setComplete(3));
 }
 @Test public void trophiesCostNothingAndSurviveSave(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());
  long before=v.getBalance();
  v.tryTrophyDrop(0.0f,"2026-10-24");
  assertEquals("trophies must never change the balance",before,v.getBalance());
  SlotsView v2=new SlotsView(RuntimeEnvironment.getApplication());
  v2.restore(v.serialize());
  assertEquals(v.getTrophyMask(),v2.getTrophyMask());
 }
}

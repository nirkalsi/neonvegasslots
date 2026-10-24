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
 @Test public void artDoesNotMoveControlsOrChangeStartingEconomy(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication()); v.layout(0,0,720,1480);
  long original=v.getBalance(); assertEquals(500,original);
  float cy=1480*.22f+(720/5f-8)*3+16+24+20;
  v.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,720*.69f,cy,0));
  assertEquals(original-10,v.getBalance());
  v.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,720*.69f,cy,0));
  assertEquals("Second tap during spin must not double-charge",original-10,v.getBalance());
 }
 @Test public void journeyIsInformationalNotAHiddenSpendTarget(){
  SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.layout(0,0,720,1480);
  String before=v.serialize();
  v.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,360,1100,0));
  assertEquals(before,v.serialize());
 }
}

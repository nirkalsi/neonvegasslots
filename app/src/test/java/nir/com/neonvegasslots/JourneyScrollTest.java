package nir.com.neonvegasslots;
import static org.junit.Assert.*;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.view.*;import android.widget.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class JourneyScrollTest {
 @Test public void actualFingerDragScrollsWithoutSelecting(){
  android.content.Context c=RuntimeEnvironment.getApplication();SlotsView game=new SlotsView(c);game.restore("500;0;1;0;0;16;0");
  ScrollView scroll=new ScrollView(c);MapView map=new MapView(c,game);scroll.addView(map);scroll.measure(View.MeasureSpec.makeMeasureSpec(720,1073741824),View.MeasureSpec.makeMeasureSpec(950,1073741824));scroll.layout(0,0,720,950);
  long t=android.os.SystemClock.uptimeMillis();scroll.dispatchTouchEvent(MotionEvent.obtain(t,t,0,350,800,0));
  for(int i=1;i<=12;i++)scroll.dispatchTouchEvent(MotionEvent.obtain(t,t+i*16,2,350,800-i*45,0));
  scroll.dispatchTouchEvent(MotionEvent.obtain(t,t+220,1,350,260,0));assertTrue(scroll.getScrollY()>0);assertEquals(0,game.getThemeIdx());
 }
}
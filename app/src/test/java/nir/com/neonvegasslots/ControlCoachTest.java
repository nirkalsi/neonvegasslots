package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import android.graphics.*;import android.view.MotionEvent;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w360dp-h740dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ControlCoachTest {
 @Test public void tipDismissalPersistsWithoutStartingSpin()throws Exception{
  android.content.Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("control_coach",0).edit().clear().commit();
  SlotsView v=new SlotsView(c);v.layout(0,0,720,1480);v.draw(new Canvas(Bitmap.createBitmap(720,1480,Bitmap.Config.ARGB_8888)));
  assertFalse(c.getSharedPreferences("control_coach",0).getBoolean("hold_auto_seen",false));
  v.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,1,1,0));assertTrue(c.getSharedPreferences("control_coach",0).getBoolean("hold_auto_seen",false));assertEquals(0,v.getSpinsDone());
  SlotsView restored=new SlotsView(c);java.lang.reflect.Method m=SlotsView.class.getDeclaredMethod("controlTipSeen");m.setAccessible(true);assertTrue((Boolean)m.invoke(restored));
 }
 @Test public void nextRewardPhrase(){assertEquals("Lv 2: +300 coins",SlotsView.nextLevelRewardLabel(1));assertEquals("Lv 101: +15150 coins",SlotsView.nextLevelRewardLabel(100));}
 @Test @Config(qualifiers="w320dp-h570dp-hdpi") public void smallLargeFontPreview()throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication();c.getResources().getConfiguration().fontScale=1.35f;
  c.getSharedPreferences("control_coach",0).edit().clear().commit();Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
  MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();android.view.View root=a.findViewById(android.R.id.content);
  root.measure(android.view.View.MeasureSpec.makeMeasureSpec(480,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(854,1073741824));root.layout(0,0,480,854);
  Bitmap b=Bitmap.createBitmap(480,854,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));try(java.io.FileOutputStream f=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-controls-small135.png")){b.compress(Bitmap.CompressFormat.PNG,100,f);}
 }
}

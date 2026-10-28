package nir.com.neonvegasslots;
import android.graphics.*;import android.view.*;import java.lang.reflect.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w320dp-h570dp-hdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StateTypographyAuditTest{
 @Test public void narrowLargeFontStates()throws Exception{
  android.content.Context c=RuntimeEnvironment.getApplication();c.getResources().getDisplayMetrics().scaledDensity=c.getResources().getDisplayMetrics().density*1.35f;
  for(String state:new String[]{"low","level"}){SlotsView v=new SlotsView(c);v.restore(state.equals("low")?"5;0;1;0;20;1;0":"500;5;1;0;0;2;0;0;;300");if(state.equals("low")){Field b=SlotsView.class.getDeclaredField("balance"),r=SlotsView.class.getDeclaredField("lastRegenAt");b.setAccessible(true);r.setAccessible(true);b.setLong(v,5L);r.setLong(v,System.currentTimeMillis());}int h=Math.max(850,v.minimumContentHeight(480));v.layout(0,0,480,h);if(state.equals("level")){Field f=SlotsView.class.getDeclaredField("levelMomentAt");f.setAccessible(true);f.setLong(v,System.currentTimeMillis()-600);}
   Bitmap b=Bitmap.createBitmap(480,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(java.io.FileOutputStream o=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-audit-"+state+".png")){b.compress(Bitmap.CompressFormat.PNG,100,o);}
  }
 }
}

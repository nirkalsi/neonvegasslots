package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.lang.reflect.Field;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RefillScreenTest {
 @Test public void fullRefillScreen() throws Exception {
  Splash.enabled=false; Splash.pulse=false; Reminders.promptEnabled=false;
  MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
  Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);
  SlotsView g=(SlotsView)f.get(a);g.restore("0;0;0;"+(System.currentTimeMillis()-30000)+";0;1;0");
  View root=a.findViewById(android.R.id.content);
  android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics(); int w=dm.widthPixels,h=dm.heightPixels;
  root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);
  Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bm));
  new java.io.File("/tmp/renders").mkdirs();
  try(FileOutputStream out=new FileOutputStream("/tmp/renders/neonvegasslots-refill-full.png")){bm.compress(Bitmap.CompressFormat.PNG,100,out);}
  assertFalse("New NO ADS tile must not be shown",hasText(root,"NO ADS"));
 }
 private boolean hasText(View v,String text){
  if(v instanceof android.widget.TextView && ((android.widget.TextView)v).getText().toString().equals(text))return true;
  if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasText(g.getChildAt(i),text))return true;}
  return false;
 }
}

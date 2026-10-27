package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.view.View;import android.graphics.*;import java.io.*;import java.lang.reflect.*;
import org.junit.Test;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w411dp-h891dp-420dpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AutoMenuTest {
 @Test public void runLimitStopsWithoutAnotherCharge_andSettingsAreNotSaved() throws Exception {
  SlotsView g=new SlotsView(RuntimeEnvironment.getApplication());g.restore("50000;0;0;0;0;1;0");
  g.setTurbo(true);g.startAuto(10);assertTrue(g.isAuto());assertEquals(10,g.autoLimit());assertEquals(49995,g.getBalance());
  Field spin=SlotsView.class.getDeclaredField("spinning");spin.setAccessible(true);spin.setBoolean(g,false);
  Field count=SlotsView.class.getDeclaredField("autoCount");count.setAccessible(true);count.setInt(g,10);
  Method step=SlotsView.class.getDeclaredMethod("autoStep");step.setAccessible(true);step.invoke(g);
  assertFalse(g.isAuto());assertEquals(49995,g.getBalance());
  SlotsView h=new SlotsView(RuntimeEnvironment.getApplication());h.restore(g.serialize());assertFalse(h.isAuto());assertFalse(h.isTurbo());
 }
 @Test public void autoMenuShowsCountsTurboAndCancel() throws Exception {
  Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
  MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
  a.onOpenAuto();assertNotNull(GameDialog.cur);assertTrue(GameDialog.cur.isShowing());
  View v=GameDialog.cur.getWindow().getDecorView();
  assertTrue(has(v,"10 spins"));assertTrue(has(v,"25 spins"));assertTrue(has(v,"50 spins"));assertTrue(has(v,"Until a feature"));assertTrue(has(v,"Cancel"));assertTrue(has(v,"Turbo - faster reel stops"));
  int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;
  View root=a.findViewById(android.R.id.content);root.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));root.layout(0,0,w,h);
  Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);root.draw(c);c.drawColor(0x99000000);
  int dw=GameDialog.cur.getWindow().getAttributes().width;v.measure(View.MeasureSpec.makeMeasureSpec(dw,1073741824),View.MeasureSpec.makeMeasureSpec(0,0));int dh=Math.min(v.getMeasuredHeight(),(int)(h*.78f));v.measure(View.MeasureSpec.makeMeasureSpec(dw,1073741824),View.MeasureSpec.makeMeasureSpec(dh,1073741824));v.layout(0,0,dw,dh);c.save();c.translate((w-dw)/2f,(h-dh)/2f);v.draw(c);c.restore();
  try(FileOutputStream out=new FileOutputStream("/tmp/renders/neonvegasslots-auto-menu-full.png")){bm.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 boolean has(View v,String s){if(v instanceof android.widget.TextView&&((android.widget.TextView)v).getText().toString().equals(s))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(has(g.getChildAt(i),s))return true;}return false;}
}

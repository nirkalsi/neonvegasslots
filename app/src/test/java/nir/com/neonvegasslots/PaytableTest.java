package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.view.View;import android.graphics.*;import java.io.*;import java.lang.reflect.*;
import org.junit.Test;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w411dp-h891dp-420dpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PaytableTest {
 @Test public void paytableShowsRealPayouts() throws Exception {
  Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
  MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
  assertFalse("auto menu removed",java.util.Arrays.stream(MainActivity.class.getDeclaredMethods()).anyMatch(m -> m.getName().equals("showAutoMenu")));
  a.onOpenPaytable();assertNotNull(GameDialog.cur);assertTrue(GameDialog.cur.isShowing());
  View v=GameDialog.cur.getWindow().getDecorView();
  assertTrue(has(v,"Paytable & paylines"));assertTrue(has(v,"Close"));
  assertEquals(1200,SlotsView.symbolPayout(0,5));assertEquals(96,SlotsView.symbolPayout(0,3));

  int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;
  View root=a.findViewById(android.R.id.content);root.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));root.layout(0,0,w,h);
  Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);root.draw(c);c.drawColor(0x99000000);
  int dw=GameDialog.cur.getWindow().getAttributes().width;v.measure(View.MeasureSpec.makeMeasureSpec(dw,1073741824),View.MeasureSpec.makeMeasureSpec(0,0));int dh=Math.min(v.getMeasuredHeight(),(int)(h*.78f));v.measure(View.MeasureSpec.makeMeasureSpec(dw,1073741824),View.MeasureSpec.makeMeasureSpec(dh,1073741824));v.layout(0,0,dw,dh);c.save();c.translate((w-dw)/2f,(h-dh)/2f);v.draw(c);c.restore();
  try(FileOutputStream out=new FileOutputStream("/tmp/renders/neonvegasslots-paytable-full.png")){bm.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 boolean has(View v,String s){if(v instanceof android.widget.TextView&&((android.widget.TextView)v).getText().toString().equals(s))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(has(g.getChildAt(i),s))return true;}return false;}
}

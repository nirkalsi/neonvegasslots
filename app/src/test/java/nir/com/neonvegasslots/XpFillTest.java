package nir.com.neonvegasslots;
import static org.junit.Assert.*;import java.lang.reflect.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34) public class XpFillTest{
 void set(SlotsView v,String n,Object x)throws Exception{Field f=SlotsView.class.getDeclaredField(n);f.setAccessible(true);f.set(v,x);}
 @Test public void displayInterpolatesButEarnedXpIsImmediate()throws Exception{SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.restore("500;0;1;0;100;1;0");assertEquals(100,v.displayedXp(5000),.01);set(v,"xpAnimFrom",100f);set(v,"xp",200L);set(v,"xpAnimAt",1000L);assertEquals(100,v.displayedXp(1000),.01);assertTrue(v.displayedXp(1200)>100);assertTrue(v.displayedXp(1200)<200);assertEquals(200,v.displayedXp(1450),.01);assertTrue(v.serialize().contains(";200;"));}
 @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test public void badgeFillPreview()throws Exception{SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.restore("500;0;1;0;240;1;0");v.layout(0,0,640,1040);android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(640,1040,android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));try(java.io.FileOutputStream f=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-xpfill.png")){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}}
 static class ClockView extends SlotsView{long clock;ClockView(android.content.Context c){super(c);}@Override long nowMillis(){return clock==0?System.currentTimeMillis():clock;}}
 @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test public void bothXpBarsShareOneDisplayClockWithoutChangingEarnedXp()throws Exception{
  ClockView v=new ClockView(RuntimeEnvironment.getApplication());v.restore("500;0;1;"+System.currentTimeMillis()+";100;1;0");v.layout(0,0,640,1040);long start=System.currentTimeMillis();set(v,"xpAnimFrom",100f);set(v,"xp",300L);set(v,"xpAnimAt",start);
  for(int elapsed:new int[]{0,225,450}){v.clock=start+elapsed;android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(640,1040,android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));assertTrue(v.serialize().contains(";300;1;0;"));try(java.io.FileOutputStream f=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-xp-sync-"+elapsed+".png")){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}}
 }

}

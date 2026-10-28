package nir.com.neonvegasslots;
import static org.junit.Assert.*;import android.graphics.*;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w360dp-h740dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ResponsiveContentTest {
 static SlotsView find(View v){if(v instanceof SlotsView)return(SlotsView)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++){SlotsView s=find(group.getChildAt(i));if(s!=null)return s;}}return null;}
 @Test public void contentFitsAndMaxTouchMatchesPixels()throws Exception{
  for(int[] size:new int[][]{{720,1480},{480,854},{720,1040},{1080,2243}}){
   android.content.Context c=RuntimeEnvironment.getApplication();c.getResources().getConfiguration().fontScale=1.35f;Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
   MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();View root=a.findViewById(android.R.id.content);int w=size[0],h=size[1];root.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));root.layout(0,0,w,h);SlotsView g=find(root);assertNotNull(g);
   assertTrue("all content inside game canvas",g.contentBottomForTest()<=g.getHeight()+1);ScrollView sc=(ScrollView)g.getParent();if(h>=1480)assertTrue("normal screen fits without artificial scroll",g.getHeight()<=sc.getHeight()+1);
   long balance=g.getBalance();float cy=g.controlsTop()+g.controlsHeight()+20;g.onTouchEvent(MotionEvent.obtain(0,0,0,w*.20f,cy,0));assertEquals(5,g.betIdx());assertEquals(balance,g.getBalance());assertEquals(0,g.getSpinsDone());
   Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));try(java.io.FileOutputStream f=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-responsive-"+w+"x"+h+".png")){b.compress(Bitmap.CompressFormat.PNG,100,f);}
   if(g.getHeight()>sc.getHeight()){sc.scrollTo(0,g.getHeight()-sc.getHeight());assertTrue(sc.getScrollY()>0);assertTrue("map reachable",g.contentBottomForTest()-sc.getScrollY()<=sc.getHeight()+1);}
   if(g.getHeight()>sc.getHeight()){Bitmap bottom=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bottom));try(java.io.FileOutputStream f=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-responsive-bottom-"+w+"x"+h+".png")){bottom.compress(Bitmap.CompressFormat.PNG,100,f);}}
   a.finish();
  }
 }
}

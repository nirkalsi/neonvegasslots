package nir.com.neonvegasslots;
import static org.junit.Assert.*;import android.graphics.*;import android.view.*;import java.lang.reflect.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE) public class LevelCollectTouchTest{
 SlotsView game()throws Exception{SlotsView v=new SlotsView(RuntimeEnvironment.getApplication());v.restore("500;5;1;0;0;2;0;0;;300");v.layout(0,0,720,1480);Field f=SlotsView.class.getDeclaredField("levelMomentAt");f.setAccessible(true);f.setLong(v,System.currentTimeMillis()-600);draw(v,"initial");return v;}
 void draw(SlotsView v,String tag)throws Exception{Bitmap b=Bitmap.createBitmap(720,1480,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(java.io.FileOutputStream o=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-collect-"+tag+".png")){b.compress(Bitmap.CompressFormat.PNG,100,o);}}
 RectF button(SlotsView v)throws Exception{Field f=SlotsView.class.getDeclaredField("levelCollect");f.setAccessible(true);return (RectF)f.get(v);}
 void touch(SlotsView v,int a,float x,float y){v.onTouchEvent(MotionEvent.obtain(0,20,a,x,y,0));}
 @Test public void pressDoesNotCreditUntilMatchingRelease()throws Exception{SlotsView v=game();RectF r=button(v);touch(v,0,r.centerX(),r.centerY());assertEquals(500,v.getBalance());assertEquals(300,v.pendingLevelReward());draw(v,"pressed");touch(v,1,r.centerX(),r.centerY());assertEquals(800,v.getBalance());assertEquals(0,v.pendingLevelReward());touch(v,1,r.centerX(),r.centerY());assertEquals(800,v.getBalance());}
 @Test public void cancelAndReleaseOutsideNeverCredit()throws Exception{SlotsView v=game();RectF r=button(v);touch(v,0,r.centerX(),r.centerY());touch(v,3,r.centerX(),r.centerY());assertEquals(500,v.getBalance());touch(v,0,r.centerX(),r.centerY());touch(v,1,0,0);assertEquals(500,v.getBalance());assertEquals(300,v.pendingLevelReward());}
}

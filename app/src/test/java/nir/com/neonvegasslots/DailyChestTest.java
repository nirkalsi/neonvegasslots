package nir.com.neonvegasslots;
import static org.junit.Assert.*;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.view.*;import android.graphics.*;import java.io.*;import java.lang.reflect.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,qualifiers="w411dp-h891dp-420dpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DailyChestTest {
 @Test public void duplicateClaimPaysOnlyOnceAndOpenChestFrame()throws Exception{
 Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
 Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);SlotsView g=(SlotsView)f.get(a);long before=g.getBalance();Method m=MainActivity.class.getDeclaredMethod("grantDaily",int.class,long.class);m.setAccessible(true);m.invoke(a,1,200L);m.invoke(a,1,200L);assertEquals(before+200,g.getBalance());
 View root=a.findViewById(android.R.id.content);int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;
 android.view.ViewGroup rg=(android.view.ViewGroup)root;DailyChest chest=null;for(int k=0;k<rg.getChildCount();k++)if(rg.getChildAt(k) instanceof DailyChest)chest=(DailyChest)rg.getChildAt(k);assertNotNull(chest);chest.setProgress(.8f);
 root.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));root.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));try(FileOutputStream o=new FileOutputStream("/tmp/renders/neonvegasslots-daily-chest-open-full.png")){b.compress(Bitmap.CompressFormat.PNG,100,o);}
 }
 @Test public void detachedChestCannotStartCoinFlight()throws Exception{
 Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
 MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
 Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);View g=(View)f.get(a);
 DailyChest.celebrate(a,g,200,20,20);
 ViewGroup root=(ViewGroup)a.findViewById(android.R.id.content);DailyChest chest=null;
 for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof DailyChest)chest=(DailyChest)root.getChildAt(i);
 assertNotNull(chest);root.removeView(chest);
 org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1000));
 ViewGroup decor=(ViewGroup)g.getRootView();for(int i=0;i<decor.getChildCount();i++)assertFalse(decor.getChildAt(i) instanceof ClaimBurst);
 }
 @Test public void finishingActivityDoesNotStartCoinFlight()throws Exception{
 Splash.enabled=false;Splash.pulse=false;Reminders.promptEnabled=false;
 MainActivity a=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
 Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);View g=(View)f.get(a);
 DailyChest.celebrate(a,g,200,20,20);a.finish();
 org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1000));
 ViewGroup decor=(ViewGroup)g.getRootView();for(int i=0;i<decor.getChildCount();i++)assertFalse(decor.getChildAt(i) instanceof ClaimBurst);
 }
}
package nir.com.neonvegasslots;
import static org.junit.Assert.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.graphics.*;import java.lang.reflect.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ReelLandingCandidateTest{
 static class ClockView extends SlotsView{long clock=System.currentTimeMillis();ClockView(android.content.Context c){super(c,false,true);}@Override long nowMillis(){return clock==0?System.currentTimeMillis():clock;}}
 @Test public void defaultDoesNotEnableCandidate()throws Exception{Field f=SlotsView.class.getDeclaredField("smoothLanding");f.setAccessible(true);assertFalse(f.getBoolean(new SlotsView(RuntimeEnvironment.getApplication())));}
 @Test public void captureFinalApproachAndFrozenNeighborRows()throws Exception{ClockView v=new ClockView(RuntimeEnvironment.getApplication());v.layout(0,0,640,1040);Method spin=SlotsView.class.getDeclaredMethod("spin");spin.setAccessible(true);spin.invoke(v);Field started=SlotsView.class.getDeclaredField("spinStartedAt"),stops=SlotsView.class.getDeclaredField("stopAt"),grid=SlotsView.class.getDeclaredField("grid");for(Field f:new Field[]{started,stops,grid})f.setAccessible(true);long start=started.getLong(v);int[][] initial=(int[][])grid.get(v);for(int r=0;r<3;r++)initial[r][0]=r+1;int[][] before=null;
 for(int left:new int[]{180,120,60,10,0,-40}){v.clock=start+780-left;long[] times=(long[])stops.get(v);for(int col=1;col<5;col++)times[col]=start+3000;Bitmap b=Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));int[][] g=(int[][])grid.get(v);if(before==null){before=new int[3][5];for(int r=0;r<3;r++)before[r]=g[r].clone();}else for(int r=0;r<3;r++)assertEquals(before[r][0],g[r][0]);try(java.io.FileOutputStream out=new java.io.FileOutputStream("/tmp/renders/neonvegasslots-landing-left"+left+".png")){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
 }
 @Test public void quickStopAndAnticipationReplanTheActualView()throws Exception{
  ClockView v=new ClockView(RuntimeEnvironment.getApplication());v.layout(0,0,640,1040);
  Method spin=SlotsView.class.getDeclaredMethod("spin"),quick=SlotsView.class.getDeclaredMethod("quickStop");spin.setAccessible(true);quick.setAccessible(true);spin.invoke(v);
  Field start=SlotsView.class.getDeclaredField("spinStartedAt"),motions=SlotsView.class.getDeclaredField("landingMotion"),stops=SlotsView.class.getDeclaredField("stopAt");for(Field f:new Field[]{start,motions,stops})f.setAccessible(true);
  long t=start.getLong(v);v.clock=t+220;Bitmap b=Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
  ReelLandingMotion m=((ReelLandingMotion[])motions.get(v))[0];double phase=m.phase(v.clock),velocity=m.velocity(v.clock);quick.invoke(v);assertEquals(t+330,((long[])stops.get(v))[0]);v.draw(new Canvas(b));assertEquals(phase,m.phase(v.clock),1e-8);assertEquals(velocity,m.velocity(v.clock),1e-8);
  v.clock=t+250;phase=m.phase(v.clock);velocity=m.velocity(v.clock);((long[])stops.get(v))[0]+=500;v.draw(new Canvas(b));assertEquals(phase,m.phase(v.clock),1e-8);assertEquals(velocity,m.velocity(v.clock),1e-8);
 }
 @Test public void captureSequentialSyntheticAnimation()throws Exception{
  ClockView v=new ClockView(RuntimeEnvironment.getApplication());v.layout(0,0,640,1040);Method spin=SlotsView.class.getDeclaredMethod("spin");spin.setAccessible(true);spin.invoke(v);Field started=SlotsView.class.getDeclaredField("spinStartedAt");started.setAccessible(true);long start=started.getLong(v);new java.io.File("/tmp/renders/neonvegasslots-landing-animation").mkdirs();
  for(int frame=0;frame<=95;frame++){v.clock=start+frame*16;Bitmap b=Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(java.io.FileOutputStream out=new java.io.FileOutputStream(String.format("/tmp/renders/neonvegasslots-landing-animation/frame-%03d.png",frame))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
 }

}

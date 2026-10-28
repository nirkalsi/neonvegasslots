package nir.com.neonvegasslots;
import static org.junit.Assert.*;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.graphics.*;import java.lang.reflect.*;import java.util.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ReelSamplingPreservationTest {
 static class TraceRandom extends Random {int calls;@Override public float nextFloat(){calls++;return super.nextFloat();}TraceRandom(long seed){super(seed);}@Override public int nextInt(int n){calls++;return super.nextInt(n);}}
 static class View extends SlotsView {long clock;View(boolean candidate){super(RuntimeEnvironment.getApplication(),false,candidate);}@Override long nowMillis(){return clock==0?System.currentTimeMillis():clock;}@Override public boolean post(Runnable r){return true;}}
 static Field field(String n)throws Exception{Field f=SlotsView.class.getDeclaredField(n);f.setAccessible(true);return f;}
 @Test public void matchedFrameTracesKeepEveryDrawGridDeadlineAndAward()throws Exception{
  for(int scenario=0;scenario<3;scenario++){
   View ordinary=new View(false),candidate=new View(true);TraceRandom a=new TraceRandom(913+scenario),b=new TraceRandom(913+scenario);field("rng").set(ordinary,a);field("rng").set(candidate,b);
   Method spin=SlotsView.class.getDeclaredMethod("spin");spin.setAccessible(true);
   for(View v:new View[]{ordinary,candidate}){v.layout(0,0,640,1040);spin.invoke(v);field("spinStartedAt").setLong(v,120000);field("lastRegenAt").setLong(v,120000);long[] deadlines=(long[])field("stopAt").get(v);for(int c=0;c<5;c++)deadlines[c]=120600+c*150;int[][] g=(int[][])field("grid").get(v);for(int r=0;r<3;r++)for(int c=0;c<5;c++)g[r][c]=(r+c)%6;}
   Bitmap bmp=Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bmp);
   for(int ms=0;ms<=2600;ms+=scenario==2?53:30){
    for(View v:new View[]{ordinary,candidate}){v.clock=120000+ms;long[] deadlines=(long[])field("stopAt").get(v);if(scenario==1&&ms==240)for(int c=0;c<5;c++)deadlines[c]=v.clock+110*(c+1);if(scenario==2&&ms==1060){int[][] g=(int[][])field("grid").get(v);for(int r=0;r<3;r++)for(int c=0;c<4;c++)g[r][c]=1;}v.draw(canvas);}
    assertEquals(a.calls,b.calls);int[][] ga=(int[][])field("grid").get(ordinary),gb=(int[][])field("grid").get(candidate);for(int r=0;r<3;r++)assertArrayEquals(ga[r],gb[r]);assertArrayEquals((long[])field("stopAt").get(ordinary),(long[])field("stopAt").get(candidate));assertEquals(SlotsView.evaluate(ga,10,null),SlotsView.evaluate(gb,10,null));
   }
   if(scenario==2){assertTrue(field("anticipationGiven").getBoolean(ordinary));assertTrue(field("anticipationGiven").getBoolean(candidate));}
   assertTrue(field("logicalRewardAt").getLong(candidate)>0);field("logicalRewardAt").setLong(candidate,System.currentTimeMillis());field("lastRegenAt").setLong(ordinary,120000);field("lastRegenAt").setLong(candidate,120000);Method finish=SlotsView.class.getDeclaredMethod("finishSpin");finish.setAccessible(true);finish.invoke(ordinary);finish.invoke(candidate);assertEquals(ordinary.serialize(),candidate.serialize());assertEquals(ordinary.getLastWin(),candidate.getLastWin());assertEquals(a.calls,b.calls);String settled=candidate.serialize();finish.invoke(candidate);assertEquals(settled,candidate.serialize());
  }
 }
 @Test public void midnightTrophyUsesLogicalStopDayNotDelayedPresentationDay()throws Exception{
  java.util.TimeZone old=java.util.TimeZone.getDefault();try{java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Jerusalem"));long before=java.time.ZonedDateTime.of(2026,10,28,23,59,59,950000000,java.time.ZoneId.of("Asia/Jerusalem")).toInstant().toEpochMilli();long after=before+180;assertEquals("2026-10-28",SlotsView.trophyDate(before));assertEquals("2026-10-29",SlotsView.trophyDate(after));
   View v=new View(true);v.layout(0,0,640,1040);v.clock=before;field("spinning").setBoolean(v,true);field("spinStartedAt").setLong(v,before-1200);long[] deadlines=(long[])field("stopAt").get(v);java.util.Arrays.fill(deadlines,before);v.draw(new Canvas(Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888)));assertEquals(before,field("logicalRewardAt").getLong(v));v.clock=after;v.draw(new Canvas(Bitmap.createBitmap(640,1040,Bitmap.Config.ARGB_8888)));assertEquals(before,field("logicalRewardAt").getLong(v));field("trophyDay").set(v,"2026-10-28");TraceRandom highRoll=new TraceRandom(1){@Override public float nextFloat(){return .99f;}};field("rng").set(v,highRoll);Method trophy=SlotsView.class.getDeclaredMethod("maybeTrophy");trophy.setAccessible(true);trophy.invoke(v);assertEquals(0,v.getTrophyMask());assertEquals("2026-10-28",field("trophyDay").get(v));
   field("logicalRewardAt").setLong(v,after);trophy.invoke(v);assertEquals(1,Integer.bitCount(v.getTrophyMask()));assertEquals("2026-10-29",field("trophyDay").get(v));
  }finally{java.util.TimeZone.setDefault(old);}
 }

}

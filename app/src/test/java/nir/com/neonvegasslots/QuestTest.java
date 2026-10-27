package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class QuestTest {
 @Test public void weekEndsOnNextMondayMidnightAndIsFixed(){
  long t=new GregorianCalendar(2026,Calendar.OCTOBER,24,23,0).getTimeInMillis(); // Saturday
  Calendar e=Calendar.getInstance(); e.setTimeInMillis(MissionsManager.weekEndMs(t));
  assertEquals(Calendar.MONDAY,e.get(Calendar.DAY_OF_WEEK)); assertEquals(0,e.get(Calendar.HOUR_OF_DAY)); assertEquals(26,e.get(Calendar.DAY_OF_MONTH));
  long t2=new GregorianCalendar(2026,Calendar.OCTOBER,25,23,59).getTimeInMillis(); // Sunday
  assertEquals("same week, same end",MissionsManager.weekEndMs(t),MissionsManager.weekEndMs(t2));
  assertEquals(MissionsManager.weekId(t),MissionsManager.weekId(t2));
  long t3=new GregorianCalendar(2026,Calendar.OCTOBER,26,0,1).getTimeInMillis();
  assertNotEquals("Monday starts a new week",MissionsManager.weekId(t),MissionsManager.weekId(t3));
 }
 @Test public void spinsAdvanceQuestAndClaimPaysOnce(){
  MissionsManager m=new MissionsManager(RuntimeEnvironment.getApplication(),null);
  for(int i=0;i<100;i++) m.onSpin(0,10,1,i);
  MissionsManager.Item q=m.weeklyQuest().get(1);
  assertTrue(q.done()); assertEquals(400,m.claim(q));
  assertEquals("no double claim",0,m.claim(m.weeklyQuest().get(1)));
  assertFalse(m.weeklyQuest().get(2).done());
 }
 @Test public void firstQuestStepIsTwentySpins(){
  MissionsManager m=new MissionsManager(RuntimeEnvironment.getApplication(),null);
  assertEquals(20,m.weeklyQuest().get(0).goal);
  for(int i=0;i<19;i++) m.onSpin(0,10,1,i);
  assertFalse(m.weeklyQuest().get(0).done());
  m.onSpin(0,10,1,19);
  assertTrue(m.weeklyQuest().get(0).done());
 }
}

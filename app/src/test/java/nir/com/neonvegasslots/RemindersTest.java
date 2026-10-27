package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.util.Calendar;
@RunWith(RobolectricTestRunner.class)
public class RemindersTest {
  @Test public void firesAt1030LocalAndNeverWithinSixHours() {
    Calendar c = Calendar.getInstance(); c.clear(); c.set(2026, Calendar.OCTOBER, 25, 22, 40); // closed late evening
    long now = c.getTimeInMillis(), at = Reminders.nextFireAt(now);
    Calendar f = Calendar.getInstance(); f.setTimeInMillis(at);
    assertEquals(10, f.get(Calendar.HOUR_OF_DAY)); assertEquals(30, f.get(Calendar.MINUTE)); assertEquals(26, f.get(Calendar.DAY_OF_MONTH));
    c.set(2026, Calendar.OCTOBER, 25, 5, 0); // closed 05:00 -> 6h later is 11:00, so next 10:30 is the following day
    f.setTimeInMillis(Reminders.nextFireAt(c.getTimeInMillis()));
    assertEquals(26, f.get(Calendar.DAY_OF_MONTH));
    c.set(2026, Calendar.OCTOBER, 25, 2, 0); // 02:00 +6h = 08:00 -> same day 10:30
    f.setTimeInMillis(Reminders.nextFireAt(c.getTimeInMillis()));
    assertEquals(25, f.get(Calendar.DAY_OF_MONTH));
    assertTrue(Reminders.nextFireAt(now) - now >= 6L * 3600000L);
  }
  @org.junit.Test public void disablingCancelsPendingAlarm() {
    android.content.Context c = org.robolectric.RuntimeEnvironment.getApplication();
    android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(android.content.Context.ALARM_SERVICE);
    Reminders.setOn(c, true);
    assertEquals(1, org.robolectric.Shadows.shadowOf(am).getScheduledAlarms().size());
    Reminders.setOn(c, false);
    assertEquals(0, org.robolectric.Shadows.shadowOf(am).getScheduledAlarms().size());
    assertFalse(Reminders.isOn(c));
  }
  @Test public void consentPopupOnlyForNeverExposedPlayers() {
    assertTrue(Reminders.shouldAsk(false, false, true, false));   // never exposed
    assertFalse(Reminders.shouldAsk(true, false, true, false));   // already asked once (any answer)
    assertFalse(Reminders.shouldAsk(false, true, true, false));   // already opted in
    assertFalse(Reminders.shouldAsk(false, false, false, false)); // system notifications off: treated as a prior refusal
    assertFalse(Reminders.shouldAsk(false, false, true, true));   // denied the system prompt once: never re-ask
  }
}

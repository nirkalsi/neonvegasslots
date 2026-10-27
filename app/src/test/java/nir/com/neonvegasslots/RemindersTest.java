package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.util.Calendar;
@org.robolectric.annotation.Config(sdk=34)
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
  @Test public void consentPopupOnlyForNewInstallsNeverExposed() {
    assertTrue(Reminders.shouldAsk(false, false, false, true, false));  // new install, never exposed
    assertFalse(Reminders.shouldAsk(true, false, false, true, false));  // legacy upgrade: ambiguous cohort, no auto popup
    assertFalse(Reminders.shouldAsk(false, true, false, true, false));  // already asked
    assertFalse(Reminders.shouldAsk(false, false, true, true, false));  // already opted in
    assertFalse(Reminders.shouldAsk(false, false, false, false, false)); // system notifications off
    assertFalse(Reminders.shouldAsk(false, false, false, true, true));  // denied system prompt once
    assertFalse(Reminders.shouldAsk(true, false, false, true, true));   // double denial on a legacy install: never re-asked
  }
  @Test public void legacyCohortFromInstallTimes() {
    assertFalse(Reminders.isLegacy(1000L, 1000L));
    assertTrue(Reminders.isLegacy(1000L, 1000L + 3600000L));
  }
  @org.junit.Test public void slotMessagesAreTrueOnly() {
    assertNull(Reminders.messageFor(0, false)); assertNull(Reminders.messageFor(2, false));
    assertNull(Reminders.messageFor(1, true)); assertNull(Reminders.messageFor(3, true));
    assertNotNull(Reminders.messageFor(0, true)); assertNotNull(Reminders.messageFor(2, true));
    int posted = 0; for (int k = 0; k < Reminders.SLOTS.length; k++) if (Reminders.messageFor(k, true) != null) posted++;
    assertTrue(posted <= 2);
    for (int sl : Reminders.SLOTS) assertTrue(sl >= 10 * 60 + 30 && sl <= 20 * 60 + 30);
  }
}

package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import org.junit.Test;
public class BonusReadyTest {
    @Test public void readyOnlyWhenNotClaimedToday() {
        assertTrue(Reminders.bonusReady("", "2026-10-28"));
        assertTrue(Reminders.bonusReady("2026-10-27", "2026-10-28"));
        assertFalse(Reminders.bonusReady("2026-10-28", "2026-10-28"));
    }
}

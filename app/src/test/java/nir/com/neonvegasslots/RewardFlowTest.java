package nir.com.neonvegasslots;

import org.junit.Test;
import static org.junit.Assert.*;

public class RewardFlowTest {

    @Test public void noFill_notifies_and_neverGrants() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.NOTIFY_NOT_READY, f.onNoFill());
        assertEquals(RewardFlow.Decision.NOTIFY_NOT_READY, f.onNoFill());
    }

    @Test public void showFailure_notifies_and_neverGrants() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.NOTIFY_SHOW_FAILED, f.onShowFailed());
    }

    @Test public void earlyDismiss_isIgnored_and_neverGrants() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.IGNORE, f.onDismissedWithoutEarn());
        // a dismissal must not consume the one-time grant
        assertEquals(RewardFlow.Decision.GRANT_REWARD, f.onEarned());
    }

    @Test public void earnedCallback_grantsExactlyOnce() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.GRANT_REWARD, f.onEarned());
    }

    @Test public void duplicateEarnedCallbacks_grantOnlyOnce() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.GRANT_REWARD, f.onEarned());
        assertEquals(RewardFlow.Decision.IGNORE, f.onEarned());
        assertEquals(RewardFlow.Decision.IGNORE, f.onEarned());
    }

    @Test public void failureAfterGrant_doesNotUngrant() {
        RewardFlow f = new RewardFlow();
        assertEquals(RewardFlow.Decision.GRANT_REWARD, f.onEarned());
        assertEquals(RewardFlow.Decision.NOTIFY_SHOW_FAILED, f.onShowFailed());
        assertEquals(RewardFlow.Decision.IGNORE, f.onEarned());
    }

    @Test public void failureStrings_areUserSafe() {
        for (String s : new String[]{RewardFlow.MSG_NOT_READY, RewardFlow.MSG_SHOW_FAILED}) {
            assertFalse(s.isEmpty());
            assertTrue("must tell the user what to do next", s.contains("try again"));
            assertFalse("no technical jargon", s.contains("AdMob"));
            assertFalse("no error codes", s.matches(".*\\b\\d{3}\\b.*"));
            assertFalse("no blame words", s.toLowerCase().contains("fail"));
        }
    }
}

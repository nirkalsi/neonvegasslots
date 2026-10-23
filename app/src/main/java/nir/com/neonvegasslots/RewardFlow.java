package nir.com.neonvegasslots;

/**
 * Rewarded-ad flow decisions, extracted as pure logic for JVM unit tests.
 * Rule: coins are granted ONLY on the ad network's earned-reward callback,
 * never on click, load failure, show failure, or early dismissal.
 */
public final class RewardFlow {
    public static final String MSG_NOT_READY = "Ad not ready yet - try again in a moment";
    public static final String MSG_SHOW_FAILED = "Ad couldn't play - try again in a moment";

    public enum Decision { GRANT_REWARD, NOTIFY_NOT_READY, NOTIFY_SHOW_FAILED, IGNORE }

    private boolean granted;

    /** No ad was loaded when the user tapped. Never grants. */
    public Decision onNoFill() { return Decision.NOTIFY_NOT_READY; }

    /** The ad failed to show. Never grants. */
    public Decision onShowFailed() { return Decision.NOTIFY_SHOW_FAILED; }

    /** The user dismissed before earning. Never grants. */
    public Decision onDismissedWithoutEarn() { return Decision.IGNORE; }

    /** Earned-reward callback. Grants once; duplicate callbacks are ignored. */
    public Decision onEarned() {
        if (granted) return Decision.IGNORE;
        granted = true;
        return Decision.GRANT_REWARD;
    }
}

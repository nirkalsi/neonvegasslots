package nir.com.neonvegasslots;

/** Pure control flow for consent then ads start. Guarantees: the SDK start runs at most once,
 *  only when the SDK reports canRequestAds, and the ready callback runs exactly once on every path. */
public final class ConsentFlow {
    public interface Gate {
        void requestInfoUpdate(Runnable onSuccess, Runnable onError);
        void showFormIfRequired(Runnable onDone);
        boolean canRequestAds();
    }
    private final Gate gate;
    private boolean started = false;
    private boolean readyRan = false;
    private Runnable start;
    private Runnable ready;

    public ConsentFlow(Gate gate) { this.gate = gate; }

    public void run(Runnable startSdk, Runnable readyCb) {
        start = startSdk; ready = readyCb;
        gate.requestInfoUpdate(
            () -> {
                if (gate.canRequestAds()) { startOnce(); finishReady(); }
                gate.showFormIfRequired(() -> {
                    if (gate.canRequestAds()) startOnce();
                    finishReady();
                });
            },
            () -> {
                if (gate.canRequestAds()) startOnce();
                finishReady();
            });
    }
    private void startOnce() { if (started) return; started = true; start.run(); }
    private void finishReady() { if (readyRan) return; readyRan = true; ready.run(); }
}

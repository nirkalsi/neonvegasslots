package nir.com.neonvegasslots;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConsentFlowTest {
    static class FakeGate implements ConsentFlow.Gate {
        boolean can; boolean updateOk = true; boolean canAfterForm;
        Runnable formDone; int formShown = 0; boolean deferForm = false;
        @Override public void requestInfoUpdate(Runnable ok, Runnable err) { if (updateOk) ok.run(); else err.run(); }
        @Override public void showFormIfRequired(Runnable done) {
            formShown++;
            if (deferForm) { formDone = done; return; }
            can = canAfterForm || can; done.run();
        }
        @Override public boolean canRequestAds() { return can; }
    }
    int starts, readies;
    private ConsentFlow flow(FakeGate g) { starts = 0; readies = 0; return new ConsentFlow(g); }

    @Test public void canRequest_startsOnce_readyOnce_evenThoughFormCallbackReenters() {
        FakeGate g = new FakeGate(); g.can = true;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(1, starts); assertEquals(1, readies);
    }
    @Test public void notAllowedAfterForm_noSdkStart_readyOnce() {
        FakeGate g = new FakeGate(); g.can = false;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(0, starts); assertEquals(1, readies);
    }
    @Test public void consentGrantedByForm_startsOnce_readyOnce() {
        FakeGate g = new FakeGate(); g.can = false; g.canAfterForm = true;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(1, starts); assertEquals(1, readies);
    }
    @Test public void formStillOpen_readyWaitsOnlyWhenNotAllowedYet() {
        FakeGate g = new FakeGate(); g.can = false; g.deferForm = true;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(0, readies);
        g.can = true; g.formDone.run(); g.formDone.run();
        assertEquals(1, starts); assertEquals(1, readies);
    }
    @Test public void updateError_withoutConsent_noStart_readyOnce_noForm() {
        FakeGate g = new FakeGate(); g.can = false; g.updateOk = false;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(0, starts); assertEquals(1, readies); assertEquals(0, g.formShown);
    }
    @Test public void updateError_butSdkSaysCanRequest_startsOnce() {
        FakeGate g = new FakeGate(); g.can = true; g.updateOk = false;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals(1, starts); assertEquals(1, readies);
    }
    @Test public void sdkNeverStartedWithoutCanRequestAds() {
        FakeGate g = new FakeGate(); g.can = false;
        flow(g).run(() -> starts++, () -> readies++);
        assertEquals("consent gate is never bypassed", 0, starts);
    }
}

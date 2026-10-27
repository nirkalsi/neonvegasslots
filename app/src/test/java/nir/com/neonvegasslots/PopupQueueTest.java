package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34)
public class PopupQueueTest {
  private GameDialog.Builder b(Activity a) { return new GameDialog.Builder(a, GameDialog.fromTheme(SlotArt.THEMES[0])).title("t").add(GameDialog.Btn.secondary("Close", null)); }
  @Test public void oneAtATimeAndAnActionBetweenAutoPopups() {
    Activity a = Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue(); GameDialog.busy = null;
    android.app.Dialog d1 = b(a).auto().show();
    assertTrue(GameDialog.isOpen());
    b(a).auto().show();                       // second popup must wait, not stack
    assertEquals(1, GameDialog.pending.size());
    d1.dismiss();
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertEquals("no gameplay action yet: auto popup still waits", 1, GameDialog.pending.size());
    assertFalse(GameDialog.isOpen());
    GameDialog.noteAction();
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertEquals(0, GameDialog.pending.size());
    assertTrue(GameDialog.isOpen());
  }
  @Test public void nothingOpensWhileBusy() {
    Activity a = Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue();
    final boolean[] busy = {true};
    GameDialog.busy = () -> busy[0];
    b(a).show();
    assertFalse(GameDialog.isOpen());
    busy[0] = false;
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertTrue(GameDialog.isOpen());
    GameDialog.busy = null; GameDialog.resetQueue();
  }
  @Test public void queuedCustomContentCanBeRebuiltAndShown() {
    Activity a=Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue(); final boolean[] busy={true};GameDialog.busy=()->busy[0];
    TrophyReveal v=new TrophyReveal(a,0);
    b(a).content(v).pinnedChrome().show();
    assertEquals(1,GameDialog.pending.size());busy[0]=false;
    ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
    assertTrue(GameDialog.isOpen());assertTrue(v.isAttachedToWindow());
    GameDialog.cur.dismiss();GameDialog.resetQueue();GameDialog.busy=null;
  }
  @Test public void destroyedOwnerCannotClearReplacementQueue() {
    Activity old=Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue();GameDialog.busy=()->true;b(old).show();old.finish();
    GameDialog.resetQueue();
    Activity next=Robolectric.buildActivity(Activity.class).setup().get();
    b(next).content(new TrophyReveal(next,0)).show();
    ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
    assertEquals("old callback must not clear new pending reveal",1,GameDialog.pending.size());
    GameDialog.busy=()->false;ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
    assertTrue(GameDialog.isOpen());GameDialog.cur.dismiss();GameDialog.resetQueue();GameDialog.busy=null;
  }
  @Test public void finishedOwnerDropsPendingRevealWithoutWindow() {
    Activity a=Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue();GameDialog.busy=()->true;b(a).content(new TrophyReveal(a,0)).show();a.finish();
    ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
    assertTrue(GameDialog.pending.isEmpty());assertFalse(GameDialog.isOpen());GameDialog.resetQueue();GameDialog.busy=null;
  }
}

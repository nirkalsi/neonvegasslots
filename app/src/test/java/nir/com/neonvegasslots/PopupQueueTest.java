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
public class PopupQueueTest {
  private GameDialog.Builder b(Activity a) { return new GameDialog.Builder(a, GameDialog.fromTheme(SlotArt.THEMES[0])).title("t").add(GameDialog.Btn.secondary("Close", null)); }
  @Test public void oneAtATimeAndAnActionBetweenAutoPopups() {
    Activity a = Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue(); GameDialog.busy = null;
    android.app.Dialog d1 = b(a).auto().show();
    assertTrue(GameDialog.showing);
    b(a).auto().show();                       // second popup must wait, not stack
    assertEquals(1, GameDialog.pending.size());
    d1.dismiss();
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertEquals("no gameplay action yet: auto popup still waits", 1, GameDialog.pending.size());
    assertFalse(GameDialog.showing);
    GameDialog.noteAction();
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertEquals(0, GameDialog.pending.size());
    assertTrue(GameDialog.showing);
  }
  @Test public void nothingOpensWhileBusy() {
    Activity a = Robolectric.buildActivity(Activity.class).setup().get();
    GameDialog.resetQueue();
    final boolean[] busy = {true};
    GameDialog.busy = () -> busy[0];
    b(a).show();
    assertFalse(GameDialog.showing);
    busy[0] = false;
    ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS);
    assertTrue(GameDialog.showing);
    GameDialog.busy = null; GameDialog.resetQueue();
  }
}

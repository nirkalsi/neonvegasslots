package nir.com.neonvegasslots;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SoundFxTest {
    @Test public void everyEffect_isAudible_unclipped_andFadesToSilence() {
        for (SoundFx.Effect e : SoundFx.Effect.values()) {
            short[] p = SoundFx.render(e);
            int peak = 0; double sum = 0;
            for (short s : p) { peak = Math.max(peak, Math.abs(s)); sum += s; }
            assertTrue(e + " too quiet", peak > 8000);
            assertTrue(e + " too loud / clipping", peak <= 0.65 * 32767);
            assertTrue(e + " has DC offset", Math.abs(sum / p.length) < 400);
            assertTrue(e + " must fade out (no click)", Math.abs(p[p.length - 1]) < 60);
            assertTrue(e + " too long for an action sound", p.length < SoundFx.RATE * 1.0);
            assertTrue(e + " too short", p.length > SoundFx.RATE * 0.12);
        }
    }
    @Test public void render_isDeterministic_andEffectsDiffer() {
        assertArrayEquals(SoundFx.render(SoundFx.Effect.SPIN), SoundFx.render(SoundFx.Effect.SPIN));
        assertNotEquals(SoundFx.render(SoundFx.Effect.SPIN).length, SoundFx.render(SoundFx.Effect.BIG_WIN).length);
    }
    @Test public void wav_hasValidHeader() {
        short[] p = SoundFx.render(SoundFx.Effect.WIN);
        byte[] w = SoundFx.wav(p);
        assertEquals("RIFF", new String(w, 0, 4));
        assertEquals("WAVE", new String(w, 8, 4));
        assertEquals(44 + p.length * 2, w.length);
        assertEquals(SoundFx.RATE, (w[24] & 255) | ((w[25] & 255) << 8) | ((w[26] & 255) << 16));
    }
    @Test public void muteSwitch_defaultsOn_persists_andMutedPlayDoesNothing() {
        Context c = RuntimeEnvironment.getApplication();
        assertTrue(SoundFx.isOn(c));
        SoundFx.setOn(c, false);
        assertFalse(SoundFx.isOn(c));
        new SoundFx(c).play(SoundFx.Effect.SPIN); // muted: must not throw or init audio
        SoundFx.setOn(c, true);
        assertTrue(SoundFx.isOn(c));
    }
}

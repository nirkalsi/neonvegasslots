package nir.com.neonvegasslots;

import static org.junit.Assert.*;
import org.junit.Test;

public class MusicTest {
    @Test public void loopIsAudibleBoundedAndClickFree() {
        short[] p = Music.render();
        assertEquals(Music.RATE * Music.SECONDS, p.length);
        int peak = 0; double e = 0;
        for (short s : p) { peak = Math.max(peak, Math.abs(s)); e += (double) s * s; }
        assertTrue("audible", Math.sqrt(e / p.length) > 800);
        assertTrue("no clipping", peak < 32000);
        // loop seam: last and first samples are near zero (no click)
        assertTrue(Math.abs(p[0]) < 400);
        assertTrue(Math.abs(p[p.length - 1]) < 400);
        System.out.println("MUSIC peak=" + peak + " rms=" + (int) Math.sqrt(e / p.length) + " first=" + p[0] + " last=" + p[p.length - 1]);
    }
}

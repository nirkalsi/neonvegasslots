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
        double dc=0;int jump=0;for(int i=0;i<p.length;i++){dc+=p[i];if(i>0)jump=Math.max(jump,Math.abs(p[i]-p[i-1]));}assertTrue("low DC offset",Math.abs(dc/p.length)<30);assertTrue("no sample spike",jump<6000);
        try{java.nio.file.Path path=java.nio.file.Paths.get("/tmp/renders/neonvegasslots-theme-music.wav");java.nio.ByteBuffer wav=java.nio.ByteBuffer.allocate(44+p.length*2).order(java.nio.ByteOrder.LITTLE_ENDIAN);wav.put("RIFF".getBytes()).putInt(36+p.length*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(Music.RATE).putInt(Music.RATE*2).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(p.length*2);for(short sample:p)wav.putShort(sample);java.nio.file.Files.write(path,wav.array());}catch(Exception error){throw new AssertionError(error);}
        System.out.println("MUSIC peak=" + peak + " rms=" + (int) Math.sqrt(e / p.length) + " first=" + p[0] + " last=" + p[p.length - 1]);
    }
}

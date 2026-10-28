package nir.com.neonvegasslots;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Original background music, synthesized in code (no third-party audio, so no licence to track).
 *  Original themed 16-second loop with quiet envelopes and a faded loop seam. Own on/off switch, on by default, paused whenever the
 *  game is not in the foreground (this also covers a rewarded ad taking over the screen). */
public final class Music {
    static final String PREF = "music_on";
    static final int RATE = 22050, SECONDS = 16;
    public static boolean isOn(Context c) { return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getBoolean(PREF, true); }
    public static void setOn(Context c, boolean on) { c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF, on).apply(); }

    /** Pure function for tests: one loop of 16-bit mono PCM. */
    static short[] render() {
        int n = RATE * SECONDS;
        double[] buf = new double[n];
        // Original soft electric pulse: neon city, not the Dragon pluck loop.
        double[] roots={110,87.31,130.81,98};int seg=n/4;
        for(int chord=0;chord<4;chord++)for(double ratio:new double[]{2,2.5,3})for(int i=0;i<seg;i++){
            double t=i/(double)RATE,env=Math.pow(Math.sin(Math.PI*i/seg),2),f=roots[chord]*ratio;
            buf[chord*seg+i]+=.045*env*Math.sin(2*Math.PI*f*t)*(1+.12*Math.sin(2*Math.PI*2*t));
        }
        int beat=RATE/2;double[] notes={440,523.25,659.25,587.33,523.25,392,440,329.63};
        for(int step=0;step<SECONDS*2;step++){int start=step*beat;double bass=roots[(step/8)%4],f=notes[(step+step/8)%notes.length];
            for(int i=0;i<beat&&start+i<n;i++){double t=i/(double)RATE,env=Math.min(1,t/.014)*Math.exp(-t*9);
                buf[start+i]+=.11*env*Math.sin(2*Math.PI*bass*t)+.085*env*(Math.sin(2*Math.PI*f*t)+.20*Math.sin(2*Math.PI*f*2*t));
            }
        }
        short[] out = new short[n];
        for (int i = 0; i < n; i++) out[i] = (short) Math.max(-32000, Math.min(32000, buf[i] * 32767 * 0.9));
        // the arpeggio tail of the last notes must not spill past the loop end: fade the final 20 ms
        int fade = RATE / 50;
        for (int i = 0; i < fade; i++) out[n - 1 - i] = (short) (out[n - 1 - i] * (i / (double) fade));
        return out;
    }

    private AudioTrack track;
    private final Context ctx;
    private short[] pcm;
    public Music(Context c) { ctx = c.getApplicationContext(); }

    /** Call from onResume. No-op when the switch is off. */
    public void start() {
        if (!isOn(ctx)) return;
        try {
            if (track == null) {
                if (pcm == null) pcm = render();
                track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
                track.write(pcm, 0, pcm.length);
                track.setLoopPoints(0, pcm.length, -1);
                track.setVolume(0.35f);
            }
            track.play();
        } catch (Throwable t) { release(); }
    }
    /** Call from onPause, and when the switch is turned off. */
    public void pause() { try { if (track != null) track.pause(); } catch (Throwable ignored) { } }
    public void release() { try { if (track != null) { track.release(); } } catch (Throwable ignored) { } track = null; }
}

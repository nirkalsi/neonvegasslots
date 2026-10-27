package nir.com.neonvegasslots;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Original background music, synthesized in code (no third-party audio, so no licence to track).
 *  A soft looping pad plus a slow pentatonic arpeggio, 16 s loop; every partial is a whole number of
 *  cycles per loop so the loop point is click-free. Own on/off switch, on by default, paused whenever the
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
        // chord pad, Am - F - C - G, 4 s each; frequencies snapped to whole cycles per loop
        double[][] chords = {{220.00, 261.63, 329.63}, {174.61, 220.00, 261.63}, {196.00, 261.63, 329.63}, {196.00, 246.94, 293.66}};
        int seg = n / 4;
        for (int c = 0; c < 4; c++) for (double f0 : chords[c]) {
            double f = Math.round(f0 * SECONDS) / (double) SECONDS;
            for (int i = 0; i < seg; i++) {
                double t = (double) i / seg, env = Math.sin(Math.PI * t); env *= env;
                int k = c * seg + i;
                buf[k] += 0.11 * env * Math.sin(2 * Math.PI * f * k / RATE);
            }
        }
        // arpeggio, 8th notes at 0.5 s, A minor pentatonic, plucked decay
        double[] scale = {440.00, 523.25, 587.33, 659.25, 783.99, 659.25, 587.33, 523.25};
        int step = RATE / 2;
        for (int s = 0; s < SECONDS * 2; s++) {
            double f = scale[(s * 3 + s / 8) % scale.length];
            for (int i = 0; i < step * 2 && s * step + i < n; i++) {
                double env = Math.exp(-i / (RATE * 0.35)) * Math.min(1.0, i / 200.0);
                buf[s * step + i] += 0.10 * env * Math.sin(2 * Math.PI * f * i / RATE);
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

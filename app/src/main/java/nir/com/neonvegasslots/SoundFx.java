package nir.com.neonvegasslots;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Map;

/** Short original action sounds, synthesised in code (no audio files, no licensing, no background music).
 *  Plays through SoundPool on the game stream, respects the system media volume and a saved on/off switch. */
public final class SoundFx {
    public enum Effect { SPIN, REEL_STOP, WIN, BIG_WIN, ANTICIPATION, NEON_BUZZ, SPIN_LOOP, CLICK, COLLECT, MEGA_WIN, EPIC_WIN }
    public static final int RATE = 22050;
    private static final String PREF = "sound_fx_on";

    /** Pure and deterministic: the samples for one effect (16-bit mono PCM at RATE). */
    public static short[] render(Effect e) {
        switch (e) {
            case SPIN_LOOP: return reelLoop();
            case CLICK: return seq(new double[]{880,660}, 20, .30);
            case COLLECT: return seq(new double[]{1046,1318,1568}, 55, .45);
            case MEGA_WIN: return seq(new double[]{659,784,1046,1318,1568}, 120, .58);
            case EPIC_WIN: return seq(new double[]{523,784,1046,1318,1568,2093}, 135, .60);
            case NEON_BUZZ: return neonBuzz();
            case ANTICIPATION: return seq(new double[]{392, 494, 587, 784}, 140, 0.42);
            case SPIN:      return seq(new double[]{330.0, 392.0, 494.0}, 55, 0.5);
            case REEL_STOP: return seq(new double[]{196.0}, 40, 0.55);
            case WIN:       return seq(new double[]{523.25, 659.25, 783.99}, 90, 0.55);
            default:        return seq(new double[]{523.25, 659.25, 783.99, 1046.5, 1318.5, 1568.0}, 110, 0.6);
        }
    }

    private static short[] neonBuzz(){short[] out=new short[RATE*420/1000];for(int i=0;i<out.length;i++){double t=i/(double)RATE,env=Math.min(1,i/(RATE*.009))*Math.max(0,(out.length-1-i)/(RATE*.09));env=Math.min(1,env);double buzz=Math.sin(2*Math.PI*110*t)+.32*Math.sin(2*Math.PI*330*t)+.14*Math.sin(2*Math.PI*880*t);out[i]=(short)(buzz/1.46*env*.48*32767);}return out;}
    private static short[] reelLoop(){
        short[] a=new short[RATE/2];
        for(int i=0;i<a.length;i++){double t=i/(double)RATE;
            // Integer cycles over half a second give a click-free repeating mechanical roll.
            double pulse=.55+.45*Math.cos(2*Math.PI*24*t);
            a[i]=(short)(12500*pulse*(.65*Math.sin(2*Math.PI*120*t)+.35*Math.sin(2*Math.PI*360*t)));}
        return a;
    }
    private static short[] seq(double[] freqs, int noteMs, double peak) {
        int per = RATE * noteMs / 1000, tail = RATE * 120 / 1000;
        short[] out = new short[per * freqs.length + tail];
        double[] acc = new double[out.length];
        for (int n = 0; n < freqs.length; n++) {
            int start = n * per, len = per + tail;
            for (int i = 0; i < len && start + i < acc.length; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1.0, i / (RATE * 0.004)) * Math.exp(-t * (n == freqs.length - 1 ? 9.0 : 16.0));
                double s = Math.sin(2 * Math.PI * freqs[n] * t) + 0.25 * Math.sin(4 * Math.PI * freqs[n] * t);
                acc[start + i] += s * env;
            }
        }
        double max = 1e-9;
        for (double v : acc) max = Math.max(max, Math.abs(v));
        int fade = RATE * 60 / 1000; // linear fade-out over the last 60 ms so the sound ends on silence (no click)
        for (int i = 0; i < out.length; i++) {
            double g = i >= out.length - fade ? (out.length - 1 - i) / (double) fade : 1.0;
            out[i] = (short) Math.round(acc[i] / max * peak * 32767 * g);
        }
        return out;
    }

    /** RIFF/WAVE bytes for SoundPool. */
    public static byte[] wav(short[] pcm) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int dataLen = pcm.length * 2;
        w(b, "RIFF"); le32(b, 36 + dataLen); w(b, "WAVE"); w(b, "fmt "); le32(b, 16);
        le16(b, 1); le16(b, 1); le32(b, RATE); le32(b, RATE * 2); le16(b, 2); le16(b, 16);
        w(b, "data"); le32(b, dataLen);
        for (short s : pcm) le16(b, s);
        return b.toByteArray();
    }
    private static void w(ByteArrayOutputStream b, String s) { for (char c : s.toCharArray()) b.write(c); }
    private static void le16(ByteArrayOutputStream b, int v) { b.write(v & 255); b.write((v >> 8) & 255); }
    private static void le32(ByteArrayOutputStream b, int v) { le16(b, v & 0xFFFF); le16(b, (v >>> 16) & 0xFFFF); }

    public static boolean isOn(Context c) { return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getBoolean(PREF, true); }
    public static void setOn(Context c, boolean on) { c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF, on).apply(); }

    private final Context ctx;
    private SoundPool pool;
    private final Map<Effect, Integer> ids = new EnumMap<>(Effect.class);
    private final Map<Integer, Boolean> loaded = new java.util.HashMap<>();

    private int played;
    private int reelStream; private boolean wantsReelLoop;
    public SoundFx(Context c) { ctx = c.getApplicationContext(); }
    /** Number of effects actually handed to the audio engine (for device tests). */
    public int playedCount() { return played; }
    /** True once SoundPool finished loading this effect. */
    public boolean isReady(Effect e) { Integer id = ids.get(e); return id != null && Boolean.TRUE.equals(loaded.get(id)); }
    /** Starts loading without playing anything. */
    public void warmUp() { if (isOn(ctx)) ensure(); }

    private void ensure() {
        if (pool != null) return;
        try {
            pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((p, id, status) -> {
                loaded.put(id, status == 0);
                if(status==0 && wantsReelLoop && ids.get(Effect.SPIN_LOOP)!=null && ids.get(Effect.SPIN_LOOP)==id)startReelLoop();
                android.util.Log.i("SoundFx", "loaded id=" + id + " status=" + status);
            });
            for (Effect e : Effect.values()) {
                File f = new File(ctx.getCacheDir(), "fx_" + e.name().toLowerCase() + ".wav");
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(wav(render(e))); }
                ids.put(e, pool.load(f.getAbsolutePath(), 1));
            }
        } catch (Exception ex) { android.util.Log.w("SoundFx", "init failed " + ex); pool = null; }
    }

    /** Plays one action effect unless the player switched sounds off. Never throws. */
    public void play(Effect e) {
        if (!isOn(ctx)) {stopReelLoop();return;}
        try {
            ensure();
            Integer id = ids.get(e);
            if (pool == null || id == null || !Boolean.TRUE.equals(loaded.get(id))) return;
            int s = pool.play(id, 0.8f, 0.8f, 1, 0, 1f);
            if (s != 0) played++;
            android.util.Log.i("SoundFx", "play " + e + " stream=" + s);
        } catch (Exception ex) { android.util.Log.w("SoundFx", "play failed " + ex); }
    }

    public void startReelLoop(){
        wantsReelLoop=true;if(!isOn(ctx)||reelStream!=0)return;ensure();
        Integer id=ids.get(Effect.SPIN_LOOP);if(pool!=null&&id!=null&&Boolean.TRUE.equals(loaded.get(id)))reelStream=pool.play(id,.34f,.34f,0,-1,1f);
    }
    boolean reelLoopRequested(){return wantsReelLoop;}
    public void stopReelLoop(){wantsReelLoop=false;if(pool!=null&&reelStream!=0)pool.stop(reelStream);reelStream=0;}
    public void release() {stopReelLoop(); if (pool != null) { pool.release(); pool = null; ids.clear(); loaded.clear(); } }
}

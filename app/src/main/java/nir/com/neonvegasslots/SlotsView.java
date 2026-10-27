package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Neon Vegas Slots - free social slot machine. Virtual coins only, no real money, no prizes. */
public class SlotsView extends View {
    public interface Listener {
        void onOutOfCoins();
        void onSpinComplete(int spinsDone, long win, long bet);
        void onBigWin(long win, long bet);
        void onLevelUp(int level, long reward);
        void saveState();
        default void onOpenShop() {}
        default void onOpenMap() {}
    }

    private static final int[] WEIGHT = {4, 7, 10, 16, 18, 12};
    private static final int[] PAY5 = {1200, 600, 360, 240, 180, 300};
    private static final int[] PAY4 = {300, 144, 96, 60, 48, 72};
    private static final int[] PAY3 = {96, 48, 36, 25, 25, 24};
    private static final int[][] LINES = {
        {1,1,1,1,1}, {0,0,0,0,0}, {2,2,2,2,2}, {0,1,2,1,0}, {2,1,0,1,2}
    };
    private static final int[] BETS = {5, 10, 25, 50, 100, 200, 500, 1000, 2500, 5000, 10000, 25000};
    /** Player level that unlocks each BET step (virtual coins only). The first six are always available. */
    static final int[] BET_UNLOCK = {1, 1, 1, 1, 1, 1, 10, 20, 35, 50, 70, 100};
    static int betCount(int level) { int n = 0; for (int u : BET_UNLOCK) if (level >= u) n++; return n; }
    static int betAmount(int i) { return BETS[i]; }
    /** XP per spin: 1 per coin up to 200, then a tenth of the extra, so big bets cannot farm levels. */
    static long xpForBet(long bet) { return bet <= 200 ? bet : 200 + (bet - 200) / 10; }
    private static String betLabel(long b) { return b >= 1000 ? (b % 1000 == 0 ? (b / 1000) + "K" : (b / 100) / 10f + "K") : String.valueOf(b); }

    int betIdx() { return betIdx; }
    void betStep(int dir) {
        int n = betCount(level);
        betIdx = ((betIdx + dir) % n + n) % n; persist(); invalidate();
    }

    private final SlotArt art = new SlotArt();
    private final Random rng = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final RectF qr = new RectF();
    private Listener listener;

    private int[][] grid = new int[3][5];      // [row][col] symbol index
    private long balance = 500;
    private long lastWin = 0;
    private int betIdx = 1;                    // 10
    private long spinsDone = 0;
    private long lastRegenAt = System.currentTimeMillis();
    private long xp = 0;
    private int level = 1;
    private long levelFlashUntil = 0;
    private int themeIdx = 0;
    private long unlockFlashUntil = 0;
    private String unlockedName = null;
    private int trophyMask = 0;            // 12 bits: 3 trophies per theme
    private String trophyDay = "";
    private long trophyFlashUntil = 0;
    private String trophyNewName = null;

    private boolean spinning = false;
    private long[] stopAt = new long[5];
    private long winStartAt = 0;
    private long winFlashUntil = 0;
    private long bigWinStartAt = 0;
    private long bigWinUntil = 0;
    private long bigWinAmount = 0;
    private int bigWinTier = 0; private long bigWinMs = 3000;
    /** Graded celebrations: BIG from 25x the bet, MEGA from 60x, EPIC from 150x. Longer, louder, more coins each step. */
    static final String[] TIER_NAME = {"BIG WIN!", "MEGA WIN!", "EPIC WIN!"};
    static final long[] TIER_MS = {3000, 3800, 4600};
    static final int[] TIER_COLOR = {0xFFFFD54A, 0xFFFF9F43, 0xFFFF5E7E};
    static int winTier(long win, long bet) { return win >= 150 * bet ? 2 : win >= 60 * bet ? 1 : 0; }
    private boolean anticipationGiven = false;
    private boolean[][] winCell = new boolean[3][5];
    private float titleGlow = 0f;

    public SlotsView(Context c) {
        super(c); art.loadBitmaps(c); randomizeAll(); }

    long nowMillis() { return System.currentTimeMillis(); } // overridable for tests

    public void setListener(Listener l) { listener = l; }
    public void addCoins(long n) { balance += n; persist(); invalidate(); }
    public long getBalance() { return balance; }
    public int getLevel() { return level; }
    boolean isSpinning() { return spinning; }
    /** True while a spin, win count-up, level-up or unlock animation is still resolving: popups wait. */
    boolean isBusyAnimating() {
        long n = System.currentTimeMillis();
        return spinning || n < winFlashUntil || n < bigWinUntil || n < levelFlashUntil || n < unlockFlashUntil || n < trophyFlashUntil;
    }
    long getLastWin() { return lastWin; }
    public long getSpinsDone() { return spinsDone; }
    public int getThemeIdx() { return themeIdx; }
    private long xpNeeded() { return level * 500L; }

    static int unlockedCount(int level) {
        int n = 0; for (SlotArt.Theme t : SlotArt.THEMES) if (level >= t.unlockLevel) n++;
        return n;
    }

    /** Tap-to-apply an unlocked machine theme. Returns false when still locked. */
    /** Full progression map for the map dialog (same art as before, now off the main screen). */
    void drawFullMap(Canvas c, float w, float h) { art.trail(c, w, 0, h, level, xp, xpNeeded(), themeIdx, null, trophyMask); }
    public boolean selectTheme(int i) {
        if (i < 0 || i >= SlotArt.THEMES.length) return false;
        if (level < SlotArt.THEMES[i].unlockLevel) return false;
        if (themeIdx != i) { themeIdx = i; persist(); invalidate(); }
        return true;
    }

    static final float TROPHY_CHANCE = 0.08f;
    static final int TROPHY_COUNT = 12;    // 4 themes x 3

    public int getTrophyMask() { return trophyMask; }
    public int trophiesOfTheme(int theme) { return Integer.bitCount((trophyMask >> (theme * 3)) & 7); }
    public boolean setComplete(int theme) { return trophiesOfTheme(theme) == 3; }

    /**
     * Cosmetic collectible roll. Zero economy effect: identical at every bet size,
     * grants no coins, changes no odds. First roll of each local day is guaranteed.
     */
    public boolean tryTrophyDrop(float roll, String today) {
        if (Integer.bitCount(trophyMask) >= TROPHY_COUNT) return false;
        boolean guaranteed = !today.equals(trophyDay);
        if (!guaranteed && roll >= TROPHY_CHANCE) return false;
        trophyDay = today;
        int free = TROPHY_COUNT - Integer.bitCount(trophyMask);
        int nth = Math.min(free - 1, (int) (roll * 9973 % free));
        for (int i = 0, seen = 0; i < TROPHY_COUNT; i++) {
            if ((trophyMask & (1 << i)) != 0) continue;
            if (seen++ == nth) {
                trophyMask |= (1 << i);
                trophyNewName = SlotArt.TROPHY_NAMES[i / 3][i % 3];
                trophyFlashUntil = System.currentTimeMillis() + 4000;
                break;
            }
        }
        persist();
        return true;
    }

    private void maybeTrophy() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        tryTrophyDrop(rng.nextFloat(), today);
    }

    public String serialize() { return balance + ";" + spinsDone + ";" + betIdx + ";" + lastRegenAt + ";" + xp + ";" + level + ";" + themeIdx + ";" + trophyMask + ";" + trophyDay; }
    public void restore(String s) {
        try {
            String[] a = s.split(";");
            balance = Long.parseLong(a[0]);
            spinsDone = Long.parseLong(a[1]);
            betIdx = Math.min(BETS.length - 1, Math.max(0, Integer.parseInt(a[2]))); // clamped to the unlocked ladder after the level is read
            if (a.length > 3) lastRegenAt = Long.parseLong(a[3]);
            if (a.length > 4) xp = Long.parseLong(a[4]);
            if (a.length > 5) level = Math.max(1, Integer.parseInt(a[5]));
            betIdx = Math.min(betIdx, betCount(level) - 1);
            if (a.length > 6) {
                int t = Integer.parseInt(a[6]);
                themeIdx = (t >= 0 && t < SlotArt.THEMES.length && level >= SlotArt.THEMES[t].unlockLevel) ? t : 0;
            }
            if (a.length > 7) trophyMask = Integer.parseInt(a[7]) & 0xFFF;
            if (a.length > 8) trophyDay = a[8];
        } catch (Exception ignored) {}
    }

    // HUD coin chip: dark pill, gold rim, coin disc, amount, green "+" that opens the shop (original art).
    private final RectF chipPlus = new RectF();
    private float chipCx = 0, chipCy = 0;
    /** Screen position of the coin chip centre (claim coins fly here). */
    float[] chipCenterOnScreen() {
        int[] l = new int[2]; getLocationOnScreen(l);
        float cx = chipCx > 0 ? chipCx : getWidth() / 2f, cy = chipCy > 0 ? chipCy : getHeight() * 0.12f;
        return new float[]{l[0] + cx, l[1] + cy};
    }
    private void drawCoinChip(Canvas cv, float w, float gridTop) {
        String amt = String.format(java.util.Locale.US, "%,d", balance);
        p.setTextAlign(Paint.Align.LEFT);
        p.setFakeBoldText(true);
        p.setTextSize(w * 0.052f);
        float tw = p.measureText(amt);
        float h = w * 0.092f, pad = h * 0.18f, disc = h - pad * 2;
        float cw = pad + disc + h * 0.28f + tw + h * 0.32f + disc + pad;
        float cx = w / 2f - cw / 2f, cy = gridTop * 0.66f - h * 0.68f;
        rf.set(cx, cy, cx + cw, cy + h);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF140A2A); cv.drawRoundRect(rf, h / 2, h / 2, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2f, w * 0.005f)); p.setColor(0xFFFFC83A); cv.drawRoundRect(rf, h / 2, h / 2, p);
        p.setStyle(Paint.Style.FILL);
        float dcx = cx + pad + disc / 2, dcy = cy + h / 2;
        chipCx = cx + cw / 2f; chipCy = dcy;
        if (balance < 100) { // refill timer: +5 coins per full minute below 100, also while the app is closed
            long left = Math.max(0, 60000 - (System.currentTimeMillis() - lastRegenAt)) / 1000;
            p.setTextSize(w * 0.036f); p.setFakeBoldText(true); p.setColor(0xFFBFE9C8);
            cv.drawText(String.format(java.util.Locale.US, "+5 in %d:%02d", left / 60, left % 60), cx + cw + w * 0.025f, dcy + p.getTextSize() * 0.35f, p);
            p.setTextSize(w * 0.052f);
        }
        p.setColor(0xFFFFC83A); cv.drawCircle(dcx, dcy, disc / 2, p);
        p.setColor(0xFFE09A12); cv.drawCircle(dcx, dcy, disc * 0.34f, p);
        p.setColor(0xFFFFE27A); cv.drawCircle(dcx - disc * 0.1f, dcy - disc * 0.1f, disc * 0.12f, p);
        p.setColor(0xFFFFFFFF);
        cv.drawText(amt, cx + pad + disc + h * 0.28f, dcy + p.getTextSize() * 0.35f, p);
        float px = cx + cw - pad - disc / 2;
        p.setColor(0xFF22C55E); cv.drawCircle(px, dcy, disc / 2, p);
        p.setColor(0xFFFFFFFF); p.setStrokeWidth(Math.max(3f, disc * 0.13f)); p.setStyle(Paint.Style.STROKE);
        cv.drawLine(px - disc * 0.22f, dcy, px + disc * 0.22f, dcy, p);
        cv.drawLine(px, dcy - disc * 0.22f, px, dcy + disc * 0.22f, p);
        p.setStyle(Paint.Style.FILL); p.setFakeBoldText(false); p.setTextAlign(Paint.Align.CENTER);
        float pad2 = h * 0.2f; // generous touch target
        chipPlus.set(cx + cw - h * 1.3f, cy - pad2, cx + cw + pad2, cy + h + pad2);
    }


    private static int lighten(int c, float f) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return 0xFF000000 | ((int) (r + (255 - r) * f) << 16) | ((int) (g + (255 - g) * f) << 8) | (int) (b + (255 - b) * f);
    }
    private static int darken(int c, float f) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return 0xFF000000 | ((int) (r * (1 - f)) << 16) | ((int) (g * (1 - f)) << 8) | (int) (b * (1 - f));
    }
    /** Glossy rounded button: soft drop shadow, vertical gradient, top gloss, rim, optional glow ring. */
    private void modernButton(Canvas cv, RectF r, int top, int bottom, int rim, float glow) {
        float rad = r.height() * 0.32f;
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF sh = new RectF(r.left + 2, r.top + 7, r.right - 2, r.bottom + 7);
        q.setColor(0x66000000); cv.drawRoundRect(sh, rad, rad, q);
        if (glow > 0f) {
            q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(8 + 6 * glow);
            q.setColor((rim & 0x00FFFFFF) | ((int) (60 + 90 * glow) << 24));
            cv.drawRoundRect(r, rad, rad, q);
            q.setStyle(Paint.Style.FILL);
        }
        q.setColor(0xFF000000); // shader must not inherit the 0x66 shadow alpha
        q.setShader(new LinearGradient(0, r.top, 0, r.bottom, top, bottom, Shader.TileMode.CLAMP));
        cv.drawRoundRect(r, rad, rad, q);
        q.setShader(null);
        RectF g = new RectF(r.left + r.height() * 0.1f, r.top + 3, r.right - r.height() * 0.1f, r.top + r.height() * 0.52f);
        q.setShader(new LinearGradient(0, g.top, 0, g.bottom, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawRoundRect(g, rad * 0.8f, rad * 0.8f, q);
        q.setShader(null);
        q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(2.5f); q.setColor(rim);
        cv.drawRoundRect(r, rad, rad, q);
        q.setStrokeWidth(1.5f); q.setColor(0x55FFFFFF);
        RectF in = new RectF(r.left + 4, r.top + 4, r.right - 4, r.bottom - 4);
        cv.drawRoundRect(in, rad * 0.85f, rad * 0.85f, q);
    }
    private void coinIcon(Canvas cv, float cx, float cy, float rr, SlotArt.Theme th) {
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setShader(new LinearGradient(cx, cy - rr, cx, cy + rr, 0xFFFFE680, 0xFFE0A100, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, rr, q); q.setShader(null);
        q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(rr * 0.18f); q.setColor(0xFF9C6A00);
        cv.drawCircle(cx, cy, rr * 0.62f, q);
        cv.drawCircle(cx, cy, rr, q);
    }
    /** Circular arrows (repeat) for AUTO. */
    private void loopIcon(Canvas cv, float cx, float cy, float rr, int color) {
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(rr * 0.3f); q.setColor(color); q.setStrokeCap(Paint.Cap.ROUND);
        RectF o = new RectF(cx - rr, cy - rr, cx + rr, cy + rr);
        cv.drawArc(o, 200, 250, false, q);
        q.setStyle(Paint.Style.FILL);
        Path t = new Path();
        float ax = cx + rr * (float) Math.cos(Math.toRadians(90)), ay = cy + rr * (float) Math.sin(Math.toRadians(90));
        t.moveTo(ax - rr * 0.75f, ay - rr * 0.1f); t.lineTo(ax - rr * 0.1f, ay - rr * 0.8f); t.lineTo(ax - rr * 0.1f, ay + rr * 0.7f); t.close();
        cv.drawPath(t, q);
    }
    /** Play triangle inside a ring for SPIN. */
    private void spinIcon(Canvas cv, float cx, float cy, float rr) {
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(rr * 0.22f); q.setColor(0xE6FFFFFF);
        cv.drawCircle(cx, cy, rr, q);
        q.setStyle(Paint.Style.FILL);
        Path t = new Path();
        t.moveTo(cx - rr * 0.28f, cy - rr * 0.5f); t.lineTo(cx + rr * 0.55f, cy); t.lineTo(cx - rr * 0.28f, cy + rr * 0.5f); t.close();
        cv.drawPath(t, q);
    }

    private void persist() { if (listener != null) listener.saveState(); }

    private int pickSymbol() { return pickSymbol(rng); }
    /** Weighted symbol draw; the single source of truth for reels, also used by the payout tests. */
    static int pickSymbol(Random rng) {
        int total = 0; for (int w : WEIGHT) total += w;
        int r = rng.nextInt(total);
        for (int i = 0; i < WEIGHT.length; i++) { r -= WEIGHT[i]; if (r < 0) return i; }
        return 0;
    }
    /** Pays all 5 lines, left to right, 3+ in a row. Marks winning cells when mark != null. Same code the game runs after every spin. */
    static long evaluate(int[][] g, long bet, boolean[][] mark) {
        long win = 0;
        for (int[] line : LINES) {
            int s = g[line[0]][0];
            int run = 1;
            for (int c = 1; c < 5; c++) { if (g[line[c]][c] == s) run++; else break; }
            long pay = 0;
            if (run >= 5) pay = PAY5[s]; else if (run == 4) pay = PAY4[s]; else if (run == 3) pay = PAY3[s];
            if (pay > 0) {
                win += pay * bet / 10;
                if (mark != null) for (int c = 0; c < run; c++) mark[line[c]][c] = true;
            }
        }
        return win;
    }

    private void randomizeAll() {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 5; c++) grid[r][c] = pickSymbol();
    }

    private void regen() { regenAt(System.currentTimeMillis()); }
    /** +5 coins per full minute while under 100 (cap 100); time while the app was closed counts, because lastRegenAt is saved. */
    void regenAt(long now) {
        if (balance >= 100) { lastRegenAt = now; return; } // the refill clock only runs while below the cap
        if (now - lastRegenAt >= 60000) {
            long steps = (now - lastRegenAt) / 60000;
            balance = Math.min(100, balance + 5 * steps);
            lastRegenAt = balance >= 100 ? now : lastRegenAt + steps * 60000; // keep the partial minute
            persist();
        }
    }
    /** test seam: put the refill state in place without playing. */
    void setRefillStateForTest(long bal, long lastAt) { balance = bal; lastRegenAt = lastAt; }
    long balanceForTest() { return balance; }
    long lastRegenAtForTest() { return lastRegenAt; }

    private boolean autoSpin = false;          // session-only, never persisted
    private int autoCount = 0;
    private final Runnable autoRunner = new Runnable() { @Override public void run() { autoStep(); } };
    private SoundFx sfx;
    private final boolean[] stopSounded = new boolean[5];
    public void setSoundFx(SoundFx s) { sfx = s; }
    private void fx(SoundFx.Effect e) { if (sfx != null) sfx.play(e); }

    /** Turn auto-spin off (player tap, low coins, big win, level-up, dialog or leaving the screen). */
    public void stopAuto() {
        removeCallbacks(autoRunner);
        if (autoSpin) { autoSpin = false; invalidate(); }
    }
    public boolean isAuto() { return autoSpin; }
    private void startAuto() {
        if (balance < BETS[betIdx]) { if (listener != null) listener.onOutOfCoins(); return; }
        autoSpin = true; autoCount = 0;
        if (!spinning) autoStep(); else invalidate();
    }
    private void autoStep() {
        if (!autoSpin || spinning) return;
        if (balance < BETS[betIdx]) { stopAuto(); return; }
        autoCount++;
        spin();
    }
    @Override protected void onDetachedFromWindow() { lpHandler.removeCallbacks(longPressAuto); stopAuto(); super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int v) { if (v != VISIBLE) stopAuto(); super.onWindowVisibilityChanged(v); }

    /** Tap SPIN while the reels run: the remaining reels stop left to right, 110 ms apart. */
    private void quickStop() {
        long now = System.currentTimeMillis(); int k = 0;
        for (int c = 0; c < 5; c++) if (stopAt[c] > now + 110L * (k + 1)) { stopAt[c] = now + 110L * (k + 1); k++; } else if (stopAt[c] > now) k++;
        invalidate();
    }
    private void spin() {
        if (spinning) { quickStop(); return; }
        regen();
        long bet = BETS[betIdx];
        if (balance < bet) { if (listener != null) listener.onOutOfCoins(); return; }
        balance -= bet;
        xp += xpForBet(bet);
        lastWin = 0;
        spinning = true;
        GameDialog.noteAction();
        long now = System.currentTimeMillis();
        for (int c = 0; c < 5; c++) { stopAt[c] = now + 600 + c * 380; stopSounded[c] = false; }
        fx(SoundFx.Effect.SPIN);
        anticipationGiven = false;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 5; c++) winCell[r][c] = false;
        persist();
        invalidate();
    }

    private void finishSpin() {
        spinning = false;
        spinsDone++;
        long bet = BETS[betIdx];
        long win = evaluate(grid, bet, winCell);
        if (win > 0) {
            balance += win; lastWin = win;
            fx(win >= 25 * bet ? SoundFx.Effect.BIG_WIN : SoundFx.Effect.WIN);
            winStartAt = System.currentTimeMillis();
            if (win >= 25 * bet) { bigWinTier = winTier(win, bet); bigWinMs = TIER_MS[bigWinTier]; bigWinStartAt = winStartAt; bigWinUntil = winStartAt + bigWinMs; bigWinAmount = win; winFlashUntil = winStartAt + bigWinMs; }
            else winFlashUntil = winStartAt + 1600;
        }
        int oldUnlocks = unlockedCount(level);
        while (xp >= xpNeeded()) {
            xp -= xpNeeded();
            level++;
            long reward = 150L * level;
            balance += reward;
            levelFlashUntil = System.currentTimeMillis() + 2000;
            if (listener != null) listener.onLevelUp(level, reward);
        }
        int newUnlocks = unlockedCount(level);
        if (newUnlocks > oldUnlocks) {
            unlockedName = SlotArt.THEMES[newUnlocks - 1].name;
            unlockFlashUntil = System.currentTimeMillis() + 4000;
        }
        maybeTrophy();
        persist();
        boolean leveled = newUnlocks > oldUnlocks || levelFlashUntil > System.currentTimeMillis();
        boolean trophy = trophyFlashUntil > System.currentTimeMillis();
        if (autoSpin) {
            if (win >= 25 * bet || leveled || trophy || balance < BETS[betIdx]) autoSpin = false;
            else postDelayed(autoRunner, win > 0 ? 1900 : 700);
        }
        if (listener != null) {
            if (win >= 25 * bet) listener.onBigWin(win, bet);
            listener.onSpinComplete((int) spinsDone, win, bet);
        }
        invalidate();
    }

    /** Reel cell height: a little taller than wide when the screen has room (doc: reels deserve more space), never smaller than square. */
    static float cellH(float w, float h) {
        float cw = w / 5f - 8;
        float avail = h - h * 0.195f - (h * 0.07f + w * 0.10f + w * 0.19f + h * 0.05f);
        float k = Math.max(1f, Math.min(1.22f, (avail - 16) / (3 * cw)));
        return cw * k;
    }
    static float ctrlY(float w, float h) { return h * 0.195f + cellH(w, h) * 3 + 16 + 24; }
    private float cellH() { return cellH(getWidth(), getHeight()); }
    private float gridTop() { return getHeight() * 0.195f; }
    private float trailTop() { return ctrlY(getWidth(), getHeight()) + getHeight() * 0.07f + getWidth() * 0.055f; }
    private float trailBottom() { return trailTop() + getWidth() * 0.19f; } // compact strip; the full map opens in a dialog
    private float trailNodeY() {
        float top = trailTop(), height = Math.min(trailBottom() - top, getWidth() * .68f);
        return top + height * .38f;
    }

    @Override protected void onDraw(Canvas cv) {
        long now = nowMillis();
        regen();
        SlotArt.Theme th = SlotArt.THEMES[themeIdx];
        int phase = (int) (now / 350);
        if (!art.sceneBackdrop(cv, getWidth(), getHeight(), th.skyTop)) art.backdrop(cv, getWidth(), getHeight(), th, phase);

        float w = getWidth();
        // HUD bar: fully opaque backing for title and balance; scene art is clipped below
        float bandH = getHeight() * 0.15f;
        int hud = 0xFF000000 | (((th.skyTop >> 16 & 0xFF) / 2) << 16) | (((th.skyTop >> 8 & 0xFF) / 2) << 8) | ((th.skyTop & 0xFF) / 2);
        p.setColor(hud);
        cv.drawRect(0, 0, w, bandH, p);
        p.setShader(new LinearGradient(0, bandH, 0, getHeight() * .185f, hud & 0x00FFFFFF | 0x99000000, 0x00000000, Shader.TileMode.CLAMP));
        cv.drawRect(0, bandH, w, getHeight() * .185f, p);
        p.setShader(null);
        p.setColor(th.neon & 0x00FFFFFF | 0x55000000);
        cv.drawRect(0, bandH - 1, w, bandH + 1, p);
        float cellW = w / 5f - 8;
        float gridTop = gridTop();
        float cellH = cellH(w, getHeight());
        float gridH = cellH * 3 + 16;

        // title
        titleGlow = (float) ((now % 4000L) / 4000.0 * 6.2832); // elapsed-time pulse: same speed at any redraw cadence
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(w * 0.06f);
        p.setFakeBoldText(true);
        p.setColor(th.title);
        p.setShadowLayer(14 + 6 * (float) Math.sin(titleGlow), 0, 0, th.titleGlow);
        cv.drawText("NEON VEGAS SLOTS", w / 2, gridTop * 0.42f, p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);

        // balance / win
        p.setTextSize(w * 0.05f);
        p.setColor(0xFFFFFFFF);
        drawCoinChip(cv, w, gridTop);
        // level + xp bar
        p.setTextSize(w * 0.034f);
        p.setColor(th.soft);
        cv.drawText("LV " + level, w * 0.14f, gridTop * 0.90f, p);
        float barL = w * 0.24f, barR = w * 0.76f, barY = gridTop * 0.835f, barH = w * 0.034f;
        rf.set(barL, barY, barR, barY + barH);
        p.setColor(0xCC000000); cv.drawRoundRect(rf, 8, 8, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w * 0.004f); p.setColor(th.title); cv.drawRoundRect(rf, 8, 8, p); p.setStyle(Paint.Style.FILL);
        float frac = Math.min(1f, xp / (float) xpNeeded());
        rf.set(barL, barY, barL + (barR - barL) * frac, barY + barH);
        p.setColor(th.title); cv.drawRoundRect(rf, 8, 8, p);
        if (lastWin > 0 && now < winFlashUntil && now >= bigWinUntil) {
            p.setColor(0xFF7CFC00);
            p.setTextSize(w * 0.06f);
            p.setFakeBoldText(true);
            long shown = lastWin * Math.min(100, (now - winStartAt) / 9) / 100;
            cv.drawText("WIN +" + shown, w / 2, gridTop * 0.95f, p);
            p.setFakeBoldText(false);
        }

        // machine frame: neon marquee with chase bulbs
        art.frame(cv, 4, gridTop - 8, w - 4, gridTop + gridH + 8, th, phase);

        // cells
        boolean flash = now < winFlashUntil && (now / 300) % 2 == 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 5; c++) {
                if (spinning && now < stopAt[c]) {
                    if (now / 60 % 2 == 0) grid[r][c] = pickSymbol();
                }
                float x = 4 + c * (cellW + 8);
                float y = gridTop + r * (cellH + 8);
                // settle bounce: a just-stopped column drops in with a small overshoot
                float settle = (now - stopAt[c]) / 260f;
                if (settle >= 0 && settle < 1) y -= cellW * 0.12f * (1 - settle) * (float) Math.cos(settle * 6.9f);
                rf.set(x + 2, y, x + cellW + 2, y + cellH);
                boolean hot = winCell[r][c] && flash;
                p.setColor(hot ? th.cellHot : th.cellBg);
                cv.drawRoundRect(rf, 12, 12, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                p.setColor(hot ? th.cellHotStroke : th.cellStroke);
                cv.drawRoundRect(rf, 12, 12, p);
                p.setStyle(Paint.Style.FILL);
                if (spinning && now < stopAt[c]) {
                    // scrolling strip: symbol + dimmed neighbours sliding upward, clipped to the cell
                    float pitch = cellH * 0.82f;
                    float scroll = (now * 1.15f + c * 173 + r * 61) % pitch;
                    cv.save();
                    cv.clipRect(x + 2, y, x + cellW + 2, y + cellH);
                    int sym = grid[r][c];
                    float cx = x + 2 + cellW / 2, cy = y + cellH / 2;
                    art.symbol(cv, sym, cx, cy - pitch + scroll, cellW * 0.74f);
                    p.setColor(0x30FFFFFF); cv.drawRect(x + 2, cy - pitch * 1.4f + scroll, x + cellW + 2, cy - pitch * 0.55f + scroll, p);
                    art.symbol(cv, sym, cx, cy + scroll, cellW * 0.82f);
                    art.symbol(cv, sym, cx, cy + pitch + scroll, cellW * 0.74f);
                    p.setColor(0x30FFFFFF); cv.drawRect(x + 2, cy + pitch * 0.55f + scroll, x + cellW + 2, cy + pitch * 1.4f + scroll, p);
                    cv.restore();
                } else {
                    float pls = (winCell[r][c] && now < winFlashUntil) ? 1f + 0.09f * (float) Math.sin(now / 110.0) : 1f; // winning symbols pulse
                    art.symbol(cv, grid[r][c], x + 2 + cellW / 2, y + cellH / 2, cellW * 0.84f * pls);
                }
            }
        }
        if (!spinning && now < winFlashUntil) { // win line: a glowing path through the winning cells, left to right
            float cw0 = w / 5f - 8, ch0 = cellH(w, getHeight()); float[] lx = new float[5], ly = new float[5]; int n = 0;
            for (int c = 0; c < 5; c++) for (int r = 0; r < 3; r++) if (winCell[r][c]) { lx[n] = 4 + c * (cw0 + 8) + 2 + cw0 / 2; ly[n] = gridTop + r * (ch0 + 8) + ch0 / 2; n++; break; }
            if (n >= 2) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
                p.setShadowLayer(14, 0, 0, th.title);
                p.setStrokeWidth(w * 0.016f); p.setColor(0xFFFFFFFF);
                for (int i = 1; i < n; i++) cv.drawLine(lx[i - 1], ly[i - 1], lx[i], ly[i], p);
                p.clearShadowLayer(); p.setStrokeWidth(w * 0.008f); p.setColor(th.title);
                for (int i = 1; i < n; i++) cv.drawLine(lx[i - 1], ly[i - 1], lx[i], ly[i], p);
                p.setStyle(Paint.Style.FILL); p.setStrokeCap(Paint.Cap.BUTT);
            }
        }
        if (spinning) {
            for (int c = 0; c < 5; c++) if (now >= stopAt[c] && !stopSounded[c]) { stopSounded[c] = true; fx(SoundFx.Effect.REEL_STOP); }
            boolean allStopped = true;
            for (int c = 0; c < 5; c++) if (now < stopAt[c]) allStopped = false;
            boolean firstFourStopped = true;
            for (int c = 0; c < 4; c++) if (now < stopAt[c]) firstFourStopped = false;
            if (firstFourStopped && now < stopAt[4]) {
                int bestRun = 1;
                for (int[] line : LINES) {
                    int s = grid[line[0]][0], run = 1;
                    for (int c = 1; c < 4; c++) { if (grid[line[c]][c] == s) run++; else break; }
                    if (run > bestRun) bestRun = run;
                }
                if (bestRun >= 3) {
                    if (!anticipationGiven) { stopAt[4] += 1100; anticipationGiven = true; }
                    // pulsing glow ring around the spinning last column
                    float cellWn = w / 5f - 8;
                    float gx = 4 + 4 * (cellWn + 8);
                    float pulse = 0.5f + 0.5f * (float) Math.sin(now / 90.0);
                    // column tint so the suspense is readable even mid-pulse
                    p.setColor(th.title & 0x00FFFFFF | ((int) (24 + 30 * pulse) << 24));
                    rf.set(gx, gridTop - 2, gx + cellWn + 4, gridTop + cellH * 3 + 22);
                    cv.drawRoundRect(rf, 16, 16, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(5 + 4 * pulse);
                    p.setColor(th.title & 0x00FFFFFF | ((int) (170 + 85 * pulse) << 24));
                    cv.drawRoundRect(rf, 16, 16, p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
            if (allStopped) { post(this::finishSpin); }
            else postInvalidateDelayed(30);
        } else if (now < winFlashUntil || now < bigWinUntil || now < unlockFlashUntil || now < levelFlashUntil || now < trophyFlashUntil) {
            postInvalidateDelayed(60);
        } else {
            postInvalidateDelayed(450); // idle ambient: chase bulbs and embers keep breathing
        }
        if (now < winFlashUntil) coinShower(cv, w, gridTop, gridH, th, now);
        if (now < bigWinUntil) bigWinOverlay(cv, w, th, now);
        if (now < levelFlashUntil) levelBurst(cv, w, gridTop, th, now);

        // bet selector + SPIN (ported from DG)
        float ctrlY = gridTop + gridH + 24;
        float btnH = getHeight() * 0.07f;
        boolean canSpin = !spinning && balance >= BETS[betIdx];
        float pulse = canSpin && !autoSpin ? (float) (0.5 + 0.5 * Math.sin(now / 520.0)) : 0f;
        // BET: minus / amount / plus (icons and numbers only)
        rf.set(w * 0.03f, ctrlY, w * 0.40f, ctrlY + btnH);
        modernButton(cv, rf, lighten(th.betBg, 0.28f), darken(th.betBg, 0.25f), th.title, 0f);
        float bcy = rf.centerY(), bu = btnH * 0.2f;
        coinIcon(cv, rf.centerX() - btnH * 0.40f, bcy, bu, th);
        p.setFakeBoldText(true); p.setTextAlign(Paint.Align.CENTER); p.setColor(0xFFFFFFFF);
        String bl = betLabel(BETS[betIdx]);
        p.setTextSize(w * (bl.length() > 3 ? 0.044f : 0.054f));
        cv.drawText(bl, rf.centerX() + btnH * 0.22f, bcy + btnH * 0.17f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(btnH * 0.09f); p.setColor(0xFFFFFFFF);
        float mx = rf.left + btnH * 0.34f, px = rf.right - btnH * 0.34f;
        cv.drawLine(mx - bu * 0.7f, bcy, mx + bu * 0.7f, bcy, p);                    // minus
        cv.drawLine(px - bu * 0.7f, bcy, px + bu * 0.7f, bcy, p);                    // plus
        cv.drawLine(px, bcy - bu * 0.7f, px, bcy + bu * 0.7f, p);
        p.setStrokeCap(Paint.Cap.BUTT); p.setStyle(Paint.Style.FILL);
        if (betIdx + 1 < BETS.length && betIdx + 1 >= betCount(level)) { // next step is locked: gold pill on the BET button edge, padlock + "LV n"
            float ph = w * 0.052f, pw = w * 0.17f, pcx = rf.right - pw * 0.55f, pcy = rf.top;
            qr.set(pcx - pw / 2, pcy - ph / 2, pcx + pw / 2, pcy + ph / 2);
            p.setColor(0xFFFFD166); cv.drawRoundRect(qr, ph / 2, ph / 2, p);
            float lx = qr.left + ph * 0.62f, ly = pcy + ph * 0.02f, ls = ph * 0.2f;
            p.setColor(0xFF3D2A00); cv.drawRoundRect(lx - ls * 1.1f, ly - ls * 0.2f, lx + ls * 1.1f, ly + ls * 1.5f, 2, 2, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(ls * 0.45f); cv.drawArc(lx - ls * 0.7f, ly - ls * 1.5f, lx + ls * 0.7f, ly + ls * 0.3f, 180, 180, false, p); p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.LEFT); p.setFakeBoldText(true); p.setTextSize(ph * 0.58f);
            cv.drawText("LV " + BET_UNLOCK[betIdx + 1], lx + ls * 1.9f, pcy + ph * 0.2f, p);
        }
        // SPIN: the hero button
        rf.set(w * 0.43f, ctrlY, w * 0.97f, ctrlY + btnH);
        int sTop = autoSpin ? 0xFFFF6B63 : spinning ? 0xFFFFB24A : (canSpin ? 0xFF86F26A : lighten(th.spinOff, 0.12f));
        int sBot = autoSpin ? 0xFFB71C1C : spinning ? 0xFFD9660F : (canSpin ? 0xFF13A53C : darken(th.spinOff, 0.2f));
        modernButton(cv, rf, sTop, sBot, canSpin ? 0xFFFFE9A8 : 0x66FFFFFF, pulse);
        p.setColor(0xFFFFFFFF);
        p.setShadowLayer(5, 0, 3, 0x99000000);
        p.setFakeBoldText(true);
        float scx = rf.centerX(), scy = rf.centerY(), sr = btnH * 0.3f;
        if (autoSpin) { // stop square + run count
            cv.drawRoundRect(scx - sr * 0.8f - btnH * 0.3f, scy - sr * 0.8f, scx + sr * 0.8f - btnH * 0.3f, scy + sr * 0.8f, 6, 6, p);
            p.setTextSize(w * 0.06f); p.setTextAlign(Paint.Align.LEFT);
            cv.drawText(String.valueOf(autoCount), scx + btnH * 0.05f, scy + btnH * 0.2f, p);
        } else if (spinning) { // STOP state: stays coloured and tappable (quick-stop), never a grey loading spinner
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(btnH * 0.46f);
            cv.drawText("STOP", scx, scy + btnH * 0.16f, p);
        } else {
            spinIcon(cv, scx - btnH * 0.28f, scy, btnH * 0.30f);
            loopIcon(cv, scx + btnH * 0.62f, scy, btnH * 0.16f, 0xCCFFFFFF); // hold = auto
        }
        p.clearShadowLayer();
        { // labels under the control row: BET caption and WIN readout
            float ly = ctrlY + btnH + w * 0.042f;
            p.setFakeBoldText(true); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w * 0.032f);
            p.setColor(th.soft); cv.drawText("BET", w * 0.215f, ly, p);
            if (lastWin > 0) { p.setColor(0xFFFFD166); cv.drawText("WIN " + lastWin, w * 0.70f, ly, p); }
            else { p.setColor(th.soft); cv.drawText(autoSpin ? "AUTO - tap to stop" : spinning ? "TAP TO STOP" : "TAP = SPIN   HOLD = AUTO", w * 0.70f, ly, p); }
            p.setFakeBoldText(false);
        }
        p.setFakeBoldText(false); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w * 0.045f);

        String ju;
        if (now < trophyFlashUntil) ju = "TROPHY:" + trophyNewName;
        else ju = now < unlockFlashUntil ? unlockedName : null;
        art.trailCompact(cv, w, trailTop(), trailBottom(), level, xp, xpNeeded(), themeIdx, ju);
        if (setComplete(themeIdx)) art.frameOrnament(cv, w / 2, gridTop - w * .022f, w * .05f, th);

        // legal line lives in Settings (Missions dialog), not on the game screen
    }

    private void coinShower(Canvas cv, float w, float gridTop, float gridH, SlotArt.Theme th, long now) {
        float t = (now - winStartAt) / 1000f;
        if (t < 0 || t > (bigWinUntil > nowMillis() ? bigWinMs / 1000f : 1.6f)) return;
        int coinsN = 22 + (bigWinUntil > now ? 18 * (bigWinTier + 1) : 0);
        for (int i = 0; i < coinsN; i++) {
            float x = w * (((i * 73856093) & 0xFFFF) / 65535f);
            float speed = gridH * (0.55f + (i % 5) * 0.16f);
            float y = gridTop - w * .05f + ((t * speed + i * 37) % (gridH + w * .12f));
            float r = w * (.009f + (i % 3) * .004f);
            p.setColor(i % 4 == 0 ? 0xFFFFF0AA : th.title);
            cv.drawCircle(x, y, r, p);
            p.setColor(0x66FFFFFF);
            cv.drawCircle(x - r * .3f, y - r * .3f, r * .35f, p);
        }
    }

    private void bigWinOverlay(Canvas cv, float w, SlotArt.Theme th, long now) {
        float t = (now - bigWinStartAt) / (float) bigWinMs;
        if (t < 0 || t > 1) return;
        float cy = getHeight() * 0.42f;
        p.setColor(0x99000000);
        cv.drawRect(0, 0, w, getHeight(), p);
        float in = Math.min(1f, t / 0.12f);
        float pop = in < 1f ? 0.6f + 0.4f * in : 1f + 0.05f * (float) Math.sin(now / 140.0);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(w * 0.14f * pop);
        p.setColor(TIER_COLOR[bigWinTier]);
        p.setShadowLayer(w * 0.03f * pop, 0, 0, 0xFFFFA000);
        cv.drawText(TIER_NAME[bigWinTier], w / 2, cy, p);
        p.clearShadowLayer();
        long shown = bigWinAmount * Math.min(100, (long) (t * 140 * 3000f / bigWinMs)) / 100;
        p.setTextSize(w * 0.10f * pop);
        p.setColor(0xFFFFFFFF);
        p.setShadowLayer(w * 0.02f, 0, 0, 0xFF7CFC00);
        cv.drawText("+" + shown, w / 2, cy + w * 0.14f, p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);
    }

    private void levelBurst(Canvas cv, float w, float gridTop, SlotArt.Theme th, long now) {
        float p1 = 1 - Math.max(0, (levelFlashUntil - now)) / 2000f;
        if (p1 < 0 || p1 > 1) return;
        float cy = gridTop() + cellH() * 1.5f + 8;
        float r = w * .10f + p1 * w * .45f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * .012f * (1 - p1) + 1);
        p.setColor(th.title & 0x00FFFFFF | ((int) ((1 - p1) * 160) << 24));
        cv.drawCircle(w / 2, cy, r, p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(w * 0.075f);
        p.setColor(th.title);
        p.setFakeBoldText(true);
        p.setShadowLayer(w * 0.02f, 0, 0, 0xFF000000);
        cv.drawText("LEVEL UP!", w / 2, cy, p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);
    }

    private final android.os.Handler lpHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean spinHeld = false;       // finger is down on SPIN
    private final Runnable longPressAuto = new Runnable() { @Override public void run() {
        if (spinHeld && !autoSpin && balance >= BETS[betIdx]) { // silent: no out-of-coins dialog from a long press
            autoSpin = true; autoCount = 0; performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            if (!spinning) autoStep(); else invalidate();
        } } };

    @Override public boolean onTouchEvent(MotionEvent e) {
        int act = e.getActionMasked();
        if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) { spinHeld = false; lpHandler.removeCallbacks(longPressAuto); return true; }
        if (act != MotionEvent.ACTION_DOWN) return true;
        float w = getWidth();
        float gridTop = gridTop();
        float ctrlY = ctrlY(w, getHeight());
        float btnH = getHeight() * 0.07f;
        float x = e.getX(), y = e.getY();
        if (chipPlus.contains(x, y)) { if (listener != null) listener.onOpenShop(); return true; }
        if (y >= ctrlY && y <= ctrlY + btnH) {
            if (x >= w * 0.03f && x <= w * 0.40f) {
                if (!spinning && !autoSpin) betStep(x < w * 0.03f + w * 0.37f * 0.33f ? -1 : 1);
            } else if (x > w * 0.40f && x <= w * 0.97f) {
                // tap: one spin; hold ~0.5 s: the spin continues as auto; any tap while auto stops it
                if (autoSpin) stopAuto();
                else if (spinning) quickStop();
                else { spin(); spinHeld = true; lpHandler.postDelayed(longPressAuto, 550); }
            }
            return true;
        }
        if (y >= trailTop() && y <= trailBottom()) { if (listener != null) listener.onOpenMap(); return true; }
        // progression trail: tap an unlocked stop to apply its theme
        float tt = trailTop(), tb = trailBottom();
        if (y >= tt && y <= Math.min(tb, tt + w * .68f)) {
            float ny = trailNodeY();
            if (Math.abs(y - ny) <= w * .12f) {
                int best = -1; float bd = Float.MAX_VALUE;
                for (int i = 0; i < SlotArt.THEMES.length; i++) {
                    float d = Math.abs(x - SlotArt.nodeX(w, i));
                    if (d < bd) { bd = d; best = i; }
                }
                if (best >= 0 && bd <= w * .11f) selectTheme(best);
            }
            return true;
        }
        return true;
    }
}

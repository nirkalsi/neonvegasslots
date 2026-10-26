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
    }

    private static final int[] WEIGHT = {4, 7, 10, 16, 18, 12};
    private static final int[] PAY5 = {100, 50, 30, 20, 15, 25};
    private static final int[] PAY4 = {25, 12, 8, 5, 4, 6};
    private static final int[] PAY3 = {8, 4, 3, 2, 2, 2};
    private static final int[][] LINES = {
        {1,1,1,1,1}, {0,0,0,0,0}, {2,2,2,2,2}, {0,1,2,1,0}, {2,1,0,1,2}
    };
    private static final int[] BETS = {5, 10, 25, 50};

    private final SlotArt art = new SlotArt();
    private final Random rng = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
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
    private boolean anticipationGiven = false;
    private boolean[][] winCell = new boolean[3][5];
    private float titleGlow = 0f;

    public SlotsView(Context c) { super(c); randomizeAll(); }

    long nowMillis() { return System.currentTimeMillis(); } // overridable for tests

    public void setListener(Listener l) { listener = l; }
    public void addCoins(long n) { balance += n; persist(); invalidate(); }
    public long getBalance() { return balance; }
    public int getLevel() { return level; }
    public long getSpinsDone() { return spinsDone; }
    public int getThemeIdx() { return themeIdx; }
    private long xpNeeded() { return level * 500L; }

    static int unlockedCount(int level) {
        int n = 0; for (SlotArt.Theme t : SlotArt.THEMES) if (level >= t.unlockLevel) n++;
        return n;
    }

    /** Tap-to-apply an unlocked machine theme. Returns false when still locked. */
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
            betIdx = Math.min(BETS.length - 1, Math.max(0, Integer.parseInt(a[2])));
            if (a.length > 3) lastRegenAt = Long.parseLong(a[3]);
            if (a.length > 4) xp = Long.parseLong(a[4]);
            if (a.length > 5) level = Math.max(1, Integer.parseInt(a[5]));
            if (a.length > 6) {
                int t = Integer.parseInt(a[6]);
                themeIdx = (t >= 0 && t < SlotArt.THEMES.length && level >= SlotArt.THEMES[t].unlockLevel) ? t : 0;
            }
            if (a.length > 7) trophyMask = Integer.parseInt(a[7]) & 0xFFF;
            if (a.length > 8) trophyDay = a[8];
        } catch (Exception ignored) {}
    }

    private void persist() { if (listener != null) listener.saveState(); }

    private int pickSymbol() {
        int total = 0; for (int w : WEIGHT) total += w;
        int r = rng.nextInt(total);
        for (int i = 0; i < WEIGHT.length; i++) { r -= WEIGHT[i]; if (r < 0) return i; }
        return 0;
    }

    private void randomizeAll() {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 5; c++) grid[r][c] = pickSymbol();
    }

    private void regen() {
        long now = System.currentTimeMillis();
        if (balance < 100 && now - lastRegenAt >= 60000) {
            long steps = (now - lastRegenAt) / 60000;
            balance = Math.min(100, balance + 5 * steps);
            lastRegenAt = now;
            persist();
        }
    }

    private SoundFx sfx;
    private final boolean[] stopSounded = new boolean[5];
    public void setSoundFx(SoundFx s) { sfx = s; }
    private void fx(SoundFx.Effect e) { if (sfx != null) sfx.play(e); }

    private void spin() {
        if (spinning) return;
        regen();
        long bet = BETS[betIdx];
        if (balance < bet) { if (listener != null) listener.onOutOfCoins(); return; }
        balance -= bet;
        xp += bet;
        lastWin = 0;
        spinning = true;
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
        long win = 0;
        for (int[] line : LINES) {
            int s = grid[line[0]][0];
            int run = 1;
            for (int c = 1; c < 5; c++) { if (grid[line[c]][c] == s) run++; else break; }
            long pay = 0;
            if (run >= 5) pay = PAY5[s]; else if (run == 4) pay = PAY4[s]; else if (run == 3) pay = PAY3[s];
            if (pay > 0) {
                win += pay * bet / 10;
                for (int c = 0; c < run; c++) winCell[line[c]][c] = true;
            }
        }
        if (win > 0) {
            balance += win; lastWin = win;
            fx(win >= 25 * bet ? SoundFx.Effect.BIG_WIN : SoundFx.Effect.WIN);
            winStartAt = System.currentTimeMillis();
            if (win >= 25 * bet) { bigWinStartAt = winStartAt; bigWinUntil = winStartAt + 3000; bigWinAmount = win; winFlashUntil = winStartAt + 3000; }
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
        if (listener != null) {
            if (win >= 25 * bet) listener.onBigWin(win, bet);
            listener.onSpinComplete((int) spinsDone, win, bet);
        }
        invalidate();
    }

    private float gridTop() { return getHeight() * 0.22f; }
    private float trailTop() { return gridTop() + (getWidth() / 5f - 8) * 3 + 16 + 24 + getHeight() * 0.07f + getWidth() * 0.055f; }
    private float trailBottom() { return getHeight() - getWidth() * 0.10f; }
    private float trailNodeY() {
        float top = trailTop(), height = Math.min(trailBottom() - top, getWidth() * .68f);
        return top + height * .38f;
    }

    @Override protected void onDraw(Canvas cv) {
        long now = nowMillis();
        regen();
        SlotArt.Theme th = SlotArt.THEMES[themeIdx];
        int phase = (int) (now / 350);
        art.backdrop(cv, getWidth(), getHeight(), th, phase);

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
        float gridH = cellW * 3 + 16;

        // title
        titleGlow = (float) ((now % 4000L) / 4000.0 * 6.2832); // elapsed-time pulse: same speed at any redraw cadence
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(w * 0.075f);
        p.setFakeBoldText(true);
        p.setColor(th.title);
        p.setShadowLayer(14 + 6 * (float) Math.sin(titleGlow), 0, 0, th.titleGlow);
        cv.drawText("NEON VEGAS SLOTS", w / 2, gridTop * 0.42f, p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);

        // balance / win
        p.setTextSize(w * 0.05f);
        p.setColor(0xFFFFFFFF);
        cv.drawText("COINS  " + balance, w / 2, gridTop * 0.66f, p);
        // level + xp bar
        p.setTextSize(w * 0.034f);
        p.setColor(th.soft);
        cv.drawText("LV " + level, w * 0.14f, gridTop * 0.90f, p);
        float barL = w * 0.24f, barR = w * 0.76f, barY = gridTop * 0.855f, barH = w * 0.022f;
        rf.set(barL, barY, barR, barY + barH);
        p.setColor(th.cellStroke); cv.drawRoundRect(rf, 8, 8, p);
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
                float y = gridTop + r * (cellW + 8);
                // settle bounce: a just-stopped column drops in with a small overshoot
                float settle = (now - stopAt[c]) / 260f;
                if (settle >= 0 && settle < 1) y -= cellW * 0.12f * (1 - settle) * (float) Math.cos(settle * 6.9f);
                rf.set(x + 2, y, x + cellW + 2, y + cellW);
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
                    float pitch = cellW * 0.82f;
                    float scroll = (now * 1.15f + c * 173 + r * 61) % pitch;
                    cv.save();
                    cv.clipRect(x + 2, y, x + cellW + 2, y + cellW);
                    int sym = grid[r][c];
                    float cx = x + 2 + cellW / 2, cy = y + cellW / 2;
                    art.symbol(cv, sym, cx, cy - pitch + scroll, cellW * 0.74f);
                    p.setColor(0x30FFFFFF); cv.drawRect(x + 2, cy - pitch * 1.4f + scroll, x + cellW + 2, cy - pitch * 0.55f + scroll, p);
                    art.symbol(cv, sym, cx, cy + scroll, cellW * 0.82f);
                    art.symbol(cv, sym, cx, cy + pitch + scroll, cellW * 0.74f);
                    p.setColor(0x30FFFFFF); cv.drawRect(x + 2, cy + pitch * 0.55f + scroll, x + cellW + 2, cy + pitch * 1.4f + scroll, p);
                    cv.restore();
                } else {
                    art.symbol(cv, grid[r][c], x + 2 + cellW / 2, y + cellW / 2, cellW * 0.82f);
                }
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
                    rf.set(gx, gridTop - 2, gx + cellWn + 4, gridTop + cellWn * 3 + 22);
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

        // bet selector
        float ctrlY = gridTop + gridH + 24;
        float btnH = getHeight() * 0.07f;
        p.setTextSize(w * 0.045f);
        rf.set(w * 0.06f, ctrlY, w * 0.36f, ctrlY + btnH);
        p.setColor(th.betBg); cv.drawRoundRect(rf, 14, 14, p);
        p.setColor(th.title); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); cv.drawRoundRect(rf, 14, 14, p); p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        cv.drawText("BET  " + BETS[betIdx], w * 0.21f, ctrlY + btnH * 0.65f, p);

        // spin button
        rf.set(w * 0.44f, ctrlY, w * 0.94f, ctrlY + btnH);
        boolean canSpin = !spinning && balance >= BETS[betIdx];
        p.setColor(spinning ? 0xFF555555 : (canSpin ? th.spinBg : th.spinOff));
        cv.drawRoundRect(rf, 14, 14, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(w * 0.06f);
        p.setFakeBoldText(true);
        cv.drawText(spinning ? "..." : "SPIN", w * 0.69f, ctrlY + btnH * 0.66f, p);
        p.setFakeBoldText(false);

        String ju;
        if (now < trophyFlashUntil) ju = "TROPHY:" + trophyNewName;
        else ju = now < unlockFlashUntil ? unlockedName : null;
        art.trail(cv, w, trailTop(), trailBottom(), level, xp, xpNeeded(), themeIdx, ju, trophyMask);
        if (setComplete(themeIdx)) art.frameOrnament(cv, w / 2, gridTop - w * .022f, w * .05f, th);

        // disclaimer
        p.setTextSize(w * 0.028f);
        p.setColor(th.soft);
        cv.drawText("Free play only. Virtual coins - no real money, no prizes.", w / 2, getHeight() - w * 0.03f, p);
    }

    private void coinShower(Canvas cv, float w, float gridTop, float gridH, SlotArt.Theme th, long now) {
        float t = (now - winStartAt) / 1000f;
        if (t < 0 || t > 1.6f) return;
        for (int i = 0; i < 22; i++) {
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
        float t = (now - bigWinStartAt) / 3000f;
        if (t < 0 || t > 1) return;
        float cy = getHeight() * 0.42f;
        p.setColor(0x99000000);
        cv.drawRect(0, 0, w, getHeight(), p);
        float in = Math.min(1f, t / 0.12f);
        float pop = in < 1f ? 0.6f + 0.4f * in : 1f + 0.05f * (float) Math.sin(now / 140.0);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(w * 0.14f * pop);
        p.setColor(0xFFFFD54A);
        p.setShadowLayer(w * 0.03f * pop, 0, 0, 0xFFFFA000);
        cv.drawText("BIG WIN!", w / 2, cy, p);
        p.clearShadowLayer();
        long shown = bigWinAmount * Math.min(100, (long) (t * 140)) / 100;
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
        float cy = gridTop() + (getWidth() / 5f - 8) * 1.5f + 8;
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

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return true;
        float w = getWidth();
        float gridTop = gridTop();
        float ctrlY = gridTop + (w / 5f - 8) * 3 + 16 + 24;
        float btnH = getHeight() * 0.07f;
        float x = e.getX(), y = e.getY();
        if (y >= ctrlY && y <= ctrlY + btnH) {
            if (x >= w * 0.06f && x <= w * 0.36f) {
                if (!spinning) { betIdx = (betIdx + 1) % BETS.length; persist(); invalidate(); }
            } else if (x >= w * 0.44f && x <= w * 0.94f) {
                spin();
            }
            return true;
        }
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

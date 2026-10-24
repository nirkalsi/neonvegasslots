package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

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

    private boolean spinning = false;
    private long[] stopAt = new long[5];
    private long winFlashUntil = 0;
    private boolean[][] winCell = new boolean[3][5];
    private float titleGlow = 0f;

    public SlotsView(Context c) { super(c); randomizeAll(); }

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

    public String serialize() { return balance + ";" + spinsDone + ";" + betIdx + ";" + lastRegenAt + ";" + xp + ";" + level + ";" + themeIdx; }
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
        for (int c = 0; c < 5; c++) stopAt[c] = now + 600 + c * 380;
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
        if (win > 0) { balance += win; lastWin = win; winFlashUntil = System.currentTimeMillis() + 1600; }
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
        float top = trailTop(), height = Math.min(trailBottom() - top, getWidth() * .52f);
        return top + height * .48f;
    }

    @Override protected void onDraw(Canvas cv) {
        long now = System.currentTimeMillis();
        regen();
        SlotArt.Theme th = SlotArt.THEMES[themeIdx];
        int phase = (int) (now / 350);
        art.backdrop(cv, getWidth(), getHeight(), th, phase);

        float w = getWidth();
        float cellW = w / 5f - 8;
        float gridTop = gridTop();
        float gridH = cellW * 3 + 16;

        // title
        titleGlow = (titleGlow + 0.03f) % 6.28f;
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
        if (now < levelFlashUntil) {
            p.setTextSize(w * 0.055f);
            p.setColor(th.title);
            p.setFakeBoldText(true);
            cv.drawText("LEVEL UP!", w / 2, gridTop * 0.30f + w * 0.07f, p);
            p.setFakeBoldText(false);
        }
        if (lastWin > 0 && now < winFlashUntil) {
            p.setColor(0xFF7CFC00);
            p.setTextSize(w * 0.06f);
            p.setFakeBoldText(true);
            cv.drawText("WIN +" + lastWin, w / 2, gridTop * 0.95f, p);
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
                rf.set(x + 2, y, x + cellW + 2, y + cellW);
                boolean hot = winCell[r][c] && flash;
                p.setColor(hot ? th.cellHot : th.cellBg);
                cv.drawRoundRect(rf, 12, 12, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                p.setColor(hot ? th.cellHotStroke : th.cellStroke);
                cv.drawRoundRect(rf, 12, 12, p);
                p.setStyle(Paint.Style.FILL);
                art.symbol(cv, grid[r][c], x + 2 + cellW / 2, y + cellW / 2, cellW * 0.82f);
            }
        }
        if (spinning) {
            boolean allStopped = true;
            for (int c = 0; c < 5; c++) if (now < stopAt[c]) allStopped = false;
            if (allStopped) { post(this::finishSpin); }
            else postInvalidateDelayed(30);
        } else if (now < winFlashUntil || now < unlockFlashUntil) {
            postInvalidateDelayed(120);
        }

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

        String ju = now < unlockFlashUntil ? unlockedName : null;
        art.trail(cv, w, trailTop(), trailBottom(), level, xp, xpNeeded(), themeIdx, ju);

        // disclaimer
        p.setTextSize(w * 0.028f);
        p.setColor(th.soft);
        cv.drawText("Free play only. Virtual coins - no real money, no prizes.", w / 2, getHeight() - w * 0.03f, p);
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
        if (y >= tt && y <= Math.min(tb, tt + w * .52f)) {
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

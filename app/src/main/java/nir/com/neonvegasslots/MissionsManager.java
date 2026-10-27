package nir.com.neonvegasslots;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.*;

/** Daily missions + lifetime achievements. Progress tracked in prefs, claimed via dialog. */
public class MissionsManager {
    public static class Item {
        public String id, title;
        public long goal, reward, progress;
        public boolean claimed;
        public boolean done() { return progress >= goal; }
    }

    private final SharedPreferences sp;
    private final Runnable onChanged;

    public MissionsManager(Context c, Runnable onChanged) {
        sp = c.getSharedPreferences("neonvegasslots", Context.MODE_PRIVATE);
        this.onChanged = onChanged;
        rollDailyIfNeeded();
    }

    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    /** Week id = Monday date (local) of the current week. The Neon Quest ends at the next Monday 00:00 local, a fixed real time. */
    static String weekId(long nowMs) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(nowMs);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        int back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday=0
        c.add(Calendar.DAY_OF_MONTH, -back);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }
    static long weekEndMs(long nowMs) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(nowMs);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        int back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back + 7);
        return c.getTimeInMillis();
    }
    private void rollWeeklyIfNeeded() {
        String w = weekId(System.currentTimeMillis());
        if (!w.equals(sp.getString("quest_week", ""))) {
            sp.edit().putString("quest_week", w).putLong("wq", 0)
                .putBoolean("wq0c", false).putBoolean("wq1c", false).putBoolean("wq2c", false).putBoolean("wq3c", false).putBoolean("wq4c", false).apply();
        }
    }
    /** Neon Quest: weekly spin milestones, free to progress, same rewards for everyone, ends at a real fixed time. */
    public List<Item> weeklyQuest() {
        rollWeeklyIfNeeded();
        long n = sp.getLong("wq", 0);
        List<Item> out = new ArrayList<>();
        out.add(mk("wq0", "Quest: spin 20 times", 20, 100, n, sp.getBoolean("wq0c", false)));
        out.add(mk("wq1", "Quest: spin 100 times", 100, 400, n, sp.getBoolean("wq1c", false)));
        out.add(mk("wq2", "Quest: spin 300 times", 300, 1000, n, sp.getBoolean("wq2c", false)));
        out.add(mk("wq3", "Quest: spin 600 times", 600, 2200, n, sp.getBoolean("wq3c", false)));
        out.add(mk("wq4", "Quest: spin 1,000 times - Neon Jackpot", 1000, 4000, n, sp.getBoolean("wq4c", false)));
        return out;
    }
    /** Human text for the real time left, e.g. "2d 5h left". */
    public String questTimeLeft() {
        long ms = Math.max(0, weekEndMs(System.currentTimeMillis()) - System.currentTimeMillis());
        long h = ms / 3600000, d = h / 24;
        return d > 0 ? d + "d " + (h % 24) + "h left" : h + "h " + ((ms / 60000) % 60) + "m left";
    }

    private void rollDailyIfNeeded() {
        if (!today().equals(sp.getString("missions_day", ""))) {
            sp.edit()
                .putString("missions_day", today())
                .putLong("dm1", 0).putLong("dm2", 0).putLong("dm3", 0)
                .putBoolean("dm1c", false).putBoolean("dm2c", false).putBoolean("dm3c", false)
                .apply();
        }
    }

    /** Call after each completed spin. */
    public void onSpin(long win, long bet, int level, long totalSpins) {
        rollDailyIfNeeded();
        rollWeeklyIfNeeded();
        SharedPreferences.Editor e = sp.edit();
        e.putLong("wq", sp.getLong("wq", 0) + 1);
        e.putLong("dm1", sp.getLong("dm1", 0) + 1);
        if (win > 0) {
            e.putLong("dm2", sp.getLong("dm2", 0) + 1);
            e.putLong("dm3", sp.getLong("dm3", 0) + win);
        }
        if (win >= 25 * bet) e.putBoolean("ach4_done", true);
        e.apply();
        if (onChanged != null) onChanged.run();
    }

    public void onDailyClaimed(int streak) {
        if (streak >= 3) sp.edit().putBoolean("ach6_done", true).apply();
    }

    public List<Item> dailyMissions() {
        rollDailyIfNeeded();
        List<Item> out = new ArrayList<>();
        out.add(mk("dm1", "Spin 25 times today", 25, 300, sp.getLong("dm1", 0), sp.getBoolean("dm1c", false)));
        out.add(mk("dm2", "Win 8 times today", 8, 200, sp.getLong("dm2", 0), sp.getBoolean("dm2c", false)));
        out.add(mk("dm3", "Win 1,000 coins today", 1000, 400, sp.getLong("dm3", 0), sp.getBoolean("dm3c", false)));
        return out;
    }

    public List<Item> achievements(int level, long totalSpins) {
        List<Item> out = new ArrayList<>();
        out.add(mkA("ach1", "First spin", 1, 100, totalSpins, sp.getBoolean("ach1c", false)));
        out.add(mkA("ach2", "100 spins", 100, 500, totalSpins, sp.getBoolean("ach2c", false)));
        out.add(mkA("ach3", "1,000 spins", 1000, 2000, totalSpins, sp.getBoolean("ach3c", false)));
        out.add(mkA("ach4", "High roller: win 25x your bet in one spin", 1, 1000,
            sp.getBoolean("ach4_done", false) ? 1 : 0, sp.getBoolean("ach4c", false)));
        out.add(mkA("ach5", "Reach level 5", 5, 1500, level, sp.getBoolean("ach5c", false)));
        out.add(mkA("ach6", "3-day daily bonus streak", 1, 800,
            sp.getBoolean("ach6_done", false) ? 1 : 0, sp.getBoolean("ach6c", false)));
        return out;
    }

    private Item mk(String id, String title, long goal, long reward, long progress, boolean claimed) {
        Item i = new Item(); i.id = id; i.title = title; i.goal = goal; i.reward = reward;
        i.progress = Math.min(progress, goal); i.claimed = claimed; return i;
    }
    private Item mkA(String id, String title, long goal, long reward, long progress, boolean claimed) {
        return mk(id, title, goal, reward, progress, claimed);
    }

    /** Marks claimed; caller grants the coins. Returns reward or 0. */
    public long claim(Item i) {
        if (i.claimed || !i.done()) return 0;
        sp.edit().putBoolean(i.id + "c", true).apply();
        if (onChanged != null) onChanged.run();
        return i.reward;
    }

    public int claimableCount(int level, long totalSpins) {
        int n = 0;
        for (Item i : dailyMissions()) if (i.done() && !i.claimed) n++;
        for (Item i : weeklyQuest()) if (i.done() && !i.claimed) n++;
        for (Item i : achievements(level, totalSpins)) if (i.done() && !i.claimed) n++;
        return n;
    }

    public boolean hasClaimable(int level, long totalSpins) {
        for (Item i : dailyMissions()) if (i.done() && !i.claimed) return true;
        for (Item i : weeklyQuest()) if (i.done() && !i.claimed) return true;
        for (Item i : achievements(level, totalSpins)) if (i.done() && !i.claimed) return true;
        return false;
    }
}

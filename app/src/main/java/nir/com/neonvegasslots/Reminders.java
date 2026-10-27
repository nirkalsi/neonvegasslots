package nir.com.neonvegasslots;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Opt-in LOCAL daily reminder. Scheduled on the device with AlarmManager (inexact), no server, no push service,
 *  no data leaves the phone. Off by default. Up to 4 slots a day, each posted only if true (in practice bonus-only: 2 a day at most), never before 10:30 or after 20:30. */
public class Reminders extends BroadcastReceiver {
    static final String PREFS_KEY = "remind_on", CHANNEL = "daily_reminder";

    /** Next 10:30 local time strictly after now + 6h, so a player who just closed the app is not nudged the same day. */
    static long nextFireAt(long nowMs) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(nowMs + 6L * 3600000L);
        c.set(Calendar.HOUR_OF_DAY, 10); c.set(Calendar.MINUTE, 30); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= nowMs + 6L * 3600000L) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    // Local daily slots (hour*60+min): morning, early afternoon, evening, late evening. Never before 10:30 or after 20:30.
    static final int[] SLOTS = {10 * 60 + 30, 13 * 60 + 30, 18 * 60 + 30, 20 * 60 + 30};
    /** Next slot strictly after now + 2h (a player who just left is not nudged straight away). */
    static long nextSlotAt(long nowMs) {
        long min = nowMs + 2L * 3600000L;
        for (int d = 0; d < 3; d++) for (int sl : SLOTS) {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(nowMs); c.add(Calendar.DAY_OF_MONTH, d);
            c.set(Calendar.HOUR_OF_DAY, sl / 60); c.set(Calendar.MINUTE, sl % 60); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            if (c.getTimeInMillis() > min) return c.getTimeInMillis();
        }
        return min + 3600000L;
    }
    /** What is TRUE at this slot, or null to post nothing. Only the daily bonus state is known on the device, so only that is ever claimed.
     *  Slot 0: bonus ready. Slot 2: bonus still unclaimed. Slots 1 and 3 post nothing (no other true event exists). So at most 2 a day, usually 1. */
    static String messageFor(int slot, boolean bonusReady) {
        switch (slot) {
            case 0: return bonusReady ? "Your daily bonus is ready. Free virtual coins." : null;
            case 2: return bonusReady ? "Your daily bonus is still waiting for you." : null;
            default: return null;
        }
    }
    static int slotIndex(long ms) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ms); int m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == m) return i;
        return -1;
    }
    /** True only if the player opted in AND the system lets this app post notifications (permission not denied or revoked). */
    static boolean effective(Context c) {
        if (!isOn(c)) return false;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }
    /** The reminder text says the daily bonus is ready, so only post it when that is true: not claimed yet on today's date (same date rule as MainActivity). */
    static boolean bonusReady(String dailyLast, String today) { return dailyLast == null || !dailyLast.equals(today); }
    static boolean bonusReadyNow(Context c) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        return bonusReady(c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getString("daily_last", ""), today);
    }
    static boolean promptEnabled = true; // test seam: unit tests turn the first-run popup off
    static final String ASKED_KEY = "notif_consent_asked";
    /** Show the one-time yes/no popup only to a player who has never been exposed to the question. A player who already
     *  opted in, already answered, or whose system notifications are off / who denied the system prompt once (rationale true)
     *  is treated as a prior refusal and is never asked again; Settings stays the route. Old builds stored no "asked" flag,
     *  so these two system signals are the only evidence available of an earlier refusal. */
    static boolean shouldAsk(boolean legacyUpgrade, boolean askedFlag, boolean remindOn, boolean notificationsEnabled, boolean rationaleShown) {
        if (legacyUpgrade) return false; // upgraded from a build that stored no "asked" flag: refusals are indistinguishable, so no automatic popup (Settings still opts in)
        if (askedFlag || remindOn) return false;
        if (!notificationsEnabled) return false;
        if (rationaleShown) return false;
        return true;
    }
    /** True when this install existed before the build that introduced the popup (first install time differs from the last update by more than a minute).
     *  Decided once and stored, so later updates do not change the cohort. */
    static boolean legacyCohort(Context c) {
        SharedPreferences sp = c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (sp.contains("notif_cohort_legacy")) return sp.getBoolean("notif_cohort_legacy", true);
        boolean legacy = true;
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            legacy = isLegacy(pi.firstInstallTime, pi.lastUpdateTime);
        } catch (Exception e) { /* unknown: treat as legacy, never risk re-asking a refusal */ }
        sp.edit().putBoolean("notif_cohort_legacy", legacy).apply();
        return legacy;
    }
    static boolean isLegacy(long firstInstall, long lastUpdate) { return lastUpdate - firstInstall > 60000L; }
    static boolean asked(Context c) { return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getBoolean(ASKED_KEY, false); }
    static void markAsked(Context c) { c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean(ASKED_KEY, true).apply(); }
    static boolean isOn(Context c) { return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getBoolean(PREFS_KEY, false); }
    static void setOn(Context c, boolean on) {
        c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREFS_KEY, on).apply();
        if (on) schedule(c); else cancel(c);
    }

    private static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 77, new Intent(c, Reminders.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void schedule(Context c) {
        if (!isOn(c)) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC_WAKEUP, nextSlotAt(System.currentTimeMillis()), pi(c));
    }
    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c));
    }

    @Override public void onReceive(Context c, Intent i) {
        if (!isOn(c)) return;
        // Alarms do not survive a reboot: re-arm the single opt-in reminder, post nothing now.
        if (i != null && Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) { if (effective(c)) schedule(c); return; }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(now);
        // The alarm is inexact: classify by the nearest slot, and stay silent if it is more than 90 min from any slot.
        int mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE), best = -1, bd = 1 << 30;
        for (int k = 0; k < SLOTS.length; k++) { int d = Math.abs(SLOTS[k] - mins); if (d < bd) { bd = d; best = k; } }
        String text = bd <= 90 ? messageFor(best, bonusReadyNow(c)) : null;
        schedule(c); // chain the next slot (still opt-in; cancelled if the player returns first)
        if (text == null) return; // nothing true to say at this slot
        if (android.os.Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent op = PendingIntent.getActivity(c, 78, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, CHANNEL) : new android.app.Notification.Builder(c);
        b.setSmallIcon(c.getApplicationInfo().icon).setContentTitle(c.getString(R.string.app_name))
         .setContentText(text).setContentIntent(op).setAutoCancel(true);
        nm.notify(77, b.build());
    }
}

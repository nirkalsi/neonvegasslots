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
 *  no data leaves the phone. Off by default. At most one reminder per day, never at night (fires at 10:30 local). */
public class Reminders extends BroadcastReceiver {
    static final String PREFS_KEY = "remind_on", CHANNEL = "daily_reminder";

    /** Next 10:30 local time strictly after now + 6h, so a player who just closed the app is not nudged the same day. */
    static long nextFireAt(long nowMs) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(nowMs + 6L * 3600000L);
        c.set(Calendar.HOUR_OF_DAY, 10); c.set(Calendar.MINUTE, 30); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= nowMs + 6L * 3600000L) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
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
        if (am != null) am.set(AlarmManager.RTC_WAKEUP, nextFireAt(System.currentTimeMillis()), pi(c));
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
        if (nm == null || !bonusReadyNow(c)) return; // bonus already claimed today: nothing true to say
        if (android.os.Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent op = PendingIntent.getActivity(c, 78, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, CHANNEL) : new android.app.Notification.Builder(c);
        b.setSmallIcon(c.getApplicationInfo().icon).setContentTitle(c.getString(R.string.app_name))
         .setContentText("Your daily bonus is ready. Free virtual coins.").setContentIntent(op).setAutoCancel(true);
        nm.notify(77, b.build());
        // one reminder only: the next one is scheduled when the player next leaves the game
    }
}

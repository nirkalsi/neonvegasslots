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
        if (am != null) am.set(AlarmManager.RTC, nextFireAt(System.currentTimeMillis()), pi(c));
    }
    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c));
    }

    @Override public void onReceive(Context c, Intent i) {
        if (!isOn(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
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

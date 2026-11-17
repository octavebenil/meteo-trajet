package io.github.octavebenil.personalweather.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.ui.MainActivity;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Schedules the daily morning weather check at the time chosen by the user. */
public final class AlarmScheduler {

    private static final int REQUEST_ALARM = 1001;
    private static final int REQUEST_SHOW = 1002;

    private AlarmScheduler() {
    }

    /** Next occurrence of {@code alarmTime} strictly after {@code now}. */
    public static ZonedDateTime nextTrigger(ZonedDateTime now, LocalTime alarmTime) {
        ZonedDateTime candidate = now.with(alarmTime).withSecond(0).withNano(0);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1).with(alarmTime);
        }
        return candidate;
    }

    /** Schedules (or cancels) the alarm according to the current settings. */
    public static void apply(Context context) {
        SettingsStore settings = new SettingsStore(context);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent operation = alarmIntent(context);
        manager.cancel(operation);
        if (!settings.isAlarmEnabled()) {
            return;
        }
        long triggerAt = nextTrigger(ZonedDateTime.now(ZoneId.systemDefault()), settings.getAlarmTime())
                .toInstant().toEpochMilli();

        if (canScheduleExact(manager)) {
            PendingIntent show = PendingIntent.getActivity(context, REQUEST_SHOW,
                    new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, show), operation);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        }
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && canScheduleExact(manager);
    }

    private static boolean canScheduleExact(AlarmManager manager) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms();
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, MorningAlarmReceiver.class)
                .setAction(MorningAlarmReceiver.ACTION_MORNING_ALARM);
        return PendingIntent.getBroadcast(context, REQUEST_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

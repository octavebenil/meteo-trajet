package io.github.octavebenil.personalweather.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import io.github.octavebenil.personalweather.R;
import io.github.octavebenil.personalweather.domain.CommuteAdvice;
import io.github.octavebenil.personalweather.domain.HourlyForecast;
import io.github.octavebenil.personalweather.scheduling.MorningAlarmService;
import io.github.octavebenil.personalweather.ui.MainActivity;
import io.github.octavebenil.personalweather.ui.WeatherText;

/** Creates notification channels and posts the app notifications. */
public final class Notifications {

    public static final int ID_CHECKING = 10;
    public static final int ID_MORNING = 11;
    public static final int ID_RAIN = 12;

    private static final String CHANNEL_MORNING = "morning_advice";
    private static final String CHANNEL_RAIN = "rain_alerts";
    private static final String CHANNEL_SERVICE = "background_check";

    private Notifications() {
    }

    public static void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel morning = new NotificationChannel(CHANNEL_MORNING,
                context.getString(R.string.channel_morning), NotificationManager.IMPORTANCE_HIGH);
        morning.setDescription(context.getString(R.string.channel_morning_desc));
        NotificationChannel rain = new NotificationChannel(CHANNEL_RAIN,
                context.getString(R.string.channel_rain), NotificationManager.IMPORTANCE_HIGH);
        rain.setDescription(context.getString(R.string.channel_rain_desc));
        NotificationChannel service = new NotificationChannel(CHANNEL_SERVICE,
                context.getString(R.string.channel_service), NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(morning);
        manager.createNotificationChannel(rain);
        manager.createNotificationChannel(service);
    }

    public static Notification checkingNotification(Context context) {
        createChannels(context);
        return new NotificationCompat.Builder(context, CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.checking_weather))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    /** Replaces the foreground notification with one that lets the user stop the ringing. */
    public static void updateRingingNotification(Context context) {
        PendingIntent stop = PendingIntent.getService(context, 0,
                new Intent(context, MorningAlarmService.class).setAction(MorningAlarmService.ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_MORNING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.alarm_ringing))
                .setContentText(context.getString(R.string.alarm_ringing_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .setOngoing(true)
                .setContentIntent(openAppAndStopAlarm(context))
                .setDeleteIntent(stop)
                .addAction(0, context.getString(R.string.stop), stop)
                .build();
        post(context, ID_CHECKING, notification);
    }

    public static void showMorningAdvice(Context context, CommuteAdvice advice, boolean fresh) {
        createChannels(context);
        String title = WeatherText.adviceTitle(context, advice);
        String text = WeatherText.adviceDetail(context, advice);
        if (!fresh) {
            text = text + "\n" + context.getString(R.string.data_may_be_outdated);
        }
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_MORNING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openApp(context))
                .setAutoCancel(true)
                .build();
        post(context, ID_MORNING, notification);
    }

    public static void showRainAlert(Context context, HourlyForecast hour) {
        createChannels(context);
        String text = WeatherText.rainAlertText(context, hour);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_RAIN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.rain_alert_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openApp(context))
                .setAutoCancel(true)
                .build();
        post(context, ID_RAIN, notification);
    }

    private static PendingIntent openAppAndStopAlarm(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_STOP_ALARM, true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent openApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** @return whether the app is allowed to show notifications. */
    public static boolean canPost(Context context) {
        boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressWarnings("MissingPermission")
    private static void post(Context context, int id, Notification notification) {
        if (!canPost(context)) {
            return;
        }
        NotificationManagerCompat.from(context).notify(id, notification);
    }
}

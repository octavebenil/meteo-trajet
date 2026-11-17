package io.github.octavebenil.personalweather.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

/** Fired by the morning alarm: starts the weather check and schedules tomorrow's alarm. */
public final class MorningAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_MORNING_ALARM =
            "io.github.octavebenil.personalweather.action.MORNING_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_MORNING_ALARM.equals(intent.getAction())) {
            return;
        }
        AlarmScheduler.apply(context);
        try {
            ContextCompat.startForegroundService(context, new Intent(context, MorningAlarmService.class));
        } catch (IllegalStateException e) {
            // Foreground service not allowed (e.g. inexact alarm): fall back to a silent check.
            WorkManager.getInstance(context).enqueueUniqueWork("morning-check",
                    ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(MorningCheckWorker.class)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build());
        }
    }
}

package io.github.octavebenil.personalweather.scheduling;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import io.github.octavebenil.personalweather.data.SettingsStore;

import java.util.concurrent.TimeUnit;

/** Keeps the forecast up to date during the day and checks for incoming rain. */
public final class DayRefreshScheduler {

    private static final String WORK_NAME = "day-refresh";
    private static final long INTERVAL_MINUTES = 60;

    private DayRefreshScheduler() {
    }

    public static void apply(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        if (!new SettingsStore(context).isDayAlertsEnabled()) {
            workManager.cancelUniqueWork(WORK_NAME);
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ForecastRefreshWorker.class, INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}

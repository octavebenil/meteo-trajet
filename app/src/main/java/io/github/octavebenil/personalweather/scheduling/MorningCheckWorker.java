package io.github.octavebenil.personalweather.scheduling;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.github.octavebenil.personalweather.notification.Notifications;

/** Fallback for the morning check when a foreground service cannot be started. */
public final class MorningCheckWorker extends Worker {

    public MorningCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        return new ForegroundInfo(Notifications.ID_CHECKING,
                Notifications.checkingNotification(getApplicationContext()));
    }

    @NonNull
    @Override
    public Result doWork() {
        MorningCheck.run(getApplicationContext());
        return Result.success();
    }
}

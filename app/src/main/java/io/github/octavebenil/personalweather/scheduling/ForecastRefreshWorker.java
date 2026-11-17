package io.github.octavebenil.personalweather.scheduling;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.data.WeatherRepository;
import io.github.octavebenil.personalweather.domain.HourlyForecast;
import io.github.octavebenil.personalweather.domain.RainAlertDetector;
import io.github.octavebenil.personalweather.notification.Notifications;

import java.time.LocalDateTime;

/** Hourly background refresh: updates the cache and warns about rain in the next hours. */
public final class ForecastRefreshWorker extends Worker {

    private static final int LOOK_AHEAD_HOURS = 3;

    public ForecastRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SettingsStore settings = new SettingsStore(context);
        WeatherRepository.Result result = new WeatherRepository(context).refresh(false);
        if (!result.fresh || result.forecast == null) {
            return Result.retry();
        }
        if (!settings.isDayAlertsEnabled()) {
            return Result.success();
        }
        HourlyForecast rainy = new RainAlertDetector(settings.getThreshold(), LOOK_AHEAD_HOURS)
                .nextRainyHour(result.forecast, LocalDateTime.now());
        if (rainy != null) {
            String key = rainy.getTime().toString();
            if (!key.equals(settings.getLastAlertHour())) {
                Notifications.showRainAlert(context, rainy);
                settings.setLastAlertHour(key);
            }
        }
        return Result.success();
    }
}

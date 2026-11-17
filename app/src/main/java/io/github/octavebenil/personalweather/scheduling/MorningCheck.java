package io.github.octavebenil.personalweather.scheduling;

import android.content.Context;

import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.data.WeatherRepository;
import io.github.octavebenil.personalweather.domain.CommuteAdvice;
import io.github.octavebenil.personalweather.domain.CommuteAdvisor;
import io.github.octavebenil.personalweather.notification.Notifications;

import java.time.LocalDate;

/** Morning routine: refresh the forecast, compute the commute advice and notify. */
final class MorningCheck {

    private MorningCheck() {
    }

    /** Blocking. @return the advice that was notified. */
    static CommuteAdvice run(Context context) {
        SettingsStore settings = new SettingsStore(context);
        // Android does not grant location to a background alarm without the
        // background-location permission, so the last saved position is used.
        WeatherRepository.Result result = new WeatherRepository(context).refresh(false);
        CommuteAdvice advice;
        if (result.forecast == null) {
            advice = new CommuteAdvice(CommuteAdvice.Recommendation.UNKNOWN, 0, 0,
                    java.util.Collections.emptyList());
        } else {
            advice = new CommuteAdvisor(settings.getThreshold())
                    .advise(result.forecast, LocalDate.now(), settings.getCommuteWindows());
        }
        Notifications.showMorningAdvice(context, advice, result.fresh);
        return advice;
    }
}

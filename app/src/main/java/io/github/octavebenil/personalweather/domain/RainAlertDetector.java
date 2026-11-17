package io.github.octavebenil.personalweather.domain;

import java.time.LocalDateTime;

/**
 * Looks ahead in the forecast during the day and finds the next rainy hour,
 * used by the periodic background refresh to warn about incoming rain.
 */
public final class RainAlertDetector {

    private final int thresholdPercent;
    private final int lookAheadHours;

    public RainAlertDetector(int thresholdPercent, int lookAheadHours) {
        this.thresholdPercent = thresholdPercent;
        this.lookAheadHours = lookAheadHours;
    }

    /** @return the first rainy hour within the look-ahead window, or {@code null}. */
    public HourlyForecast nextRainyHour(Forecast forecast, LocalDateTime now) {
        for (HourlyForecast hour : forecast.upcomingHours(now, lookAheadHours + 1)) {
            if (hour.isRainy(thresholdPercent)) {
                return hour;
            }
        }
        return null;
    }
}

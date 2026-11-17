package io.github.octavebenil.personalweather.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Helpers to build forecasts in tests. */
final class TestForecasts {

    static final LocalDate DAY = LocalDate.of(2026, 11, 17);

    private TestForecasts() {
    }

    /** 24 dry hours for {@link #DAY}, with the given overrides (hour → probability). */
    static Forecast dayWithProbabilities(int... hourAndProbability) {
        List<HourlyForecast> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            int probability = 0;
            for (int i = 0; i + 1 < hourAndProbability.length; i += 2) {
                if (hourAndProbability[i] == h) {
                    probability = hourAndProbability[i + 1];
                }
            }
            hours.add(new HourlyForecast(DAY.atTime(h, 0), 10, probability, 0,
                    WeatherCondition.CLOUDY, 5));
        }
        return new Forecast(10, 8, WeatherCondition.CLOUDY, 5, hours, LocalDateTime.of(DAY, java.time.LocalTime.of(6, 0)));
    }
}

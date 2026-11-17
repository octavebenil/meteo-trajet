package io.github.octavebenil.personalweather.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Current conditions plus the hour-by-hour forecast. Immutable. */
public final class Forecast {

    private final double currentTemperatureC;
    private final double apparentTemperatureC;
    private final WeatherCondition currentCondition;
    private final double currentWindSpeedKmh;
    private final List<HourlyForecast> hourly;
    private final LocalDateTime fetchedAt;

    public Forecast(double currentTemperatureC,
                    double apparentTemperatureC,
                    WeatherCondition currentCondition,
                    double currentWindSpeedKmh,
                    List<HourlyForecast> hourly,
                    LocalDateTime fetchedAt) {
        this.currentTemperatureC = currentTemperatureC;
        this.apparentTemperatureC = apparentTemperatureC;
        this.currentCondition = Objects.requireNonNull(currentCondition, "currentCondition");
        this.currentWindSpeedKmh = currentWindSpeedKmh;
        this.hourly = Collections.unmodifiableList(new ArrayList<>(hourly));
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
    }

    public double getCurrentTemperatureC() {
        return currentTemperatureC;
    }

    public double getApparentTemperatureC() {
        return apparentTemperatureC;
    }

    public WeatherCondition getCurrentCondition() {
        return currentCondition;
    }

    public double getCurrentWindSpeedKmh() {
        return currentWindSpeedKmh;
    }

    public List<HourlyForecast> getHourly() {
        return hourly;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    /** @return the hours starting with the one that contains {@code now}, at most {@code count}. */
    public List<HourlyForecast> upcomingHours(LocalDateTime now, int count) {
        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
        List<HourlyForecast> result = new ArrayList<>();
        for (HourlyForecast hour : hourly) {
            if (!hour.getTime().isBefore(currentHour)) {
                result.add(hour);
                if (result.size() >= count) {
                    break;
                }
            }
        }
        return result;
    }
}

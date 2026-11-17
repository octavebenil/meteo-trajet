package io.github.octavebenil.personalweather.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Forecast for a single hour. Immutable. */
public final class HourlyForecast {

    private final LocalDateTime time;
    private final double temperatureC;
    private final int precipitationProbability;
    private final double precipitationMm;
    private final WeatherCondition condition;
    private final double windSpeedKmh;

    public HourlyForecast(LocalDateTime time,
                          double temperatureC,
                          int precipitationProbability,
                          double precipitationMm,
                          WeatherCondition condition,
                          double windSpeedKmh) {
        this.time = Objects.requireNonNull(time, "time");
        this.temperatureC = temperatureC;
        this.precipitationProbability = Math.max(0, Math.min(100, precipitationProbability));
        this.precipitationMm = Math.max(0, precipitationMm);
        this.condition = Objects.requireNonNull(condition, "condition");
        this.windSpeedKmh = windSpeedKmh;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    /** @return probability of precipitation, from 0 to 100. */
    public int getPrecipitationProbability() {
        return precipitationProbability;
    }

    public double getPrecipitationMm() {
        return precipitationMm;
    }

    public WeatherCondition getCondition() {
        return condition;
    }

    public double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    /**
     * An hour is rainy when its precipitation probability reaches the threshold,
     * or when the model already expects a wet condition with measurable precipitation.
     */
    public boolean isRainy(int thresholdPercent) {
        return precipitationProbability >= thresholdPercent
                || (condition.isWet() && precipitationMm >= 0.1);
    }
}

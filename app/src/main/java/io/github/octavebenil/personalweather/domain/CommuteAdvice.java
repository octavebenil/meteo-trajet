package io.github.octavebenil.personalweather.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of the commute analysis for one day. Immutable. */
public final class CommuteAdvice {

    /** What the app suggests for the day. */
    public enum Recommendation {
        /** No meaningful rain risk on the commute: the bus is fine. */
        BUS,
        /** Rain is likely on the commute: taking the car is safer. */
        CAR,
        /** Not enough forecast data to decide. */
        UNKNOWN
    }

    private final Recommendation recommendation;
    private final int maxProbability;
    private final double totalPrecipitationMm;
    private final List<HourlyForecast> rainyHours;

    public CommuteAdvice(Recommendation recommendation,
                         int maxProbability,
                         double totalPrecipitationMm,
                         List<HourlyForecast> rainyHours) {
        this.recommendation = recommendation;
        this.maxProbability = maxProbability;
        this.totalPrecipitationMm = totalPrecipitationMm;
        this.rainyHours = Collections.unmodifiableList(new ArrayList<>(rainyHours));
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public int getMaxProbability() {
        return maxProbability;
    }

    public double getTotalPrecipitationMm() {
        return totalPrecipitationMm;
    }

    /** Hours of the commute windows where rain is expected, in chronological order. */
    public List<HourlyForecast> getRainyHours() {
        return rainyHours;
    }

    public boolean isRainRisk() {
        return recommendation == Recommendation.CAR;
    }
}

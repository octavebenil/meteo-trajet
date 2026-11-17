package io.github.octavebenil.personalweather.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides between bus and car by looking at the rain risk during the
 * outbound and return commute windows of a given day.
 */
public final class CommuteAdvisor {

    private final int thresholdPercent;

    public CommuteAdvisor(int thresholdPercent) {
        if (thresholdPercent < 0 || thresholdPercent > 100) {
            throw new IllegalArgumentException("threshold must be between 0 and 100");
        }
        this.thresholdPercent = thresholdPercent;
    }

    public CommuteAdvice advise(Forecast forecast, LocalDate day, List<TimeWindow> windows) {
        int maxProbability = 0;
        double totalMm = 0;
        int matchedHours = 0;
        List<HourlyForecast> rainy = new ArrayList<>();

        for (HourlyForecast hour : forecast.getHourly()) {
            if (!hour.getTime().toLocalDate().equals(day) || !isInAnyWindow(hour, windows)) {
                continue;
            }
            matchedHours++;
            maxProbability = Math.max(maxProbability, hour.getPrecipitationProbability());
            totalMm += hour.getPrecipitationMm();
            if (hour.isRainy(thresholdPercent)) {
                rainy.add(hour);
            }
        }

        CommuteAdvice.Recommendation recommendation;
        if (matchedHours == 0) {
            recommendation = CommuteAdvice.Recommendation.UNKNOWN;
        } else if (rainy.isEmpty()) {
            recommendation = CommuteAdvice.Recommendation.BUS;
        } else {
            recommendation = CommuteAdvice.Recommendation.CAR;
        }
        return new CommuteAdvice(recommendation, maxProbability, totalMm, rainy);
    }

    private static boolean isInAnyWindow(HourlyForecast hour, List<TimeWindow> windows) {
        for (TimeWindow window : windows) {
            if (window.overlapsHourStartingAt(hour.getTime().toLocalTime())) {
                return true;
            }
        }
        return false;
    }
}

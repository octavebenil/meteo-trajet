package io.github.octavebenil.personalweather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommuteAdvisorTest {

    private static final List<TimeWindow> WINDOWS = Arrays.asList(
            new TimeWindow(LocalTime.of(7, 0), LocalTime.of(9, 0)),
            new TimeWindow(LocalTime.of(17, 0), LocalTime.of(19, 0)));

    @Test
    public void recommendsBusWhenCommuteIsDry() {
        Forecast forecast = TestForecasts.dayWithProbabilities(7, 10, 8, 20, 12, 90);

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, WINDOWS);

        assertEquals(CommuteAdvice.Recommendation.BUS, advice.getRecommendation());
        assertEquals(20, advice.getMaxProbability());
        assertFalse(advice.isRainRisk());
    }

    @Test
    public void recommendsCarWhenMorningCommuteIsRainy() {
        Forecast forecast = TestForecasts.dayWithProbabilities(8, 30);

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, WINDOWS);

        assertEquals(CommuteAdvice.Recommendation.CAR, advice.getRecommendation());
        assertEquals(1, advice.getRainyHours().size());
        assertEquals(8, advice.getRainyHours().get(0).getTime().getHour());
    }

    @Test
    public void recommendsCarWhenReturnCommuteIsRainy() {
        Forecast forecast = TestForecasts.dayWithProbabilities(18, 70);

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, WINDOWS);

        assertTrue(advice.isRainRisk());
        assertEquals(70, advice.getMaxProbability());
    }

    @Test
    public void windowEndIsExclusive() {
        // 09:00-10:00 slot is outside a 07:00-09:00 window.
        Forecast forecast = TestForecasts.dayWithProbabilities(9, 100);

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, WINDOWS);

        assertEquals(CommuteAdvice.Recommendation.BUS, advice.getRecommendation());
    }

    @Test
    public void partialHourOverlapCounts() {
        List<TimeWindow> windows = Collections.singletonList(
                new TimeWindow(LocalTime.of(7, 30), LocalTime.of(8, 15)));
        Forecast forecast = TestForecasts.dayWithProbabilities(8, 50);

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, windows);

        assertEquals(CommuteAdvice.Recommendation.CAR, advice.getRecommendation());
    }

    @Test
    public void wetConditionWithPrecipitationCountsEvenBelowThreshold() {
        HourlyForecast drizzle = new HourlyForecast(TestForecasts.DAY.atTime(8, 0), 9, 10, 0.4,
                WeatherCondition.DRIZZLE, 3);
        Forecast forecast = new Forecast(9, 7, WeatherCondition.DRIZZLE, 3,
                Collections.singletonList(drizzle), LocalDateTime.of(TestForecasts.DAY, LocalTime.of(6, 0)));

        CommuteAdvice advice = new CommuteAdvisor(30).advise(forecast, TestForecasts.DAY, WINDOWS);

        assertEquals(CommuteAdvice.Recommendation.CAR, advice.getRecommendation());
    }

    @Test
    public void unknownWhenNoDataForDay() {
        Forecast forecast = TestForecasts.dayWithProbabilities();

        CommuteAdvice advice = new CommuteAdvisor(30)
                .advise(forecast, TestForecasts.DAY.plusDays(5), WINDOWS);

        assertEquals(CommuteAdvice.Recommendation.UNKNOWN, advice.getRecommendation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidThreshold() {
        new CommuteAdvisor(101);
    }
}

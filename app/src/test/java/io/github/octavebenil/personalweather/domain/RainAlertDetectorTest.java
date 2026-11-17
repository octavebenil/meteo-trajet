package io.github.octavebenil.personalweather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RainAlertDetectorTest {

    @Test
    public void findsFirstRainyHourWithinLookAhead() {
        Forecast forecast = TestForecasts.dayWithProbabilities(13, 40, 14, 80);

        HourlyForecast hour = new RainAlertDetector(30, 3)
                .nextRainyHour(forecast, TestForecasts.DAY.atTime(11, 20));

        assertEquals(13, hour.getTime().getHour());
    }

    @Test
    public void ignoresRainBeyondLookAhead() {
        Forecast forecast = TestForecasts.dayWithProbabilities(16, 90);

        assertNull(new RainAlertDetector(30, 3).nextRainyHour(forecast, TestForecasts.DAY.atTime(11, 0)));
    }

    @Test
    public void includesCurrentHour() {
        Forecast forecast = TestForecasts.dayWithProbabilities(11, 60);

        HourlyForecast hour = new RainAlertDetector(30, 3)
                .nextRainyHour(forecast, TestForecasts.DAY.atTime(11, 45));

        assertEquals(11, hour.getTime().getHour());
    }
}

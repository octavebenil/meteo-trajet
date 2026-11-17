package io.github.octavebenil.personalweather.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.github.octavebenil.personalweather.domain.Forecast;
import io.github.octavebenil.personalweather.domain.WeatherCondition;

import org.junit.Test;

import java.time.LocalDateTime;

public class ForecastParserTest {

    private static final String SAMPLE = "{"
            + "\"current\":{\"time\":\"2026-11-17T07:15\",\"temperature_2m\":8.4,"
            + "\"apparent_temperature\":5.9,\"weather_code\":61,\"wind_speed_10m\":14.2},"
            + "\"hourly\":{"
            + "\"time\":[\"2026-11-17T07:00\",\"2026-11-17T08:00\"],"
            + "\"temperature_2m\":[8.1,8.9],"
            + "\"precipitation_probability\":[45,null],"
            + "\"precipitation\":[0.6,0.0],"
            + "\"weather_code\":[61,3],"
            + "\"wind_speed_10m\":[13.0,15.5]}}";

    @Test
    public void parsesCurrentAndHourly() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 11, 17, 7, 15);

        Forecast forecast = ForecastParser.parse(SAMPLE, now);

        assertEquals(8.4, forecast.getCurrentTemperatureC(), 0.001);
        assertEquals(5.9, forecast.getApparentTemperatureC(), 0.001);
        assertEquals(WeatherCondition.RAIN, forecast.getCurrentCondition());
        assertEquals(2, forecast.getHourly().size());
        assertEquals(45, forecast.getHourly().get(0).getPrecipitationProbability());
        assertEquals(0.6, forecast.getHourly().get(0).getPrecipitationMm(), 0.001);
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), forecast.getHourly().get(1).getTime());
        assertEquals(now, forecast.getFetchedAt());
    }

    @Test
    public void nullProbabilityBecomesZero() throws Exception {
        Forecast forecast = ForecastParser.parse(SAMPLE, LocalDateTime.now());

        assertEquals(0, forecast.getHourly().get(1).getPrecipitationProbability());
    }

    @Test(expected = ForecastParser.ParseException.class)
    public void rejectsInvalidJson() throws Exception {
        ForecastParser.parse("{\"error\":true}", LocalDateTime.now());
    }

    @Test
    public void urlContainsCoordinatesAndFields() {
        String url = OpenMeteoClient.buildUrl(48.8566, 2.3522);

        assertTrue(url.contains("latitude=48.8566"));
        assertTrue(url.contains("longitude=2.3522"));
        assertTrue(url.contains("precipitation_probability"));
        assertTrue(url.contains("timezone=auto"));
    }
}

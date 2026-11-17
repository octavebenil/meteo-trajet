package io.github.octavebenil.personalweather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeatherConditionTest {

    @Test
    public void mapsWmoCodes() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0));
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(3));
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(63));
        assertEquals(WeatherCondition.SHOWERS, WeatherCondition.fromWmoCode(81));
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95));
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmoCode(-1));
    }

    @Test
    public void wetFlag() {
        assertTrue(WeatherCondition.RAIN.isWet());
        assertTrue(WeatherCondition.SNOW.isWet());
        assertFalse(WeatherCondition.FOG.isWet());
        assertFalse(WeatherCondition.CLEAR.isWet());
    }
}

package io.github.octavebenil.personalweather.domain;

/**
 * Simplified weather condition derived from WMO weather interpretation codes
 * (the codes returned by the Open-Meteo API).
 */
public enum WeatherCondition {
    CLEAR(false),
    PARTLY_CLOUDY(false),
    CLOUDY(false),
    FOG(false),
    DRIZZLE(true),
    RAIN(true),
    FREEZING_RAIN(true),
    SNOW(true),
    SHOWERS(true),
    THUNDERSTORM(true),
    UNKNOWN(false);

    private final boolean wet;

    WeatherCondition(boolean wet) {
        this.wet = wet;
    }

    /** @return true when this condition means precipitation is falling. */
    public boolean isWet() {
        return wet;
    }

    /** Maps a WMO weather code to a {@link WeatherCondition}. */
    public static WeatherCondition fromWmoCode(int code) {
        switch (code) {
            case 0:
                return CLEAR;
            case 1:
            case 2:
                return PARTLY_CLOUDY;
            case 3:
                return CLOUDY;
            case 45:
            case 48:
                return FOG;
            case 51:
            case 53:
            case 55:
                return DRIZZLE;
            case 56:
            case 57:
            case 66:
            case 67:
                return FREEZING_RAIN;
            case 61:
            case 63:
            case 65:
                return RAIN;
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return SNOW;
            case 80:
            case 81:
            case 82:
                return SHOWERS;
            case 95:
            case 96:
            case 99:
                return THUNDERSTORM;
            default:
                return UNKNOWN;
        }
    }
}

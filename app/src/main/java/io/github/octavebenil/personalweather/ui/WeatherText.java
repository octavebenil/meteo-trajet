package io.github.octavebenil.personalweather.ui;

import android.content.Context;

import io.github.octavebenil.personalweather.R;
import io.github.octavebenil.personalweather.domain.CommuteAdvice;
import io.github.octavebenil.personalweather.domain.HourlyForecast;
import io.github.octavebenil.personalweather.domain.WeatherCondition;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Turns domain objects into user-facing text. */
public final class WeatherText {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private WeatherText() {
    }

    public static String emoji(WeatherCondition condition) {
        switch (condition) {
            case CLEAR:
                return "☀️";
            case PARTLY_CLOUDY:
                return "⛅";
            case CLOUDY:
                return "☁️";
            case FOG:
                return "🌫️";
            case DRIZZLE:
                return "🌦️";
            case RAIN:
            case FREEZING_RAIN:
            case SHOWERS:
                return "🌧️";
            case SNOW:
                return "❄️";
            case THUNDERSTORM:
                return "⛈️";
            default:
                return "🌡️";
        }
    }

    public static String describe(Context context, WeatherCondition condition) {
        switch (condition) {
            case CLEAR:
                return context.getString(R.string.cond_clear);
            case PARTLY_CLOUDY:
                return context.getString(R.string.cond_partly_cloudy);
            case CLOUDY:
                return context.getString(R.string.cond_cloudy);
            case FOG:
                return context.getString(R.string.cond_fog);
            case DRIZZLE:
                return context.getString(R.string.cond_drizzle);
            case RAIN:
                return context.getString(R.string.cond_rain);
            case FREEZING_RAIN:
                return context.getString(R.string.cond_freezing_rain);
            case SNOW:
                return context.getString(R.string.cond_snow);
            case SHOWERS:
                return context.getString(R.string.cond_showers);
            case THUNDERSTORM:
                return context.getString(R.string.cond_thunderstorm);
            default:
                return context.getString(R.string.cond_unknown);
        }
    }

    public static String temperature(double celsius) {
        return String.format(Locale.getDefault(), "%.0f°", celsius);
    }

    public static String hour(HourlyForecast hour) {
        return hour.getTime().format(HOUR);
    }

    public static String adviceTitle(Context context, CommuteAdvice advice) {
        switch (advice.getRecommendation()) {
            case CAR:
                return context.getString(R.string.advice_car);
            case BUS:
                return context.getString(R.string.advice_bus);
            default:
                return context.getString(R.string.advice_unknown);
        }
    }

    public static String adviceDetail(Context context, CommuteAdvice advice) {
        switch (advice.getRecommendation()) {
            case CAR:
                HourlyForecast first = advice.getRainyHours().get(0);
                return context.getString(R.string.advice_car_detail,
                        hour(first), first.getPrecipitationProbability(),
                        advice.getMaxProbability(), advice.getTotalPrecipitationMm());
            case BUS:
                return context.getString(R.string.advice_bus_detail, advice.getMaxProbability());
            default:
                return context.getString(R.string.advice_unknown_detail);
        }
    }

    public static String rainAlertText(Context context, HourlyForecast hour) {
        return context.getString(R.string.rain_alert_text, hour(hour),
                hour.getPrecipitationProbability(), hour.getPrecipitationMm());
    }
}

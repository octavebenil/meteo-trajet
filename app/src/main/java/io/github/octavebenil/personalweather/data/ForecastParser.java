package io.github.octavebenil.personalweather.data;

import io.github.octavebenil.personalweather.domain.Forecast;
import io.github.octavebenil.personalweather.domain.HourlyForecast;
import io.github.octavebenil.personalweather.domain.WeatherCondition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Converts an Open-Meteo JSON response into a {@link Forecast}. */
public final class ForecastParser {

    private ForecastParser() {
    }

    public static Forecast parse(String json, LocalDateTime fetchedAt) throws ParseException {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject current = root.getJSONObject("current");
            JSONObject hourly = root.getJSONObject("hourly");

            JSONArray times = hourly.getJSONArray("time");
            JSONArray temperatures = hourly.getJSONArray("temperature_2m");
            JSONArray probabilities = hourly.getJSONArray("precipitation_probability");
            JSONArray precipitation = hourly.getJSONArray("precipitation");
            JSONArray codes = hourly.getJSONArray("weather_code");
            JSONArray wind = hourly.optJSONArray("wind_speed_10m");

            List<HourlyForecast> hours = new ArrayList<>(times.length());
            for (int i = 0; i < times.length(); i++) {
                hours.add(new HourlyForecast(
                        LocalDateTime.parse(times.getString(i)),
                        temperatures.optDouble(i, 0),
                        probabilities.optInt(i, 0),
                        precipitation.optDouble(i, 0),
                        WeatherCondition.fromWmoCode(codes.optInt(i, -1)),
                        wind == null ? 0 : wind.optDouble(i, 0)));
            }

            return new Forecast(
                    current.optDouble("temperature_2m", 0),
                    current.optDouble("apparent_temperature", current.optDouble("temperature_2m", 0)),
                    WeatherCondition.fromWmoCode(current.optInt("weather_code", -1)),
                    current.optDouble("wind_speed_10m", 0),
                    hours,
                    fetchedAt);
        } catch (JSONException | DateTimeParseException e) {
            throw new ParseException("Invalid forecast response", e);
        }
    }

    /** Thrown when the response cannot be read. */
    public static final class ParseException extends Exception {
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package io.github.octavebenil.personalweather.data;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.octavebenil.personalweather.domain.Forecast;

import java.time.LocalDateTime;

/** Keeps the last successful API response so the app can show data offline. */
public final class ForecastCache {

    private static final String PREFS = "forecast_cache";
    private static final String KEY_JSON = "json";
    private static final String KEY_FETCHED_AT = "fetched_at";

    private final SharedPreferences prefs;

    public ForecastCache(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String json, LocalDateTime fetchedAt) {
        prefs.edit().putString(KEY_JSON, json).putString(KEY_FETCHED_AT, fetchedAt.toString()).apply();
    }

    /** @return the cached forecast, or {@code null} when nothing valid is cached. */
    public Forecast load() {
        String json = prefs.getString(KEY_JSON, null);
        String fetchedAt = prefs.getString(KEY_FETCHED_AT, null);
        if (json == null || fetchedAt == null) {
            return null;
        }
        try {
            return ForecastParser.parse(json, LocalDateTime.parse(fetchedAt));
        } catch (ForecastParser.ParseException | RuntimeException e) {
            return null;
        }
    }
}

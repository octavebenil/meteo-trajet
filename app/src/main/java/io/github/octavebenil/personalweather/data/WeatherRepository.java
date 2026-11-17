package io.github.octavebenil.personalweather.data;

import android.content.Context;
import android.location.Location;

import io.github.octavebenil.personalweather.domain.Forecast;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Single entry point to obtain a forecast: refreshes the location when possible,
 * calls the API, and falls back to the cache. Blocking: call from a background thread.
 */
public final class WeatherRepository {

    private final SettingsStore settings;
    private final ForecastCache cache;
    private final LocationProvider locationProvider;
    private final OpenMeteoClient client;

    public WeatherRepository(Context context) {
        this.settings = new SettingsStore(context);
        this.cache = new ForecastCache(context);
        this.locationProvider = new LocationProvider(context);
        this.client = new OpenMeteoClient();
    }

    /** Result of a refresh: a forecast and whether it comes from the network. */
    public static final class Result {
        public final Forecast forecast;
        public final SavedLocation location;
        public final boolean fresh;
        public final String error;

        Result(Forecast forecast, SavedLocation location, boolean fresh, String error) {
            this.forecast = forecast;
            this.location = location;
            this.fresh = fresh;
            this.error = error;
        }
    }

    /**
     * @param updateLocation whether to ask the system for a new location
     *                       (only possible while the app is in the foreground).
     */
    public Result refresh(boolean updateLocation) {
        SavedLocation location = settings.getLocation();
        if (updateLocation) {
            Location fix = locationProvider.getLocation();
            if (fix != null) {
                String name = locationProvider.resolvePlaceName(fix.getLatitude(), fix.getLongitude());
                if (name == null && location != null) {
                    name = location.getPlaceName();
                }
                location = new SavedLocation(fix.getLatitude(), fix.getLongitude(), name);
                settings.setLocation(location);
            }
        }
        if (location == null) {
            return new Result(cache.load(), null, false, "no_location");
        }
        try {
            String json = client.fetchForecastJson(location.getLatitude(), location.getLongitude());
            LocalDateTime now = LocalDateTime.now();
            Forecast forecast = ForecastParser.parse(json, now);
            cache.save(json, now);
            return new Result(forecast, location, true, null);
        } catch (IOException | ForecastParser.ParseException e) {
            return new Result(cache.load(), location, false, "network");
        }
    }

    public Forecast cached() {
        return cache.load();
    }
}

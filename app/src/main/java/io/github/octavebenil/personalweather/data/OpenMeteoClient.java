package io.github.octavebenil.personalweather.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Minimal HTTP client for the free Open-Meteo forecast API (no API key needed). */
public final class OpenMeteoClient {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final int TIMEOUT_MS = 15_000;

    /** Builds the request URL. Package-private for tests. */
    static String buildUrl(double latitude, double longitude) {
        return String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m"
                        + "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m"
                        + "&timezone=auto&forecast_days=2",
                BASE_URL, latitude, longitude);
    }

    /** Performs a blocking request. Must not be called on the main thread. */
    public String fetchForecastJson(double latitude, double longitude) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrl(latitude, longitude)).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP status " + status);
            }
            try (InputStream in = connection.getInputStream()) {
                return readAll(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}

package io.github.octavebenil.personalweather.data;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Finds the device location with the framework {@link LocationManager}
 * (no Google Play Services needed). Blocking: call from a background thread.
 */
public final class LocationProvider {

    private static final long TIMEOUT_SECONDS = 20;

    private final Context context;

    public LocationProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** @return a fresh or recent location, or {@code null} if unavailable. */
    @SuppressWarnings("MissingPermission")
    public Location getLocation() {
        if (!hasPermission()) {
            return null;
        }
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        String provider = pickProvider(manager);
        if (provider != null) {
            Location current = requestCurrent(manager, provider);
            if (current != null) {
                return current;
            }
        }
        return bestLastKnown(manager);
    }

    @SuppressWarnings("MissingPermission")
    private Location requestCurrent(LocationManager manager, String provider) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Location> result = new AtomicReference<>();
        CancellationSignal cancel = new CancellationSignal();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, cancel,
                    executor, location -> {
                        result.set(location);
                        latch.countDown();
                    });
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                cancel.cancel();
            }
        } catch (InterruptedException e) {
            cancel.cancel();
            Thread.currentThread().interrupt();
        } catch (SecurityException | IllegalArgumentException e) {
            return null;
        } finally {
            executor.shutdown();
        }
        return result.get();
    }

    @SuppressWarnings("MissingPermission")
    private static Location bestLastKnown(LocationManager manager) {
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            } catch (SecurityException ignored) {
                // Permission may have been revoked for this provider.
            }
        }
        return best;
    }

    private static String pickProvider(LocationManager manager) {
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        return null;
    }

    /** Best-effort reverse geocoding. Returns {@code null} when no name can be found. */
    @SuppressWarnings("deprecation")
    public String resolvePlaceName(double latitude, double longitude) {
        if (!Geocoder.isPresent()) {
            return null;
        }
        try {
            List<Address> addresses = new Geocoder(context, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }
            Address address = addresses.get(0);
            if (address.getLocality() != null) {
                return address.getLocality();
            }
            return address.getSubAdminArea() != null ? address.getSubAdminArea() : address.getAdminArea();
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }
}

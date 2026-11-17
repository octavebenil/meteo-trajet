package io.github.octavebenil.personalweather.data;

/** Last known coordinates of the user, with an optional place name. Immutable. */
public final class SavedLocation {

    private final double latitude;
    private final double longitude;
    private final String placeName;

    public SavedLocation(double latitude, double longitude, String placeName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeName = placeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** @return the place name, or {@code null} when unknown. */
    public String getPlaceName() {
        return placeName;
    }
}

package io.github.octavebenil.personalweather.data;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.octavebenil.personalweather.domain.TimeWindow;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/** User settings and last known location, stored in {@link SharedPreferences}. */
public final class SettingsStore {

    public static final int DEFAULT_THRESHOLD = 30;

    private static final String PREFS = "settings";
    private static final String KEY_ALARM_ENABLED = "alarm_enabled";
    private static final String KEY_ALARM_MINUTES = "alarm_minutes";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_SOUND_ONLY_RAIN = "sound_only_rain";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_OUT_START = "out_start";
    private static final String KEY_OUT_END = "out_end";
    private static final String KEY_BACK_START = "back_start";
    private static final String KEY_BACK_END = "back_end";
    private static final String KEY_DAY_ALERTS = "day_alerts";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_PLACE = "place";
    private static final String KEY_LAST_ALERT_HOUR = "last_alert_hour";

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isAlarmEnabled() {
        return prefs.getBoolean(KEY_ALARM_ENABLED, false);
    }

    public void setAlarmEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, enabled).apply();
    }

    public LocalTime getAlarmTime() {
        return minutesToTime(prefs.getInt(KEY_ALARM_MINUTES, 6 * 60 + 30));
    }

    public void setAlarmTime(LocalTime time) {
        prefs.edit().putInt(KEY_ALARM_MINUTES, timeToMinutes(time)).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, false);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isSoundOnlyWhenRain() {
        return prefs.getBoolean(KEY_SOUND_ONLY_RAIN, true);
    }

    public void setSoundOnlyWhenRain(boolean onlyWhenRain) {
        prefs.edit().putBoolean(KEY_SOUND_ONLY_RAIN, onlyWhenRain).apply();
    }

    /** @return the chosen ringtone URI, or {@code null} for the default alarm sound. */
    public String getRingtoneUri() {
        return prefs.getString(KEY_RINGTONE_URI, null);
    }

    public void setRingtoneUri(String uri) {
        prefs.edit().putString(KEY_RINGTONE_URI, uri).apply();
    }

    public int getThreshold() {
        return prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD);
    }

    public void setThreshold(int threshold) {
        prefs.edit().putInt(KEY_THRESHOLD, threshold).apply();
    }

    public TimeWindow getOutboundWindow() {
        return safeWindow(prefs.getInt(KEY_OUT_START, 7 * 60), prefs.getInt(KEY_OUT_END, 9 * 60));
    }

    public void setOutboundWindow(LocalTime start, LocalTime end) {
        prefs.edit().putInt(KEY_OUT_START, timeToMinutes(start)).putInt(KEY_OUT_END, timeToMinutes(end)).apply();
    }

    public TimeWindow getReturnWindow() {
        return safeWindow(prefs.getInt(KEY_BACK_START, 17 * 60), prefs.getInt(KEY_BACK_END, 19 * 60));
    }

    public void setReturnWindow(LocalTime start, LocalTime end) {
        prefs.edit().putInt(KEY_BACK_START, timeToMinutes(start)).putInt(KEY_BACK_END, timeToMinutes(end)).apply();
    }

    public List<TimeWindow> getCommuteWindows() {
        return Arrays.asList(getOutboundWindow(), getReturnWindow());
    }

    public boolean isDayAlertsEnabled() {
        return prefs.getBoolean(KEY_DAY_ALERTS, true);
    }

    public void setDayAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DAY_ALERTS, enabled).apply();
    }

    /** @return the last saved location, or {@code null} if none was ever saved. */
    public SavedLocation getLocation() {
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LON)) {
            return null;
        }
        return new SavedLocation(
                Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0)),
                Double.longBitsToDouble(prefs.getLong(KEY_LON, 0)),
                prefs.getString(KEY_PLACE, null));
    }

    public void setLocation(SavedLocation location) {
        prefs.edit()
                .putLong(KEY_LAT, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(KEY_LON, Double.doubleToRawLongBits(location.getLongitude()))
                .putString(KEY_PLACE, location.getPlaceName())
                .apply();
    }

    /** Key of the last rainy hour a day alert was sent for, to avoid duplicate alerts. */
    public String getLastAlertHour() {
        return prefs.getString(KEY_LAST_ALERT_HOUR, null);
    }

    public void setLastAlertHour(String hourKey) {
        prefs.edit().putString(KEY_LAST_ALERT_HOUR, hourKey).apply();
    }

    static int timeToMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    static LocalTime minutesToTime(int minutes) {
        int clamped = Math.max(0, Math.min(23 * 60 + 59, minutes));
        return LocalTime.of(clamped / 60, clamped % 60);
    }

    private static TimeWindow safeWindow(int startMinutes, int endMinutes) {
        LocalTime start = minutesToTime(startMinutes);
        LocalTime end = minutesToTime(endMinutes);
        return end.isBefore(start) ? new TimeWindow(end, start) : new TimeWindow(start, end);
    }
}

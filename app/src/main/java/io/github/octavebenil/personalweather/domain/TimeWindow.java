package io.github.octavebenil.personalweather.domain;

import java.time.LocalTime;
import java.util.Objects;

/** A time range within a day, e.g. a commute from 07:00 to 09:00. Immutable. */
public final class TimeWindow {

    private final LocalTime start;
    private final LocalTime end;

    public TimeWindow(LocalTime start, LocalTime end) {
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    /**
     * Tells whether the hourly slot [hourStart, hourStart + 1h) overlaps this window
     * [start, end). A zero-length window matches the hour that contains it.
     */
    public boolean overlapsHourStartingAt(LocalTime hourStart) {
        LocalTime hourEnd = hourStart.getHour() == 23 ? LocalTime.MAX : hourStart.plusHours(1);
        if (start.equals(end)) {
            return !start.isBefore(hourStart) && start.isBefore(hourEnd);
        }
        return hourStart.isBefore(end) && hourEnd.isAfter(start);
    }
}

package io.github.octavebenil.personalweather.scheduling;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AlarmSchedulerTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    public void sameDayWhenAlarmIsLater() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 17, 5, 0, 0, 0, PARIS);

        ZonedDateTime next = AlarmScheduler.nextTrigger(now, LocalTime.of(6, 30));

        assertEquals(ZonedDateTime.of(2026, 11, 17, 6, 30, 0, 0, PARIS), next);
    }

    @Test
    public void nextDayWhenAlarmHasPassed() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 17, 7, 0, 0, 0, PARIS);

        ZonedDateTime next = AlarmScheduler.nextTrigger(now, LocalTime.of(6, 30));

        assertEquals(ZonedDateTime.of(2026, 11, 18, 6, 30, 0, 0, PARIS), next);
    }

    @Test
    public void nextDayWhenExactlyNow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 17, 6, 30, 0, 0, PARIS);

        ZonedDateTime next = AlarmScheduler.nextTrigger(now, LocalTime.of(6, 30));

        assertEquals(18, next.getDayOfMonth());
    }

    @Test
    public void keepsLocalTimeAcrossDstChange() {
        // Night of 28 to 29 March 2026: clocks go forward in Paris.
        ZonedDateTime now = ZonedDateTime.of(2026, 3, 28, 22, 0, 0, 0, PARIS);

        ZonedDateTime next = AlarmScheduler.nextTrigger(now, LocalTime.of(6, 30));

        assertEquals(LocalTime.of(6, 30), next.toLocalTime());
        assertEquals(29, next.getDayOfMonth());
    }
}

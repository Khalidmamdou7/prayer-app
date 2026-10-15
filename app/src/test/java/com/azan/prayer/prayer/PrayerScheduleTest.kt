package com.azan.prayer.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PrayerScheduleTest {
    private val zone = ZoneId.of("Africa/Cairo")
    private val calc = PrayerCalculator(30.0444, 31.2357, CalculationMethod.EGYPT, AsrMethod.STANDARD, zone)
    private val schedule = PrayerSchedule(calc::timesFor)
    private val day = LocalDate.of(2026, 10, 15)
    private val times = calc.timesFor(day)

    @Test
    fun reminderIsTwentyMinutesBeforeTheNextAdhan() {
        val now = times[Prayer.DHUHR].plusMinutes(5)
        val event = schedule.nextReminder(Prayer.DHUHR, 20, now)
        assertEquals(times[Prayer.ASR].minusMinutes(20), event.time)
        assertEquals(day, event.prayerDate)
    }

    @Test
    fun fajrReminderIsBeforeSunrise() {
        val now = day.atStartOfDay(zone).plusHours(1)
        val event = schedule.nextReminder(Prayer.FAJR, 20, now)
        assertEquals(times[Prayer.SUNRISE].minusMinutes(20), event.time)
    }

    @Test
    fun ishaReminderBelongsToPreviousDayAfterMidnight() {
        val now = day.atStartOfDay(zone).plusHours(1)
        val event = schedule.nextReminder(Prayer.ISHA, 20, now)
        assertEquals(day.minusDays(1), event.prayerDate)
        assertEquals(times[Prayer.FAJR].minusMinutes(20), event.time)
    }

    @Test
    fun reminderMovesToTomorrowOncePassed() {
        val now = times[Prayer.ASR].minusMinutes(10)
        val event = schedule.nextReminder(Prayer.DHUHR, 20, now)
        assertEquals(day.plusDays(1), event.prayerDate)
    }

    @Test
    fun reminderNeverBeforeItsOwnAdhan() {
        // Maghrib to Isha is ~78 minutes in Cairo, so a 90 minute reminder is clamped to Maghrib.
        val now = times[Prayer.ASR]
        val event = schedule.nextReminder(Prayer.MAGHRIB, 90, now)
        assertEquals(times[Prayer.MAGHRIB], event.time)
    }

    @Test
    fun nextAdhanRollsOverToTomorrow() {
        val now = times[Prayer.ISHA].plusMinutes(1)
        val event = schedule.nextAdhan(Prayer.FAJR, now)
        assertEquals(day.plusDays(1), event.prayerDate)
        val sameDay = schedule.nextAdhan(Prayer.ISHA, times[Prayer.ISHA].minusMinutes(1))
        assertEquals(times[Prayer.ISHA], sameDay.time)
    }

    @Test
    fun currentPrayer() {
        assertEquals(Prayer.ASR to day, schedule.current(times[Prayer.ASR].plusMinutes(1)))
        assertNull(schedule.current(times[Prayer.SUNRISE].plusMinutes(1)))
        assertEquals(Prayer.ISHA to day.minusDays(1), schedule.current(day.atStartOfDay(zone).plusHours(2)))
    }

    @Test
    fun nextIncludesSunriseAndRollsOver() {
        assertEquals(Prayer.SUNRISE, schedule.next(times[Prayer.FAJR].plusMinutes(1)).first)
        val (prayer, time) = schedule.next(times[Prayer.ISHA].plusMinutes(1))
        assertEquals(Prayer.FAJR, prayer)
        assertEquals(day.plusDays(1), time.toLocalDate())
    }

    @Test
    fun formatsRemaining() {
        assertEquals("01:02:03", PrayerSchedule.formatRemaining(java.time.Duration.ofSeconds(3723), withSeconds = true))
        assertEquals("1h 2m", PrayerSchedule.formatRemaining(java.time.Duration.ofSeconds(3723), withSeconds = false))
        assertEquals("5m", PrayerSchedule.formatRemaining(java.time.Duration.ofMinutes(5), withSeconds = false))
    }
}

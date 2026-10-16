package com.azan.prayer.sync

import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.prayer.PrayerCalculator
import com.azan.prayer.prayer.PrayerTimesSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

class SyncTest {
    private val zone = ZoneId.of("Africa/Cairo")
    private val calculator = PrayerCalculator(30.0444, 31.2357, CalculationMethod.EGYPT, AsrMethod.STANDARD, zone)
    private val day = LocalDate.of(2026, 10, 15)

    private fun sample(): String =
        javaClass.classLoader!!.getResource("aladhan_cairo_2026_10.json")!!.readText()

    @Test
    fun parsesCalendar() {
        val days = AladhanApi.parseCalendar(sample())
        assertEquals(setOf(day, day.plusDays(1), day.plusDays(2)), days.keys)
        assertEquals(OffsetDateTime.parse("2026-10-15T05:30:00+03:00").toInstant(), days.getValue(day)[Prayer.FAJR])
        assertEquals(OffsetDateTime.parse("2026-10-15T19:42:00+03:00").toInstant(), days.getValue(day)[Prayer.ISHA])
        assertEquals(Prayer.entries.toSet(), days.getValue(day).keys)
    }

    @Test
    fun buildsUrl() {
        val url = AladhanApi.monthUrl(30.0444, 31.2357, CalculationMethod.EGYPT, AsrMethod.HANAFI, 2026, 10)
        assertEquals(
            "https://api.aladhan.com/v1/calendar/2026/10?latitude=30.04&longitude=31.24&method=5&school=1&iso8601=true",
            url,
        )
    }

    @Test
    fun syncedTimesAgreeWithCalculation() {
        val days = AladhanApi.parseCalendar(sample())
        assertEquals(days, PrayerTimesSource.validate(calculator, days))
    }

    @Test
    fun rejectsImplausibleDays() {
        val days = AladhanApi.parseCalendar(sample()).toMutableMap()
        val bad = days.getValue(day).toMutableMap()
        bad[Prayer.ASR] = bad.getValue(Prayer.ASR).plusSeconds(3 * 3600) // wrong time zone, say
        days[day] = bad
        days[day.plusDays(1)] = days.getValue(day.plusDays(1)) - Prayer.ISHA // incomplete
        assertEquals(setOf(day.plusDays(2)), PrayerTimesSource.validate(calculator, days).keys)
    }

    @Test
    fun prefersSyncedTimesAndFallsBackToCalculation() {
        val synced = mapOf(day to Prayer.entries.associateWith { Instant.parse("2026-10-15T00:00:00Z").plusSeconds(it.ordinal * 3600L) })
        val source = PrayerTimesSource(calculator, synced, emptyMap(), zone)
        assertTrue(source.isSynced(day))
        assertEquals(LocalTime.of(5, 0), source.timesFor(day)[Prayer.DHUHR].toLocalTime()) // 02:00Z = 05:00 Cairo
        assertFalse(source.isSynced(day.plusDays(1)))
        assertEquals(calculator.timesFor(day.plusDays(1)), source.timesFor(day.plusDays(1)))
    }

    @Test
    fun appliesAdjustments() {
        val source = PrayerTimesSource(calculator, emptyMap(), mapOf(Prayer.FAJR to 2, Prayer.ISHA to -5), zone)
        val base = calculator.timesFor(day)
        val adjusted = source.timesFor(day)
        assertEquals(base[Prayer.FAJR].plusMinutes(2), adjusted[Prayer.FAJR])
        assertEquals(base[Prayer.ISHA].minusMinutes(5), adjusted[Prayer.ISHA])
        assertEquals(base[Prayer.DHUHR], adjusted[Prayer.DHUHR])
    }

    @Test
    fun storeFormatRoundTrips() {
        val original = SyncedTimes("k", Instant.ofEpochSecond(1_800_000_000), AladhanApi.parseCalendar(sample()), "boom")
        assertEquals(original, SyncedTimes.fromJson(SyncedTimes.toJson(original)))
    }
}

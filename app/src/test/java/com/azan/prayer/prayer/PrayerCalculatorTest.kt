package com.azan.prayer.prayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reference times come from the widely used `adhan` library (v4.4.3). It adds small
 * per-method Dhuhr offsets, so a two-minute tolerance is allowed.
 */
class PrayerCalculatorTest {

    private data class Case(
        val lat: Double,
        val lng: Double,
        val zone: String,
        val method: CalculationMethod,
        val asr: AsrMethod,
        val date: String,
        val expected: String,
    )

    private val cases = listOf(
        Case(30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPT, AsrMethod.STANDARD, "2026-10-15", "05:30 06:57 12:42 15:57 18:24 19:42"),
        Case(30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPT, AsrMethod.STANDARD, "2026-06-21", "04:08 05:54 12:58 16:32 19:59 21:33"),
        Case(21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA, AsrMethod.STANDARD, "2026-03-01", "05:25 06:41 12:33 15:55 18:25 19:55"),
        Case(40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA, AsrMethod.HANAFI, "2026-01-15", "05:58 07:18 12:07 15:13 16:53 18:14"),
        Case(51.5074, -0.1278, "Europe/London", CalculationMethod.MWL, AsrMethod.STANDARD, "2026-12-21", "05:59 08:04 12:00 13:38 15:53 17:51"),
        Case(24.8607, 67.0011, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI, "2026-08-05", "04:39 06:02 12:39 17:17 19:14 20:37"),
        Case(-6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.SINGAPORE, AsrMethod.STANDARD, "2026-04-10", "04:36 05:54 11:55 15:12 17:54 19:03"),
    )

    @Test
    fun matchesReferenceTimetables() {
        for (case in cases) {
            val zone = ZoneId.of(case.zone)
            val date = LocalDate.parse(case.date)
            val times = PrayerCalculator(case.lat, case.lng, case.method, case.asr, zone).timesFor(date)
            Prayer.entries.zip(case.expected.split(" ")).forEach { (prayer, expected) ->
                val actual = times[prayer]
                assertTrue("${case.zone} ${case.date} $prayer date", actual.toLocalDate() == date)
                val diff = Duration.between(LocalTime.parse(expected), actual.toLocalTime()).toMinutes()
                assertTrue(
                    "${case.zone} ${case.date} $prayer: expected $expected, got ${actual.toLocalTime()}",
                    kotlin.math.abs(diff) <= 2,
                )
            }
        }
    }

    @Test
    fun timesAreInOrder() {
        val calc = PrayerCalculator(30.0444, 31.2357, CalculationMethod.EGYPT, AsrMethod.STANDARD, ZoneId.of("Africa/Cairo"))
        var date = LocalDate.of(2026, 1, 1)
        repeat(365) {
            val t = calc.timesFor(date)
            Prayer.entries.zipWithNext().forEach { (a, b) -> assertTrue("$date $a < $b", t[a].isBefore(t[b])) }
            date = date.plusDays(1)
        }
    }

    @Test
    fun highLatitudeSummerStillProducesTimes() {
        // In Oslo around the solstice the sun never reaches 18° below the horizon.
        val calc = PrayerCalculator(59.91, 10.75, CalculationMethod.MWL, AsrMethod.STANDARD, ZoneId.of("Europe/Oslo"))
        val t = calc.timesFor(LocalDate.of(2026, 6, 21))
        Prayer.entries.zipWithNext().forEach { (a, b) -> assertTrue("$a < $b", t[a].isBefore(t[b])) }
    }
}

package com.azan.prayer.prayer

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

data class PrayerTimes(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    operator fun get(prayer: Prayer): ZonedDateTime = times.getValue(prayer)
}

/**
 * Offline astronomical prayer time calculation (the classic PrayTimes algorithm),
 * including the angle-based adjustment for high latitudes.
 */
class PrayerCalculator(
    private val latitude: Double,
    private val longitude: Double,
    private val method: CalculationMethod,
    private val asrMethod: AsrMethod,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun timesFor(date: LocalDate): PrayerTimes {
        val jDate = julianDate(date) - longitude / (15.0 * 24.0)

        // Two passes: the first uses rough guesses for the sun position, the second refines it.
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0, 18.0)
        repeat(2) { t = computeTimes(jDate, t) }

        val sunrise = t[1]
        val sunset = t[4]
        val night = fixHour(sunrise - sunset)
        val fajr = adjustHighLatitude(t[0], sunrise, method.fajrAngle, night, ccw = true)
        val maghrib = if (method.maghribAngle != null) {
            adjustHighLatitude(t[5], sunset, method.maghribAngle, night, ccw = false)
        } else {
            sunset
        }
        val isha = if (method.ishaAngle != null) {
            adjustHighLatitude(t[6], sunset, method.ishaAngle, night, ccw = false)
        } else {
            maghrib + method.ishaMinutes / 60.0
        }

        // Convert from hours in UTC (relative to the local meridian) to local clock times.
        val hoursUtc = mapOf(
            Prayer.FAJR to fajr,
            Prayer.SUNRISE to sunrise,
            Prayer.DHUHR to t[2],
            Prayer.ASR to t[3],
            Prayer.MAGHRIB to maghrib,
            Prayer.ISHA to isha,
        ).mapValues { (_, h) -> h - longitude / 15.0 }

        val midnightUtc = date.atStartOfDay(ZoneId.of("UTC"))
        val times = hoursUtc.mapValues { (_, h) ->
            val seconds = (h * 3600.0).roundToLong()
            // Round to the nearest minute, like published timetables.
            val minutes = Math.floorDiv(seconds + 30, 60)
            midnightUtc.plusMinutes(minutes).withZoneSameInstant(zone)
        }
        return PrayerTimes(date, times)
    }

    private fun computeTimes(jDate: Double, guess: DoubleArray): DoubleArray {
        val g = DoubleArray(guess.size) { guess[it] / 24.0 }
        val riseSetAngle = 0.833
        return doubleArrayOf(
            sunAngleTime(jDate, method.fajrAngle, g[0], ccw = true),
            sunAngleTime(jDate, riseSetAngle, g[1], ccw = true),
            midDay(jDate, g[2]),
            asrTime(jDate, asrMethod.shadowFactor, g[3]),
            sunAngleTime(jDate, riseSetAngle, g[4], ccw = false),
            sunAngleTime(jDate, method.maghribAngle ?: riseSetAngle, g[5], ccw = false),
            sunAngleTime(jDate, method.ishaAngle ?: 18.0, g[6], ccw = false),
        )
    }

    private fun adjustHighLatitude(time: Double, base: Double, angle: Double, night: Double, ccw: Boolean): Double {
        val portion = angle / 60.0 * night
        val diff = if (ccw) fixHour(base - time) else fixHour(time - base)
        return if (time.isNaN() || diff > portion) base + if (ccw) -portion else portion else time
    }

    private fun midDay(jDate: Double, t: Double): Double {
        val eqt = sunPosition(jDate + t).equation
        return fixHour(12.0 - eqt)
    }

    private fun sunAngleTime(jDate: Double, angle: Double, t: Double, ccw: Boolean): Double {
        val decl = sunPosition(jDate + t).declination
        val noon = midDay(jDate, t)
        val cosT = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
        val time = darccos(cosT) / 15.0
        return noon + if (ccw) -time else time
    }

    private fun asrTime(jDate: Double, factor: Int, t: Double): Double {
        val decl = sunPosition(jDate + t).declination
        val angle = -darccot(factor + dtan(abs(latitude - decl)))
        return sunAngleTime(jDate, angle, t, ccw = false)
    }

    private data class SunPosition(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0)
        return SunPosition(
            declination = darcsin(dsin(e) * dsin(l)),
            equation = q / 15.0 - ra,
        )
    }

    private fun julianDate(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.dayOfMonth + b - 1524.5
    }

    private companion object {
        fun dsin(d: Double) = sin(Math.toRadians(d))
        fun dcos(d: Double) = cos(Math.toRadians(d))
        fun dtan(d: Double) = tan(Math.toRadians(d))
        fun darcsin(x: Double) = Math.toDegrees(asin(x))
        fun darccos(x: Double) = Math.toDegrees(acos(x))
        fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
        fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
        fun fixAngle(a: Double) = fix(a, 360.0)
        fun fixHour(a: Double) = fix(a, 24.0)
        fun fix(a: Double, b: Double): Double {
            val r = a - b * floor(a / b)
            return if (r < 0) r + b else r
        }
    }
}

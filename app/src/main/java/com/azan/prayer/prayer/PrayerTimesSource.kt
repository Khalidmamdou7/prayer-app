package com.azan.prayer.prayer

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Picks the times for a day: synced (online) times when available for that day, otherwise the
 * on-device calculation. User minute adjustments are applied on top of either.
 */
class PrayerTimesSource(
    private val calculator: PrayerCalculator,
    private val synced: Map<LocalDate, Map<Prayer, Instant>>,
    private val adjustments: Map<Prayer, Int>,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun isSynced(date: LocalDate): Boolean = synced[date]?.keys?.containsAll(Prayer.entries) == true

    fun timesFor(date: LocalDate): PrayerTimes {
        val day = synced[date]
        val base = if (day != null && isSynced(date)) {
            PrayerTimes(date, Prayer.entries.associateWith { day.getValue(it).atZone(zone) })
        } else {
            calculator.timesFor(date)
        }
        if (adjustments.values.all { it == 0 }) return base
        return PrayerTimes(date, base.times.mapValues { (prayer, time) -> time.plusMinutes((adjustments[prayer] ?: 0).toLong()) })
    }

    companion object {
        /** Synced days that disagree with the calculation by more than this are treated as bad data. */
        val MAX_DEVIATION: Duration = Duration.ofMinutes(60)

        /** Keeps only complete days whose times are plausible compared with [calculator]. */
        fun validate(
            calculator: PrayerCalculator,
            days: Map<LocalDate, Map<Prayer, Instant>>,
        ): Map<LocalDate, Map<Prayer, Instant>> = days.filter { (date, times) ->
            val calculated = calculator.timesFor(date)
            Prayer.entries.all { prayer ->
                val t = times[prayer] ?: return@all false
                Duration.between(calculated[prayer].toInstant(), t).abs() <= MAX_DEVIATION
            } && Prayer.entries.zipWithNext().all { (a, b) -> times.getValue(a).isBefore(times.getValue(b)) }
        }
    }
}

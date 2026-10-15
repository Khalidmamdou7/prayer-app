package com.azan.prayer.prayer

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

enum class EventType { ADHAN, REMINDER }

/**
 * A future alarm.
 *
 * For [EventType.REMINDER], [prayer] is the prayer whose time is about to end (the one the user
 * is asked about) and [prayerDate] is the day that prayer belongs to.
 */
data class AlarmEvent(
    val type: EventType,
    val prayer: Prayer,
    val prayerDate: LocalDate,
    val time: ZonedDateTime,
)

/** Pure scheduling rules, independent of Android so they can be unit tested. */
class PrayerSchedule(private val timesFor: (LocalDate) -> PrayerTimes) {

    /** Time at which [prayer]'s window ends: sunrise for Fajr, the next prayer otherwise. */
    fun endOf(prayer: Prayer, date: LocalDate): ZonedDateTime = when (prayer) {
        Prayer.FAJR -> timesFor(date)[Prayer.SUNRISE]
        Prayer.DHUHR -> timesFor(date)[Prayer.ASR]
        Prayer.ASR -> timesFor(date)[Prayer.MAGHRIB]
        Prayer.MAGHRIB -> timesFor(date)[Prayer.ISHA]
        Prayer.ISHA -> timesFor(date.plusDays(1))[Prayer.FAJR]
        Prayer.SUNRISE -> error("Sunrise is not a prayer")
    }

    /** Next adhan for [prayer] strictly after [now]. */
    fun nextAdhan(prayer: Prayer, now: ZonedDateTime): AlarmEvent {
        val today = now.toLocalDate()
        return (0L..2L).asSequence()
            .map { today.plusDays(it) }
            .map { AlarmEvent(EventType.ADHAN, prayer, it, timesFor(it)[prayer]) }
            .first { it.time.isAfter(now) }
    }

    /**
     * Next "have you prayed?" reminder for [prayer] strictly after [now]: [minutesBefore] minutes
     * before its window ends, but never before the prayer's own adhan.
     */
    fun nextReminder(prayer: Prayer, minutesBefore: Int, now: ZonedDateTime): AlarmEvent {
        val today = now.toLocalDate()
        return (-1L..2L).asSequence()
            .map { today.plusDays(it) }
            .map { date ->
                val start = timesFor(date)[prayer]
                val remindAt = endOf(prayer, date).minusMinutes(minutesBefore.toLong())
                AlarmEvent(EventType.REMINDER, prayer, date, maxOf(remindAt, start))
            }
            .first { it.time.isAfter(now) }
    }

    /** The prayer whose time is currently running, with the date it belongs to, or null (sunrise to Dhuhr). */
    fun current(now: ZonedDateTime): Pair<Prayer, LocalDate>? {
        val today = now.toLocalDate()
        for (date in listOf(today, today.minusDays(1))) {
            for (prayer in Prayer.prayers.reversed()) {
                val start = timesFor(date)[prayer]
                if (!now.isBefore(start) && now.isBefore(endOf(prayer, date))) return prayer to date
            }
        }
        return null
    }

    /** The next time point (including sunrise) after [now]. */
    fun next(now: ZonedDateTime): Pair<Prayer, ZonedDateTime> {
        val today = now.toLocalDate()
        for (date in listOf(today, today.plusDays(1))) {
            val times = timesFor(date)
            Prayer.entries.firstOrNull { times[it].isAfter(now) }?.let { return it to times[it] }
        }
        error("No upcoming prayer found")
    }

    companion object {
        fun formatRemaining(duration: Duration, withSeconds: Boolean): String {
            val total = duration.seconds.coerceAtLeast(0)
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (withSeconds) "%02d:%02d:%02d".format(h, m, s)
            else if (h > 0) "${h}h ${m}m" else "${m}m"
        }
    }
}

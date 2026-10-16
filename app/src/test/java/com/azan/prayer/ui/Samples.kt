package com.azan.prayer.ui

import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import java.time.Duration
import java.time.LocalDate

/** Fixed data for screenshots: Cairo, mid-afternoon on 15 October 2026. */
object Samples {
    val today = LocalDate.of(2026, 10, 15)

    fun row(p: Prayer, time: String, phase: Phase, remaining: Duration? = null, prayed: Boolean? = null, mode: AlertMode = AlertMode.NOTIFICATION) =
        PrayerRowState(p, time, phase, remaining, if (p.isPrayer) mode else null, prayed, today)

    val state = HomeState(
        locationName = "Cairo, Egypt",
        gregorianDate = "Thursday, October 15, 2026",
        hijriDate = hijriDate(today, java.util.Locale.US)?.let { "$it AH" },
        next = Prayer.ASR,
        nextTime = "15:58",
        remaining = Duration.ofSeconds(1 * 3600 + 23 * 60 + 41),
        previous = Prayer.DHUHR,
        previousTime = "12:42",
        progress = 0.58f,
        rows = listOf(
            row(Prayer.FAJR, "05:30", Phase.PASSED, prayed = true, mode = AlertMode.ADHAN),
            row(Prayer.SUNRISE, "06:57", Phase.PASSED),
            row(Prayer.DHUHR, "12:42", Phase.CURRENT, Duration.ofMinutes(83), prayed = false),
            row(Prayer.ASR, "15:58", Phase.NEXT, Duration.ofMinutes(83)),
            row(Prayer.MAGHRIB, "18:24", Phase.UPCOMING, Duration.ofMinutes(229), mode = AlertMode.ADHAN),
            row(Prayer.ISHA, "19:42", Phase.UPCOMING, Duration.ofMinutes(307), mode = AlertMode.OFF),
        ),
        syncedAgo = "2 hours ago",
        notificationsOff = false,
        exactAlarmsOff = false,
    )

    val settings = AppSettings(
        latitude = 30.0444, longitude = 31.2357, locationName = "Cairo, Egypt",
        method = CalculationMethod.EGYPT, asrMethod = AsrMethod.STANDARD,
        alertModes = Prayer.prayers.associateWith { AlertMode.NOTIFICATION } + (Prayer.FAJR to AlertMode.ADHAN),
        reminderEnabled = true, reminderMinutes = 20, adhanUri = null, adhanName = null,
        syncEnabled = true, adjustments = Prayer.entries.associateWith { 0 },
    )

    /** The same moment as [state], formatted the way the app does for Arabic. */
    fun arabicState(): HomeState {
        val ar = java.util.Locale("ar", "EG")
        val clock = clockFormatter(true, ar)
        fun t(h: Int, m: Int) = java.time.LocalTime.of(h, m).format(clock)
        val times = listOf(t(5, 30), t(6, 57), t(12, 42), t(15, 58), t(18, 24), t(19, 42))
        return state.copy(
            locationName = "القاهرة، مصر",
            gregorianDate = gregorianDate(today, ar),
            hijriDate = hijriDate(today, ar)?.let { "$it هـ" },
            nextTime = times[3],
            previousTime = times[2],
            rows = state.rows.mapIndexed { i, r -> r.copy(time = times[i]) },
            syncedAgo = "قبل ساعتين",
        )
    }
}

package com.azan.prayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.azan.prayer.R
import java.time.Duration
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.util.Locale

/** The app's current language (follows the in-app language choice, not just the system's). */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

/** "01:23:41", with the locale's digits (e.g. ٠١:٢٣:٤١ in Arabic). */
fun countdown(d: Duration, locale: Locale): String {
    val total = d.seconds.coerceAtLeast(0)
    return String.format(locale, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
}

/** "1h 23m" / "23m", translated. */
@Composable
@ReadOnlyComposable
fun shortDuration(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = (total / 3600).toInt()
    val m = ((total % 3600) / 60).toInt()
    return if (h > 0) stringResource(R.string.duration_hm, h, m) else stringResource(R.string.duration_m, m)
}

fun clockFormatter(is24h: Boolean, locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", locale).withDecimalStyle(DecimalStyle.of(locale))

fun gregorianDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).withDecimalStyle(DecimalStyle.of(locale)))

/** "4 Jumada I 1448", or null if the date is outside the Hijri calendar's range. */
fun hijriDate(date: LocalDate, locale: Locale): String? = runCatching {
    HijrahDate.from(date).format(
        DateTimeFormatter.ofPattern("d MMMM y", locale)
            .withChronology(HijrahChronology.INSTANCE)
            .withDecimalStyle(DecimalStyle.of(locale)),
    )
}.getOrNull()

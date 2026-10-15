package com.azan.prayer.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.ui.theme.AzanTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.LocalDate

/**
 * Renders the main screens to PNGs in app/screenshots/ for design review.
 * Run with: ./gradlew testDebugUnitTest -Pscreenshots=true --tests '*ScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhenAsked() = Assume.assumeTrue(System.getProperty("screenshots") == "true")

    private val today = LocalDate.of(2026, 10, 15)

    private fun row(p: Prayer, time: String, phase: Phase, remaining: Duration? = null, prayed: Boolean? = null, mode: AlertMode = AlertMode.NOTIFICATION) =
        PrayerRowState(p, time, phase, remaining, if (p.isPrayer) mode else null, prayed, today)

    private val state = HomeState(
        locationName = "Cairo, Egypt",
        gregorianDate = "Thursday, October 15, 2026",
        hijriDate = hijri(today),
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

    private val settings = AppSettings(
        latitude = 30.0444, longitude = 31.2357, locationName = "Cairo, Egypt",
        method = CalculationMethod.EGYPT, asrMethod = AsrMethod.STANDARD,
        alertModes = Prayer.prayers.associateWith { AlertMode.NOTIFICATION } + (Prayer.FAJR to AlertMode.ADHAN),
        reminderEnabled = true, reminderMinutes = 20, adhanUri = null, adhanName = null,
        syncEnabled = true, adjustments = Prayer.entries.associateWith { 0 },
    )

    private fun home(dark: Boolean, s: HomeState = state) = compose.setContent {
        AzanTheme(darkTheme = dark) { HomeContent(s, {}, { AlertMode.OFF }, { _, _ -> }, {}, {}) }
    }

    @Test
    fun homeLight() {
        home(dark = false)
        compose.onRoot().captureRoboImage("../docs/screenshots/home_light.png")
    }

    @Test
    fun homeDark() {
        home(dark = true)
        compose.onRoot().captureRoboImage("../docs/screenshots/home_dark.png")
    }

    @Test
    fun homeWithWarnings() {
        home(dark = false, state.copy(notificationsOff = true, exactAlarmsOff = true, syncedAgo = null))
        compose.onRoot().captureRoboImage("../docs/screenshots/home_warnings.png")
    }

    @Test
    fun welcome() {
        compose.setContent { AzanTheme(darkTheme = false) { WelcomeScreen(settings.copy(latitude = null, longitude = null), {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/welcome.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h2600dp-xxhdpi")
    fun settingsLight() {
        compose.setContent { AzanTheme(darkTheme = false) { SettingsScreen(settings, {}, {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/settings.png")
    }
}

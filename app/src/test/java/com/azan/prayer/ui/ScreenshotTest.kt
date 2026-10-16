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

    private val state = Samples.state
    private val settings = Samples.settings
    private fun arabicState() = Samples.arabicState()

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
    @Config(qualifiers = "ar-rEG-ldrtl-w400dp-h900dp-xxhdpi")
    fun homeArabic() {
        home(dark = false, arabicState())
        compose.onRoot().captureRoboImage("../docs/screenshots/home_ar.png")
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl-w400dp-h900dp-xxhdpi")
    fun homeArabicDark() {
        home(dark = true, arabicState())
        compose.onRoot().captureRoboImage("../docs/screenshots/home_ar_dark.png")
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl-w400dp-h900dp-xxhdpi")
    fun welcomeArabic() {
        compose.setContent { AzanTheme(darkTheme = false) { WelcomeScreen(settings.copy(latitude = null, longitude = null), {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/welcome_ar.png")
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl-w400dp-h2700dp-xxhdpi")
    fun settingsArabic() {
        compose.setContent { AzanTheme(darkTheme = false) { SettingsScreen(settings.copy(locationName = "القاهرة، مصر"), {}, {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/settings_ar.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun setup() {
        compose.setContent { AzanTheme(darkTheme = false) { SetupScreen(settings.copy(onboarded = false, alertModes = Prayer.prayers.associateWith { AlertMode.NOTIFICATION }), {}, {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/setup.png")
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl-w400dp-h1500dp-xxhdpi")
    fun setupArabic() {
        compose.setContent { AzanTheme(darkTheme = true) { SetupScreen(settings.copy(onboarded = false, alertModes = Prayer.prayers.associateWith { AlertMode.NOTIFICATION }), {}, {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/setup_ar_dark.png")
    }

    @Test
    fun homeFirstVisit() {
        home(dark = false, state.copy(showTip = true))
        compose.onRoot().captureRoboImage("../docs/screenshots/home_tip.png")
    }

    @Test
    fun welcomeDark() {
        compose.setContent { AzanTheme(darkTheme = true) { WelcomeScreen(settings.copy(latitude = null, longitude = null), {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/welcome_dark.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h2700dp-xxhdpi")
    fun settingsLight() {
        compose.setContent { AzanTheme(darkTheme = false) { SettingsScreen(settings, {}, {}) } }
        compose.onRoot().captureRoboImage("../docs/screenshots/settings.png")
    }
}

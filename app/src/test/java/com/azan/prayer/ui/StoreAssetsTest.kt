package com.azan.prayer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.azan.prayer.R
import com.azan.prayer.data.AlertMode
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.ui.components.PrayerGlyph
import com.azan.prayer.ui.components.StarPattern
import com.azan.prayer.ui.theme.AzanTheme
import com.azan.prayer.ui.theme.LocalHeroColors
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Google Play listing graphics, written to fastlane/metadata/android/<locale>/images.
 * Run with: ./gradlew testDebugUnitTest -Pscreenshots=true --tests '*StoreAssetsTest*'
 *
 * Sizes follow Play's rules: 512×512 icon, 1024×500 feature graphic, 1080×1920 phone screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
class StoreAssetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhenAsked() = Assume.assumeTrue(System.getProperty("screenshots") == "true")

    private fun out(locale: String, name: String) = "../fastlane/metadata/android/$locale/images/$name"

    private fun capture(path: String, content: @Composable () -> Unit) {
        compose.setContent { AzanTheme(darkTheme = false) { content() } }
        compose.onRoot().captureRoboImage(path)
    }

    // --- Icon -------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun icon() = capture(out("en-US", "icon.png")) {
        val hero = LocalHeroColors.current
        Box(Modifier.fillMaxSize().background(hero.gradient)) {
            // The launcher foreground leaves room for adaptive-icon masks; the store icon can be bolder.
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().scale(1.35f))
        }
    }

    // --- Feature graphic -------------------------------------------------------------------

    @Composable
    private fun FeatureGraphic() {
        val hero = LocalHeroColors.current
        Box(Modifier.fillMaxSize().background(hero.gradient)) {
            StarPattern(hero.pattern.copy(alpha = 0.08f), Modifier.fillMaxSize(), cell = 72.dp)
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrayerGlyph(Prayer.ISHA, hero.accent, Modifier.size(132.dp))
                Spacer(Modifier.width(48.dp))
                Column {
                    Text(stringResource(R.string.app_name), color = hero.content, fontSize = 88.sp, style = MaterialTheme.typography.displayLarge)
                    Text(
                        stringResource(R.string.welcome_tagline),
                        color = hero.muted,
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() = capture(out("en-US", "featureGraphic.png")) { FeatureGraphic() }

    @Test
    @Config(qualifiers = "ar-ldrtl-w1024dp-h500dp-mdpi")
    fun featureGraphicArabic() = capture(out("ar", "featureGraphic.png")) { FeatureGraphic() }

    // --- Phone screenshots (1080×1920) -----------------------------------------------------

    private fun home(dark: Boolean, state: HomeState) {
        compose.setContent {
            AzanTheme(darkTheme = dark) { HomeContent(state, {}, { AlertMode.OFF }, { _, _ -> }, {}, {}) }
        }
    }

    private fun welcome(locale: String) = capture(out(locale, "phoneScreenshots/3_setup.png")) {
        SetupScreen(Samples.settings.copy(onboarded = false, alertModes = Prayer.prayers.associateWith { AlertMode.NOTIFICATION }), {}, {})
    }

    private fun settings(locale: String, name: String) = capture(out(locale, "phoneScreenshots/4_settings.png")) {
        SettingsScreen(Samples.settings.copy(locationName = name), {}, {})
    }

    @Test
    @Config(qualifiers = EN)
    fun en1Home() {
        home(dark = false, Samples.state)
        compose.onRoot().captureRoboImage(out("en-US", "phoneScreenshots/1_home.png"))
    }

    @Test
    @Config(qualifiers = EN)
    fun en2HomeDark() {
        home(dark = true, Samples.state)
        compose.onRoot().captureRoboImage(out("en-US", "phoneScreenshots/2_home_dark.png"))
    }

    @Test
    @Config(qualifiers = EN)
    fun en3Welcome() = welcome("en-US")

    @Test
    @Config(qualifiers = EN)
    fun en4Settings() = settings("en-US", "Cairo, Egypt")

    @Test
    @Config(qualifiers = AR)
    fun ar1Home() {
        home(dark = false, Samples.arabicState())
        compose.onRoot().captureRoboImage(out("ar", "phoneScreenshots/1_home.png"))
    }

    @Test
    @Config(qualifiers = AR)
    fun ar2HomeDark() {
        home(dark = true, Samples.arabicState())
        compose.onRoot().captureRoboImage(out("ar", "phoneScreenshots/2_home_dark.png"))
    }

    @Test
    @Config(qualifiers = AR)
    fun ar3Welcome() = welcome("ar")

    @Test
    @Config(qualifiers = AR)
    fun ar4Settings() = settings("ar", "القاهرة، مصر")

    private companion object {
        const val EN = "w360dp-h640dp-xxhdpi"
        const val AR = "ar-rEG-ldrtl-w360dp-h640dp-xxhdpi"
    }
}

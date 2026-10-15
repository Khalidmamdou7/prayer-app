package com.azan.prayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5B4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5ECE5),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF8C6A2B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E7C9),
    onSecondaryContainer = Color(0xFF2B1F00),
    tertiary = Color(0xFF3D6373),
    background = Color(0xFFF6F4EE),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFF6F4EE),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE6E3DA),
    onSurfaceVariant = Color(0xFF5B5F5B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFBF7),
    surfaceContainer = Color(0xFFF0EEE7),
    surfaceContainerHigh = Color(0xFFEAE7DF),
    surfaceContainerHighest = Color(0xFFE4E1D8),
    outline = Color(0xFFB4B6B0),
    outlineVariant = Color(0xFFE0DED6),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED3C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF124B42),
    onPrimaryContainer = Color(0xFFC6EEE3),
    secondary = Color(0xFFE2C27F),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF4A3C1B),
    onSecondaryContainer = Color(0xFFF4E7C9),
    tertiary = Color(0xFFA6CCDD),
    background = Color(0xFF0C1311),
    onBackground = Color(0xFFE1E5E2),
    surface = Color(0xFF0C1311),
    onSurface = Color(0xFFE1E5E2),
    surfaceVariant = Color(0xFF26302D),
    onSurfaceVariant = Color(0xFFA8B1AD),
    surfaceContainerLowest = Color(0xFF070D0C),
    surfaceContainerLow = Color(0xFF121B19),
    surfaceContainer = Color(0xFF16201E),
    surfaceContainerHigh = Color(0xFF1C2724),
    surfaceContainerHighest = Color(0xFF232E2B),
    outline = Color(0xFF6C7672),
    outlineVariant = Color(0xFF29332F),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF5C1D19),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Colours that are not part of Material's scheme: the hero header. */
@Immutable
data class HeroColors(val gradient: Brush, val content: Color, val muted: Color, val accent: Color, val pattern: Color)

private val LightHero = HeroColors(
    gradient = Brush.verticalGradient(listOf(Color(0xFF0E5A4D), Color(0xFF0A3F36))),
    content = Color.White,
    muted = Color.White.copy(alpha = 0.72f),
    accent = Color(0xFFE6C886),
    pattern = Color.White.copy(alpha = 0.06f),
)

private val DarkHero = HeroColors(
    gradient = Brush.verticalGradient(listOf(Color(0xFF0F3E36), Color(0xFF0A1F1B))),
    content = Color(0xFFF1F4F2),
    muted = Color(0xFFF1F4F2).copy(alpha = 0.66f),
    accent = Color(0xFFE2C27F),
    pattern = Color.White.copy(alpha = 0.045f),
)

val LocalHeroColors = staticCompositionLocalOf { LightHero }

private val base = Typography()

private val AppTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

/** Digits that don't jiggle as a countdown ticks. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AzanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHeroColors provides if (darkTheme) DarkHero else LightHero) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

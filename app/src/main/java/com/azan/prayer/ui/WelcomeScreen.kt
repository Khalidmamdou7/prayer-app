package com.azan.prayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.ui.components.PrayerGlyph
import com.azan.prayer.ui.components.StarPattern
import com.azan.prayer.ui.theme.LocalHeroColors

/** First run: a short introduction and the one thing the app needs, your location. */
@Composable
fun WelcomeScreen(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val hero = LocalHeroColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(hero.gradient),
        ) {
            StarPattern(hero.pattern, Modifier.matchParentSize())
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, top = 56.dp, bottom = 76.dp),
            ) {
                PrayerGlyph(Prayer.ISHA, hero.accent, Modifier.size(44.dp))
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.welcome_title), color = hero.content, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.welcome_tagline), color = hero.muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Column(
            Modifier
                .offset(y = (-28).dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp),
        ) {
            Text(stringResource(R.string.welcome_where), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.welcome_where_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            LocationPicker(settings, onChange)
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

package com.azan.prayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.ui.components.StarPattern
import com.azan.prayer.ui.theme.LocalHeroColors

/**
 * Second (last) first-run step: how to be alerted, the "have you prayed?" reminder,
 * and where everything can be changed later.
 */
@Composable
fun SetupScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onDone: () -> Unit) {
    // One choice for all prayers here; each prayer can be changed individually later.
    val allMode = settings.alertModes.values.toSet().singleOrNull()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OnboardingHeader(
                step = 2,
                title = stringResource(R.string.setup_title),
                subtitle = stringResource(R.string.setup_subtitle),
            )
            Column(
                Modifier
                    .offset(y = (-28).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Section(stringResource(R.string.setup_at_time)) {
                    ModeChoice(
                        selected = allMode == AlertMode.ADHAN,
                        title = stringResource(R.string.mode_adhan),
                        description = stringResource(R.string.setup_mode_adhan),
                    ) { onChange(settings.withAllModes(AlertMode.ADHAN)) }
                    ModeChoice(
                        selected = allMode == AlertMode.NOTIFICATION,
                        title = stringResource(R.string.mode_notification),
                        description = stringResource(R.string.setup_mode_notification),
                    ) { onChange(settings.withAllModes(AlertMode.NOTIFICATION)) }
                    ModeChoice(
                        selected = allMode == AlertMode.OFF,
                        title = stringResource(R.string.mode_off),
                        description = stringResource(R.string.setup_mode_off),
                    ) { onChange(settings.withAllModes(AlertMode.OFF)) }
                }

                Section(stringResource(R.string.section_reminder)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.reminder_switch),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Switch(settings.reminderEnabled, { onChange(settings.copy(reminderEnabled = it)) })
                    }
                    Text(
                        pluralStringResource(R.plurals.reminder_desc, settings.reminderMinutes, settings.reminderMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (settings.reminderEnabled) {
                        val options = listOf(10, 15, 20, 30)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            options.forEachIndexed { i, minutes ->
                                SegmentedButton(
                                    selected = settings.reminderMinutes == minutes,
                                    onClick = { onChange(settings.copy(reminderMinutes = minutes)) },
                                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                                    icon = {},
                                ) { Text(stringResource(R.string.duration_m, minutes)) }
                            }
                        }
                    }
                }

                Section(stringResource(R.string.setup_tips_title)) {
                    Tip(painterResource(R.drawable.ic_bell), stringResource(R.string.tip_bell))
                    Tip(null, stringResource(R.string.tip_tick), ring = true)
                    Tip(rememberVectorPainter(Icons.Outlined.Settings), stringResource(R.string.tip_settings))
                }

                Button(
                    onClick = {
                        onChange(settings.copy(onboarded = true))
                        onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text(stringResource(R.string.setup_done), style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

private fun AppSettings.withAllModes(mode: AlertMode) = copy(alertModes = Prayer.prayers.associateWith { mode })

@Composable
private fun ModeChoice(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainerLowest)
            .border(1.dp, if (selected) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tip(icon: Painter?, text: String, ring: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            } else if (ring) {
                Box(
                    Modifier
                        .size(18.dp)
                        .border(1.6.dp, scheme.primary, CircleShape),
                )
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

/** The green header shared by both first-run steps. */
@Composable
fun OnboardingHeader(step: Int, title: String, subtitle: String, emblem: @Composable (() -> Unit)? = null) {
    val hero = LocalHeroColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(hero.gradient),
    ) {
        StarPattern(hero.pattern, Modifier.matchParentSize())
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 68.dp),
        ) {
            Text(
                stringResource(R.string.step_of, step, 2),
                color = hero.accent,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(16.dp))
            if (emblem != null) {
                emblem()
                Spacer(Modifier.height(16.dp))
            }
            Text(title, color = hero.content, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = hero.muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

package com.azan.prayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.prayer.PrayerSchedule
import com.azan.prayer.sync.SyncedTimesStore
import com.azan.prayer.ui.components.PrayedToggle
import com.azan.prayer.ui.components.PrayerGlyph
import com.azan.prayer.ui.components.StarPattern
import com.azan.prayer.ui.theme.LocalHeroColors
import com.azan.prayer.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class Phase { PASSED, CURRENT, NEXT, UPCOMING }

data class PrayerRowState(
    val prayer: Prayer,
    val time: String,
    val phase: Phase,
    /** Until the prayer starts (NEXT/UPCOMING) or until it ends (CURRENT). */
    val remaining: Duration?,
    val alertMode: AlertMode?,
    /** Null when it can't be ticked yet (future, or sunrise). */
    val prayed: Boolean?,
    val prayedDate: LocalDate,
)

data class HomeState(
    val locationName: String,
    val gregorianDate: String,
    val hijriDate: String?,
    val next: Prayer,
    val nextTime: String,
    val remaining: Duration,
    val previous: Prayer,
    val previousTime: String,
    val progress: Float,
    val rows: List<PrayerRowState>,
    /** "2 hours ago" when today's times came from the online sync, null when calculated on the phone. */
    val syncedAgo: String?,
    val notificationsOff: Boolean,
    val exactAlarmsOff: Boolean,
) {
    val prayedCount get() = rows.count { it.prayer.isPrayer && it.prayed == true }
}

@Composable
fun HomeScreen(
    settings: AppSettings,
    store: SettingsStore,
    refreshKey: Int,
    onChange: (AppSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    var prayedVersion by remember { mutableIntStateOf(0) }
    val syncVersion by SyncedTimesStore.version.collectAsState()
    val synced = remember(syncVersion, refreshKey) { SyncedTimesStore(context).load() }
    val source = remember(settings, synced) { settings.timesSource(synced) } ?: return
    val schedule = remember(source) { PrayerSchedule(source::timesFor) }
    val is24h = DateFormat.is24HourFormat(context)
    val formatter = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }
    val notificationsOff = remember(refreshKey) { !Notifications.canPost(context) }
    val exactOff = remember(refreshKey) { !AlarmScheduler.canScheduleExact(context) }

    val today = now.toLocalDate()
    val times = remember(source, today) { source.timesFor(today) }
    val (next, nextTime) = schedule.next(now)
    val (previous, previousTime) = schedule.previous(now)
    val current = schedule.current(now)
    val prayedFlags = remember(refreshKey, prayedVersion, today, current) {
        Prayer.prayers.associateWith { p ->
            val date = current?.takeIf { it.first == p }?.second ?: today
            store.isPrayed(p, date)
        }
    }

    val rows = Prayer.entries.map { prayer ->
        val time = times[prayer]
        val active = current?.takeIf { it.first == prayer }
        val phase = when {
            active != null -> Phase.CURRENT
            prayer == next && nextTime.toLocalDate() == today -> Phase.NEXT
            time.isAfter(now) -> Phase.UPCOMING
            else -> Phase.PASSED
        }
        PrayerRowState(
            prayer = prayer,
            time = time.format(formatter),
            phase = phase,
            remaining = when (phase) {
                Phase.CURRENT -> Duration.between(now, schedule.endOf(prayer, active!!.second))
                Phase.NEXT, Phase.UPCOMING -> Duration.between(now, time)
                Phase.PASSED -> null
            },
            alertMode = if (prayer.isPrayer) settings.alertMode(prayer) else null,
            prayed = if (prayer.isPrayer && phase in setOf(Phase.CURRENT, Phase.PASSED)) prayedFlags[prayer] == true else null,
            prayedDate = active?.second ?: today,
        )
    }

    val span = Duration.between(previousTime, nextTime).seconds.coerceAtLeast(1)
    val state = HomeState(
        locationName = settings.locationName.ifBlank { "%.3f, %.3f".format(settings.latitude, settings.longitude) },
        gregorianDate = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
        hijriDate = hijri(today),
        next = next,
        nextTime = nextTime.format(formatter),
        remaining = Duration.between(now, nextTime),
        previous = previous,
        previousTime = previousTime.format(formatter),
        progress = (Duration.between(previousTime, now).seconds.toFloat() / span).coerceIn(0f, 1f),
        rows = rows,
        syncedAgo = synced.syncedAt?.takeIf { source.isSynced(today) }
            ?.let { DateUtils.getRelativeTimeSpanString(it.toEpochMilli()).toString() },
        notificationsOff = notificationsOff,
        exactAlarmsOff = exactOff && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )

    HomeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onCycleAlert = { prayer ->
            val mode = AlertMode.entries[(settings.alertMode(prayer).ordinal + 1) % AlertMode.entries.size]
            onChange(settings.copy(alertModes = settings.alertModes + (prayer to mode)))
            mode
        },
        onPrayedChange = { row, value ->
            store.setPrayed(row.prayer, row.prayedDate, value)
            prayedVersion++
        },
        onFixNotifications = onRequestNotifications,
        onFixExactAlarms = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                )
            }
        },
    )
}

fun hijri(date: LocalDate): String? = runCatching {
    HijrahDate.from(date).format(DateTimeFormatter.ofPattern("d MMMM y").withChronology(HijrahChronology.INSTANCE)) + " AH"
}.getOrNull()

@Composable
fun HomeContent(
    state: HomeState,
    onOpenSettings: () -> Unit,
    onCycleAlert: (Prayer) -> AlertMode,
    onPrayedChange: (PrayerRowState, Boolean) -> Unit,
    onFixNotifications: () -> Unit,
    onFixExactAlarms: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val modeLabels = mapOf(
        AlertMode.OFF to stringResource(R.string.alert_set_off),
        AlertMode.NOTIFICATION to stringResource(R.string.alert_set_notification),
        AlertMode.ADHAN to stringResource(R.string.alert_set_adhan),
    )
    val prayerNames = Prayer.entries.associateWith { stringResource(it.label) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Hero(state, onOpenSettings)
            Column(
                Modifier
                    .offset(y = (-28).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.notificationsOff) {
                    Notice(stringResource(R.string.perm_notifications), stringResource(R.string.allow), onFixNotifications)
                }
                if (state.exactAlarmsOff) {
                    Notice(stringResource(R.string.perm_exact), stringResource(R.string.allow), onFixExactAlarms)
                }
                TodayHeader(state)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        state.rows.forEachIndexed { i, row ->
                            val nearCurrent = row.phase == Phase.CURRENT || state.rows.getOrNull(i - 1)?.phase == Phase.CURRENT
                            if (i > 0 && !nearCurrent) {
                                HorizontalDivider(
                                    Modifier.padding(start = 70.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                            PrayerRow(
                                row,
                                onCycleAlert = {
                                    val mode = onCycleAlert(row.prayer)
                                    scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        snackbar.showSnackbar(modeLabels.getValue(mode).format(prayerNames.getValue(row.prayer)))
                                    }
                                },
                                onPrayedChange = { onPrayedChange(row, it) },
                            )
                        }
                    }
                }
                SourceFooter(state.syncedAgo)
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun Hero(state: HomeState, onOpenSettings: () -> Unit) {
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
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 52.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSettings)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = hero.accent, modifier = Modifier.size(18.dp))
                    Text(
                        state.locationName,
                        color = hero.content,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = hero.content)
                }
            }
            Text(
                listOfNotNull(state.gregorianDate, state.hijriDate).joinToString("  ·  "),
                color = hero.muted,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(28.dp))
            AnimatedContent(state.next, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "next") { next ->
                Text(
                    stringResource(R.string.next_prayer_in, stringResource(next.label)),
                    color = hero.accent,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                PrayerSchedule.formatRemaining(state.remaining, withSeconds = true),
                color = hero.content,
                style = MaterialTheme.typography.displayLarge.tabular(),
            )
            Text(
                stringResource(R.string.at_time, state.nextTime),
                color = hero.muted,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(end = 12.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(hero.content.copy(alpha = 0.16f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(state.progress)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(hero.accent),
                    )
                }
                Row(Modifier.padding(top = 8.dp)) {
                    Text(
                        "${stringResource(state.previous.label)}  ${state.previousTime}",
                        color = hero.muted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${stringResource(state.next.label)}  ${state.nextTime}",
                        color = hero.muted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(state: HomeState) {
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.today), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        val total = Prayer.prayers.size
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < state.prayedCount) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
            Text(
                stringResource(R.string.prayed_count, state.prayedCount, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun PrayerRow(row: PrayerRowState, onCycleAlert: () -> Unit, onPrayedChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isCurrent = row.phase == Phase.CURRENT
    val isNext = row.phase == Phase.NEXT
    val emphasized = isCurrent || isNext
    val dim = !emphasized && (row.phase == Phase.PASSED || !row.prayer.isPrayer)
    val background = if (isCurrent) scheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (emphasized) scheme.primary.copy(alpha = 0.12f) else scheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            PrayerGlyph(row.prayer, if (emphasized) scheme.primary else scheme.onSurfaceVariant, Modifier.size(22.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(row.prayer.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (dim) scheme.onSurfaceVariant else scheme.onSurface,
                )
                Text(
                    stringResource(row.prayer.arabicLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 1.dp),
                )
            }
            val status = when (row.phase) {
                Phase.CURRENT -> stringResource(R.string.status_now, PrayerSchedule.formatRemaining(row.remaining!!, false))
                Phase.NEXT, Phase.UPCOMING -> stringResource(R.string.in_time, PrayerSchedule.formatRemaining(row.remaining!!, false))
                Phase.PASSED -> null
            }
            if (status != null) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (row.phase) {
                        Phase.CURRENT -> scheme.primary
                        Phase.NEXT -> scheme.secondary
                        else -> scheme.onSurfaceVariant
                    },
                    fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
        Text(
            row.time,
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = if (dim) scheme.onSurfaceVariant else scheme.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (row.alertMode != null) {
            val (icon, label) = when (row.alertMode) {
                AlertMode.OFF -> R.drawable.ic_bell_off to R.string.mode_off
                AlertMode.NOTIFICATION -> R.drawable.ic_bell to R.string.mode_notification
                AlertMode.ADHAN -> R.drawable.ic_volume to R.string.mode_adhan
            }
            IconButton(onClick = onCycleAlert, modifier = Modifier.padding(start = 2.dp)) {
                Icon(
                    painterResource(icon),
                    contentDescription = stringResource(label),
                    tint = if (row.alertMode == AlertMode.OFF) scheme.outline else scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(50.dp))
        }
        if (row.prayer.isPrayer) {
            PrayedToggle(
                checked = row.prayed == true,
                enabled = row.prayed != null,
                onCheckedChange = onPrayedChange,
            )
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun Notice(text: String, action: String, onAction: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun SourceFooter(syncedAgo: String?) {
    Text(
        if (syncedAgo != null) stringResource(R.string.source_synced, syncedAgo) else stringResource(R.string.source_calculated),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

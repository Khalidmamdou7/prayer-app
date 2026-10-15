package com.azan.prayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.prayer.PrayerSchedule
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Re-read permission state whenever the screen resumes.
            val notificationsOk = remember(refreshKey) { Notifications.canPost(context) }
            val exactOk = remember(refreshKey) { AlarmScheduler.canScheduleExact(context) }
            if (!notificationsOk) {
                Banner(stringResource(R.string.perm_notifications)) {
                    if (Build.VERSION.SDK_INT >= 33) onRequestNotifications()
                }
            }
            if (!exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Banner(stringResource(R.string.perm_exact)) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            }

            val schedule = settings.schedule()
            if (schedule == null) {
                Text(stringResource(R.string.no_location))
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.set_location)) }
                return@Column
            }

            val today = now.toLocalDate()
            val times = remember(settings, today) { settings.calculator()!!.timesFor(today) }
            val (nextPrayer, nextTime) = schedule.next(now)
            val current = schedule.current(now)

            HeaderCard(settings, today, nextPrayer, Duration.between(now, nextTime))

            // Read prayed flags on every tick: notification actions may change them while we are open.
            val prayedKey = refreshKey to prayedVersion
            val is24h = DateFormat.is24HourFormat(context)
            val formatter = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    for (prayer in Prayer.entries) {
                        val time = times[prayer]
                        // After midnight the running Isha belongs to yesterday.
                        val active = current?.takeIf { it.first == prayer }
                        val isCurrent = active != null
                        val status = when {
                            isCurrent -> stringResource(R.string.now_time)
                            time.isAfter(now) -> stringResource(
                                R.string.in_time,
                                PrayerSchedule.formatRemaining(Duration.between(now, time), withSeconds = false),
                            )
                            else -> stringResource(R.string.passed)
                        }
                        val prayedDate = active?.second ?: today
                        val prayed = remember(prayedKey, prayer, prayedDate) { store.isPrayed(prayer, prayedDate) }
                        PrayerRow(
                            prayer = prayer,
                            time = time.format(formatter),
                            status = status,
                            highlighted = prayer == nextPrayer || isCurrent,
                            alertMode = if (prayer.isPrayer) settings.alertMode(prayer) else null,
                            onCycleAlert = {
                                val next = AlertMode.entries[(settings.alertMode(prayer).ordinal + 1) % AlertMode.entries.size]
                                onChange(settings.copy(alertModes = settings.alertModes + (prayer to next)))
                            },
                            prayed = if (prayer.isPrayer && (!time.isAfter(now) || isCurrent)) prayed else null,
                            onPrayedChange = {
                                store.setPrayed(prayer, prayedDate, it)
                                prayedVersion++
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(settings: AppSettings, today: LocalDate, next: Prayer, remaining: Duration) {
    val gregorian = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val hijri = runCatching {
        HijrahDate.from(today).format(DateTimeFormatter.ofPattern("d MMMM y").withChronology(HijrahChronology.INSTANCE))
    }.getOrNull()
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                settings.locationName.ifBlank { "%.3f, %.3f".format(settings.latitude, settings.longitude) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(gregorian, style = MaterialTheme.typography.bodyMedium)
            if (hijri != null) Text("$hijri AH", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.padding(6.dp))
            Text(stringResource(R.string.next_prayer, stringResource(next.label)), style = MaterialTheme.typography.titleLarge)
            Text(
                PrayerSchedule.formatRemaining(remaining, withSeconds = true),
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    time: String,
    status: String,
    highlighted: Boolean,
    alertMode: AlertMode?,
    onCycleAlert: () -> Unit,
    prayed: Boolean?,
    onPrayedChange: (Boolean) -> Unit,
) {
    val bg = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(prayer.label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            )
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(time, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(4.dp))
        if (alertMode != null) {
            IconButton(onClick = onCycleAlert) {
                val (icon, label) = when (alertMode) {
                    AlertMode.OFF -> R.drawable.ic_bell_off to R.string.mode_off
                    AlertMode.NOTIFICATION -> R.drawable.ic_bell to R.string.mode_notification
                    AlertMode.ADHAN -> R.drawable.ic_volume to R.string.mode_adhan
                }
                Icon(painterResource(icon), contentDescription = stringResource(label))
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        if (prayed != null) {
            Checkbox(checked = prayed, onCheckedChange = onPrayedChange)
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun Banner(text: String, onAction: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction) { Text(stringResource(R.string.allow)) }
        }
    }
}

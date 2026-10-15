package com.azan.prayer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.alarm.AdhanService
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.sync.SyncWorker
import com.azan.prayer.sync.SyncedTimesStore
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    if (settings.hasLocation) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocationSection(settings, onChange)
            CalculationSection(settings, onChange)
            SyncSection(settings, onChange)
            AdjustSection(settings, onChange)
            AlertsSection(settings, onChange)
            ReminderSection(settings, onChange)
            ReliabilitySection()
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Section(stringResource(R.string.section_calculation)) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = settings.method.title,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.method)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CalculationMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.title) },
                        onClick = {
                            expanded = false
                            onChange(settings.copy(method = method))
                        },
                    )
                }
            }
        }
        Text(stringResource(R.string.asr_method), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AsrMethod.entries.forEachIndexed { i, asr ->
                SegmentedButton(
                    selected = settings.asrMethod == asr,
                    onClick = { onChange(settings.copy(asrMethod = asr)) },
                    shape = SegmentedButtonDefaults.itemShape(i, AsrMethod.entries.size),
                ) { Text(if (asr == AsrMethod.STANDARD) "Standard" else "Hanafi") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onChange(settings.copy(adhanUri = uri, adhanName = displayName(context, uri)))
        }
    }

    Section(stringResource(R.string.section_alerts)) {
        val labels = mapOf(
            AlertMode.OFF to stringResource(R.string.mode_off),
            AlertMode.NOTIFICATION to stringResource(R.string.mode_notification),
            AlertMode.ADHAN to stringResource(R.string.mode_adhan),
        )
        for (prayer in Prayer.prayers) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(prayer.label), Modifier.width(80.dp))
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    AlertMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.alertMode(prayer) == mode,
                            onClick = { onChange(settings.copy(alertModes = settings.alertModes + (prayer to mode))) },
                            shape = SegmentedButtonDefaults.itemShape(i, AlertMode.entries.size),
                            icon = {},
                        ) { Text(labels.getValue(mode), style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        Text(stringResource(R.string.adhan_sound), style = MaterialTheme.typography.labelLarge)
        AdhanVoicePicker(settings, onChange, onChooseFile = { picker.launch(arrayOf("audio/*")) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { AdhanService.start(context, Prayer.DHUHR) }) { Text(stringResource(R.string.test_adhan)) }
            TextButton(onClick = { AdhanService.stop(context) }) { Text(stringResource(R.string.stop)) }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()

@Composable
private fun ReminderSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Section(stringResource(R.string.section_reminder)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reminder_switch), Modifier.weight(1f))
            Switch(settings.reminderEnabled, { onChange(settings.copy(reminderEnabled = it)) })
        }
        Text(
            stringResource(R.string.reminder_desc, settings.reminderMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.reminderEnabled) {
            Text(stringResource(R.string.minutes_before), style = MaterialTheme.typography.labelLarge)
            val options = listOf(10, 15, 20, 30, 45)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, minutes ->
                    SegmentedButton(
                        selected = settings.reminderMinutes == minutes,
                        onClick = { onChange(settings.copy(reminderMinutes = minutes)) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        icon = {},
                    ) { Text("$minutes") }
                }
            }
        }
    }
}

@Composable
private fun SyncSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val context = LocalContext.current
    val version by SyncedTimesStore.version.collectAsState()
    val synced = remember(version) { SyncedTimesStore(context).load() }
    Section(stringResource(R.string.section_sync)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sync_switch), Modifier.weight(1f))
            Switch(settings.syncEnabled, { onChange(settings.copy(syncEnabled = it)) })
        }
        Text(
            stringResource(R.string.sync_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.syncEnabled) {
            val current = synced.key == settings.syncKey() && synced.syncedAt != null
            Text(
                if (current) {
                    stringResource(
                        R.string.sync_last,
                        DateUtils.getRelativeTimeSpanString(synced.syncedAt!!.toEpochMilli()).toString(),
                        synced.days.keys.count { !it.isBefore(LocalDate.now()) },
                    )
                } else {
                    stringResource(R.string.sync_never)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            synced.lastError?.let {
                Text(stringResource(R.string.sync_error, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = { SyncWorker.syncNow(context) }) { Text(stringResource(R.string.sync_now)) }
        }
    }
}

@Composable
private fun AdjustSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Section(stringResource(R.string.section_adjust)) {
        Text(
            stringResource(R.string.adjust_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (prayer in Prayer.entries) {
            val minutes = settings.adjustments[prayer] ?: 0
            fun set(value: Int) = onChange(settings.copy(adjustments = settings.adjustments + (prayer to value)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(prayer.label), Modifier.weight(1f))
                TextButton(onClick = { set((minutes - 1).coerceAtLeast(-MAX_ADJUST)) }) { Text("−") }
                Text(
                    if (minutes > 0) "+$minutes" else "$minutes",
                    Modifier.width(40.dp),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { set((minutes + 1).coerceAtMost(MAX_ADJUST)) }) { Text("+") }
            }
        }
    }
}

private const val MAX_ADJUST = 30

@Composable
private fun ReliabilitySection() {
    val context = LocalContext.current
    Section(stringResource(R.string.section_reliability)) {
        Text(stringResource(R.string.battery_desc), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }) { Text(stringResource(R.string.battery_settings)) }
    }
}

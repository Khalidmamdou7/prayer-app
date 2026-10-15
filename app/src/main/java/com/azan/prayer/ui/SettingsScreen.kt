package com.azan.prayer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.azan.prayer.R
import com.azan.prayer.alarm.AdhanService
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.AppSettings
import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

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
            AlertsSection(settings, onChange)
            ReminderSection(settings, onChange)
            ReliabilitySection()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun LocationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lat by remember(settings.latitude) { mutableStateOf(settings.latitude?.toString() ?: "") }
    var lng by remember(settings.longitude) { mutableStateOf(settings.longitude?.toString() ?: "") }
    var name by remember(settings.locationName) { mutableStateOf(settings.locationName) }
    var message by remember { mutableStateOf<String?>(null) }
    val locatingText = stringResource(R.string.locating)
    val failedText = stringResource(R.string.location_failed)
    val invalidText = stringResource(R.string.invalid_coordinates)

    fun locate() {
        message = locatingText
        scope.launch {
            val location = currentLocation(context)
            if (location == null) {
                message = failedText
                return@launch
            }
            val place = withContext(Dispatchers.IO) { placeName(context, location) }
            message = null
            onChange(settings.copy(latitude = location.latitude, longitude = location.longitude, locationName = place ?: ""))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else message = failedText
    }

    Section(stringResource(R.string.section_location)) {
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    locate()
                } else {
                    permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.use_current_location)) }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                lat, { lat = it },
                label = { Text(stringResource(R.string.latitude)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                lng, { lng = it },
                label = { Text(stringResource(R.string.longitude)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            name, { name = it },
            label = { Text(stringResource(R.string.location_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                val la = lat.trim().toDoubleOrNull()
                val lo = lng.trim().toDoubleOrNull()
                if (la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0) {
                    message = invalidText
                } else {
                    message = null
                    onChange(settings.copy(latitude = la, longitude = lo, locationName = name.trim()))
                }
            },
        ) { Text(stringResource(R.string.save_location)) }
    }
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location? {
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    for (provider in providers) {
        val fresh = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(context)) {
                    if (cont.isActive) cont.resume(it)
                }
            }
        }
        if (fresh != null) return fresh
    }
    return providers.firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
}

@Suppress("DEPRECATION")
private fun placeName(context: Context, location: Location): String? = runCatching {
    if (!Geocoder.isPresent()) return null
    Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
        ?.firstOrNull()
        ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
}.getOrNull()

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
        Text(settings.adhanName ?: stringResource(R.string.adhan_default), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.choose_file)) }
            if (settings.adhanUri != null) {
                TextButton(onClick = { onChange(settings.copy(adhanUri = null, adhanName = null)) }) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
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
private fun ReliabilitySection() {
    val context = LocalContext.current
    Section(stringResource(R.string.section_reliability)) {
        Text(stringResource(R.string.battery_desc), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }) { Text(stringResource(R.string.battery_settings)) }
    }
}

package com.azan.prayer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.azan.prayer.data.AppSettings
import com.azan.prayer.location.City
import com.azan.prayer.location.CityDatabase
import com.azan.prayer.location.CitySearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** A status line with an optional button (e.g. "Open settings"). */
private data class Notice(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

@Composable
fun LocationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Section(stringResource(R.string.section_location)) {
        if (settings.hasLocation) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text(
                        settings.locationName.ifBlank { stringResource(R.string.custom_location) },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "%.4f, %.4f".format(settings.latitude, settings.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        LocationPicker(settings, onChange)
        ManualCoordinates(settings, onChange)
    }
}

/** City search plus "use my location". Calls [onChange] with the new location. */
@Composable
fun LocationPicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cities by produceState<CitySearch?>(null) { value = CityDatabase.get(context) }
    var notice by remember { mutableStateOf<Notice?>(null) }

    val s = LocationStrings(
        locating = stringResource(R.string.locating),
        locationOff = stringResource(R.string.location_off),
        noFix = stringResource(R.string.location_failed),
        denied = stringResource(R.string.location_denied),
        blocked = stringResource(R.string.location_blocked),
        openSettings = stringResource(R.string.open_settings),
        methodSet = stringResource(R.string.method_auto_set),
    )

    fun applyCity(city: City, latitude: Double = city.latitude, longitude: Double = city.longitude) {
        val method = CitySearch.methodForCountry(city.countryCode)
        onChange(
            settings.copy(
                latitude = latitude,
                longitude = longitude,
                locationName = city.localName(context.resources.configuration.locales[0]),
                method = method ?: settings.method,
            ),
        )
        notice = if (method != null && method != settings.method) Notice(s.methodSet.format(context.getString(method.title))) else null
    }

    fun locate() {
        notice = Notice(s.locating)
        scope.launch {
            when (val result = currentLocation(context)) {
                LocateResult.LocationOff -> notice = Notice(s.locationOff, s.openSettings) {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                LocateResult.NoFix -> notice = Notice(s.noFix)
                is LocateResult.Found -> {
                    val loc = result.location
                    // Name the place from the offline city list; no internet needed.
                    val city = cities?.nearest(loc.latitude, loc.longitude)
                    if (city != null) {
                        applyCity(city, loc.latitude, loc.longitude)
                    } else {
                        notice = null
                        onChange(settings.copy(latitude = loc.latitude, longitude = loc.longitude, locationName = ""))
                    }
                }
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            locate()
        } else {
            val activity = context.findActivity()
            // If Android no longer shows the dialog, the user must allow it from the app's settings page.
            val blocked = activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            notice = if (blocked) {
                Notice(s.blocked, s.openSettings) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                }
            } else {
                Notice(s.denied)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CitySearchField(cities, onPick = ::applyCity)
        OutlinedButton(
            onClick = {
                if (hasLocationPermission(context)) {
                    locate()
                } else {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.use_current_location), Modifier.padding(start = 8.dp))
        }
        notice?.let { n ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    n.text,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (n.actionLabel != null && n.action != null) TextButton(onClick = n.action) { Text(n.actionLabel) }
            }
        }
    }
}

private data class LocationStrings(
    val locating: String,
    val locationOff: String,
    val noFix: String,
    val denied: String,
    val blocked: String,
    val openSettings: String,
    val methodSet: String,
)

@Composable
private fun CitySearchField(cities: CitySearch?, onPick: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<City>()) }
    LaunchedEffect(query, cities) {
        if (cities == null || query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(150) // debounce typing
        results = withContext(Dispatchers.Default) { cities.search(query, limit = 8) }
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(stringResource(R.string.search_city)) },
        placeholder = { Text(stringResource(R.string.search_city_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = null) }
            }
        },
        singleLine = true,
        enabled = cities != null,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
    if (query.isNotBlank() && cities != null && results.isEmpty()) {
        Text(stringResource(R.string.no_cities), style = MaterialTheme.typography.bodySmall)
    }
    Column {
        results.forEachIndexed { i, city ->
            if (i > 0) HorizontalDivider()
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(city)
                        query = ""
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            ) {
                Text(
                    if (city.arabicName.isNotBlank()) "${city.name} · ${city.arabicName}" else city.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    listOf(city.region, city.countryName()).filter { it.isNotBlank() }.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ManualCoordinates(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(stringResource(R.string.enter_coordinates)) }
    if (!open) return
    var lat by remember(settings.latitude) { mutableStateOf(settings.latitude?.toString() ?: "") }
    var lng by remember(settings.longitude) { mutableStateOf(settings.longitude?.toString() ?: "") }
    var name by remember(settings.locationName) { mutableStateOf(settings.locationName) }
    var invalid by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (invalid) Text(stringResource(R.string.invalid_coordinates), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val la = lat.trim().toDoubleOrNull()
            val lo = lng.trim().toDoubleOrNull()
            invalid = la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0
            if (!invalid) {
                onChange(settings.copy(latitude = la, longitude = lo, locationName = name.trim()))
                open = false
            }
        }) { Text(stringResource(R.string.save_location)) }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private sealed interface LocateResult {
    data class Found(val location: Location) : LocateResult
    data object LocationOff : LocateResult
    data object NoFix : LocateResult
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): LocateResult {
    val lm = context.getSystemService(LocationManager::class.java) ?: return LocateResult.NoFix
    if (!LocationManagerCompat.isLocationEnabled(lm)) return LocateResult.LocationOff

    val candidates = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
    }
    val providers = candidates.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

    for (provider in providers) {
        val fix = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(context)) {
                        if (cont.isActive) cont.resume(it)
                    }
                } catch (_: SecurityException) {
                    // e.g. GPS with only approximate permission on older Android
                    if (cont.isActive) cont.resume(null)
                } catch (_: IllegalArgumentException) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        if (fix != null) return LocateResult.Found(fix)
    }

    val lastKnown = lm.allProviders
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    return lastKnown?.let { LocateResult.Found(it) } ?: LocateResult.NoFix
}

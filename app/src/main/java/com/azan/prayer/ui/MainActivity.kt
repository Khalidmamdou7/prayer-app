package com.azan.prayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications
import com.azan.prayer.data.AppSettings
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.sync.SyncWorker
import com.azan.prayer.sync.SyncedTimes
import com.azan.prayer.sync.SyncedTimesStore
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AzanTheme { AzanApp() } }
    }
}

@Composable
private fun AzanApp() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var showSettings by rememberSaveable { mutableStateOf(!settings.hasLocation) }
    // Bumped whenever something outside Compose may have changed (notification actions, permissions).
    var refreshKey by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        refreshKey++
        AlarmScheduler.rescheduleAll(context)
        if (settings.syncEnabled && settings.hasLocation && needsSync(SyncedTimesStore(context).load(), settings)) {
            SyncWorker.syncNow(context)
        }
        onPauseOrDispose { }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshKey++
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val update: (AppSettings) -> Unit = { new ->
        val old = settings
        settings = new
        store.save(new)
        AlarmScheduler.rescheduleAll(context)
        if (new.syncEnabled && new.hasLocation && (new.syncKey() != old.syncKey() || !old.syncEnabled)) {
            SyncWorker.syncNow(context)
        }
    }

    if (showSettings) {
        BackHandler(enabled = settings.hasLocation) { showSettings = false }
        SettingsScreen(
            settings = settings,
            onChange = update,
            onBack = { showSettings = false },
        )
    } else {
        HomeScreen(
            settings = settings,
            store = store,
            refreshKey = refreshKey,
            onChange = update,
            onOpenSettings = { showSettings = true },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
    }
}

/** True when stored times are for other settings, older than a day, or about to run out. */
private fun needsSync(synced: SyncedTimes, settings: AppSettings): Boolean {
    val syncedAt = synced.syncedAt ?: return true
    if (synced.key != settings.syncKey()) return true
    if (Duration.between(syncedAt, Instant.now()) > Duration.ofDays(1)) return true
    val lastDay = synced.days.keys.maxOrNull() ?: return true
    return lastDay.isBefore(LocalDate.now().plusDays(14))
}

private val Green = Color(0xFF0F5B4E)
private val Gold = Color(0xFFC9A646)

@Composable
fun AzanTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF7FD4C1), secondary = Gold)
        else -> lightColorScheme(primary = Green, secondary = Gold)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

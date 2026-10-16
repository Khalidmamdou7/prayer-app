package com.azan.prayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications
import com.azan.prayer.data.AppSettings
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.ui.theme.AzanTheme
import com.azan.prayer.sync.SyncWorker
import com.azan.prayer.sync.SyncedTimes
import com.azan.prayer.sync.SyncedTimesStore
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
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
    var showSettings by rememberSaveable { mutableStateOf(false) }
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
    // Ask for notifications once setup is finished, right after the user chose how to be alerted.
    val ready = settings.hasLocation && settings.onboarded
    LaunchedEffect(ready) {
        if (ready && Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
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

    val screen = when {
        !settings.hasLocation -> Screen.WELCOME
        !settings.onboarded -> Screen.SETUP
        showSettings -> Screen.SETTINGS
        else -> Screen.HOME
    }
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { if (forward) it / 8 else -it / 8 }) togetherWith
                fadeOut(tween(160))
        },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.WELCOME -> WelcomeScreen(settings, update)
            Screen.SETUP -> {
                // Back goes to choosing the city again.
                BackHandler { update(settings.copy(latitude = null, longitude = null, locationName = "")) }
                SetupScreen(settings, update, onDone = {})
            }
            Screen.SETTINGS -> {
                BackHandler { showSettings = false }
                SettingsScreen(settings = settings, onChange = update, onBack = { showSettings = false })
            }
            Screen.HOME -> HomeScreen(
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
}

/** True when stored times are for other settings, older than a day, or about to run out. */
private fun needsSync(synced: SyncedTimes, settings: AppSettings): Boolean {
    val syncedAt = synced.syncedAt ?: return true
    if (synced.key != settings.syncKey()) return true
    if (Duration.between(syncedAt, Instant.now()) > Duration.ofDays(1)) return true
    val lastDay = synced.days.keys.maxOrNull() ?: return true
    return lastDay.isBefore(LocalDate.now().plusDays(14))
}

private enum class Screen { WELCOME, SETUP, HOME, SETTINGS }

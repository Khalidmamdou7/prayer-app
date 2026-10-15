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
        settings = new
        store.save(new)
        AlarmScheduler.rescheduleAll(context)
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

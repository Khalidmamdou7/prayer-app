package com.azan.prayer.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    @Test
    fun newUserGoesThroughSetup() {
        prefs.edit().clear().commit()
        val store = SettingsStore(context)
        assertFalse(store.load().onboarded)

        // Choosing a city alone doesn't finish setup...
        store.save(store.load().copy(latitude = 30.0, longitude = 31.0))
        assertFalse(store.load().onboarded)
        // ...pressing Start does.
        store.save(store.load().copy(onboarded = true))
        assertTrue(store.load().onboarded)
    }

    @Test
    fun updatingFromAnOlderVersionSkipsSetup() {
        // What version 1.0 (before onboarding existed) left behind.
        prefs.edit().clear().putString("lat", "30.0").putString("lng", "31.0").commit()
        val settings = SettingsStore(context).load()
        assertTrue(settings.onboarded)
        assertFalse(settings.homeTipSeen)
    }
}

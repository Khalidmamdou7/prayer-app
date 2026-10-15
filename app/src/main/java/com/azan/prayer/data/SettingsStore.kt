package com.azan.prayer.data

import android.content.Context
import android.net.Uri
import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.prayer.PrayerCalculator
import com.azan.prayer.prayer.PrayerSchedule
import com.azan.prayer.prayer.PrayerTimesSource
import com.azan.prayer.sync.SyncedTimes
import java.time.LocalDate
import java.util.Locale

/** What happens when a prayer's time comes in. */
enum class AlertMode { OFF, NOTIFICATION, ADHAN }

data class AppSettings(
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String,
    val method: CalculationMethod,
    val asrMethod: AsrMethod,
    val alertModes: Map<Prayer, AlertMode>,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val adhanUri: Uri?,
    val adhanName: String?,
    val syncEnabled: Boolean,
    /** Minutes added to each time (negative = earlier), to match a local mosque. */
    val adjustments: Map<Prayer, Int>,
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null

    fun alertMode(prayer: Prayer): AlertMode = alertModes[prayer] ?: AlertMode.NOTIFICATION

    fun calculator(): PrayerCalculator? =
        if (latitude != null && longitude != null) {
            PrayerCalculator(latitude, longitude, method, asrMethod)
        } else {
            null
        }

    /** Identifies what synced times are valid for; they're discarded when this changes. */
    fun syncKey(): String = String.format(
        Locale.US, "%.4f,%.4f,%s,%s", latitude ?: 0.0, longitude ?: 0.0, method.name, asrMethod.name,
    )

    /** Synced times when available and still matching these settings, otherwise the calculation. */
    fun timesSource(synced: SyncedTimes): PrayerTimesSource? = calculator()?.let { calc ->
        val days = if (syncEnabled && synced.key == syncKey()) synced.days else emptyMap()
        PrayerTimesSource(calc, days, adjustments)
    }

    fun schedule(synced: SyncedTimes): PrayerSchedule? = timesSource(synced)?.let { PrayerSchedule(it::timesFor) }
}

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull(),
        longitude = prefs.getString(KEY_LNG, null)?.toDoubleOrNull(),
        locationName = prefs.getString(KEY_LOCATION_NAME, "") ?: "",
        method = enumOrDefault(prefs.getString(KEY_METHOD, null), CalculationMethod.EGYPT),
        asrMethod = enumOrDefault(prefs.getString(KEY_ASR, null), AsrMethod.STANDARD),
        alertModes = Prayer.prayers.associateWith {
            enumOrDefault(prefs.getString(KEY_ALERT_PREFIX + it.name, null), AlertMode.NOTIFICATION)
        },
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, true),
        reminderMinutes = prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES),
        adhanUri = prefs.getString(KEY_ADHAN_URI, null)?.let(Uri::parse),
        adhanName = prefs.getString(KEY_ADHAN_NAME, null),
        syncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, true),
        adjustments = Prayer.entries.associateWith { prefs.getInt(KEY_ADJUST_PREFIX + it.name, 0) },
    )

    fun save(s: AppSettings) {
        prefs.edit().apply {
            putString(KEY_LAT, s.latitude?.toString())
            putString(KEY_LNG, s.longitude?.toString())
            putString(KEY_LOCATION_NAME, s.locationName)
            putString(KEY_METHOD, s.method.name)
            putString(KEY_ASR, s.asrMethod.name)
            s.alertModes.forEach { (prayer, mode) -> putString(KEY_ALERT_PREFIX + prayer.name, mode.name) }
            putBoolean(KEY_REMINDER_ENABLED, s.reminderEnabled)
            putInt(KEY_REMINDER_MINUTES, s.reminderMinutes)
            putString(KEY_ADHAN_URI, s.adhanUri?.toString())
            putString(KEY_ADHAN_NAME, s.adhanName)
            putBoolean(KEY_SYNC_ENABLED, s.syncEnabled)
            s.adjustments.forEach { (prayer, minutes) -> putInt(KEY_ADJUST_PREFIX + prayer.name, minutes) }
        }.apply()
    }

    fun isPrayed(prayer: Prayer, date: LocalDate): Boolean = prefs.getBoolean(prayedKey(prayer, date), false)

    fun setPrayed(prayer: Prayer, date: LocalDate, prayed: Boolean) {
        prefs.edit().apply {
            if (prayed) putBoolean(prayedKey(prayer, date), true) else remove(prayedKey(prayer, date))
            // Keep the file small: drop records older than a week.
            val cutoff = date.minusDays(7)
            prefs.all.keys
                .filter { it.startsWith(KEY_PRAYED_PREFIX) }
                .filter { key -> key.split('_').getOrNull(1)?.let(LocalDate::parse)?.isBefore(cutoff) == true }
                .forEach(::remove)
        }.apply()
    }

    private fun prayedKey(prayer: Prayer, date: LocalDate) = "${KEY_PRAYED_PREFIX}${date}_${prayer.name}"

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 20
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_LOCATION_NAME = "location_name"
        private const val KEY_METHOD = "method"
        private const val KEY_ASR = "asr_method"
        private const val KEY_ALERT_PREFIX = "alert_"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_ADHAN_URI = "adhan_uri"
        private const val KEY_ADHAN_NAME = "adhan_name"
        private const val KEY_PRAYED_PREFIX = "prayed_"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_ADJUST_PREFIX = "adjust_"
    }
}

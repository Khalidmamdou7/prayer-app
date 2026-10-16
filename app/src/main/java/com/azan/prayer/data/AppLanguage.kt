package com.azan.prayer.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.azan.prayer.alarm.Notifications
import java.util.Locale

/**
 * In-app language choice: "" follows the phone, otherwise a language tag ("en", "ar").
 *
 * Android 13+ applies the choice to the whole app. On older versions AppCompat only applies it
 * to activities, so notifications and the adhan service use [localized] to get translated text.
 */
object AppLanguage {
    const val SYSTEM = ""
    val choices = listOf(SYSTEM, "en", "ar")

    private const val PREFS = "settings"
    private const val KEY = "language"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.getApplicationLocales()[0]?.language ?: SYSTEM
        } else {
            prefs(context).getString(KEY, SYSTEM) ?: SYSTEM
        }

    fun set(context: Context, tag: String) {
        prefs(context).edit().putString(KEY, tag).apply()
        // Channel names are shown in the system's notification settings; rename them too.
        Notifications.createChannels(withLanguage(context, tag))
        AppCompatDelegate.setApplicationLocales(
            if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    fun localized(context: Context): Context =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context else withLanguage(context, current(context))

    private fun withLanguage(context: Context, tag: String): Context {
        if (tag == SYSTEM) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

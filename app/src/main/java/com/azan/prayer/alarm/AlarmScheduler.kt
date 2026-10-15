package com.azan.prayer.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.AlarmEvent
import com.azan.prayer.prayer.EventType
import com.azan.prayer.prayer.Prayer
import java.time.ZonedDateTime

/**
 * Keeps exactly one pending alarm per (prayer, event type): the next adhan and the next
 * "have you prayed?" reminder. Every time an alarm fires, everything is rescheduled.
 */
object AlarmScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun rescheduleAll(context: Context) {
        val settings = SettingsStore(context).load()
        val schedule = settings.schedule()
        val now = ZonedDateTime.now()
        for (prayer in Prayer.prayers) {
            val adhan = schedule?.takeIf { settings.alertMode(prayer) != AlertMode.OFF }
                ?.nextAdhan(prayer, now)
            set(context, EventType.ADHAN, prayer, adhan)

            val reminder = schedule?.takeIf { settings.reminderEnabled }
                ?.nextReminder(prayer, settings.reminderMinutes, now)
            set(context, EventType.REMINDER, prayer, reminder)
        }
    }

    private fun set(context: Context, type: EventType, prayer: Prayer, event: AlarmEvent?) {
        val am = context.getSystemService(AlarmManager::class.java)
        val requestCode = type.ordinal * 100 + prayer.ordinal
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_TYPE, type.name)
            putExtra(AlarmReceiver.EXTRA_PRAYER, prayer.name)
            event?.let { putExtra(AlarmReceiver.EXTRA_DATE, it.prayerDate.toEpochDay()) }
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (event == null) {
            am.cancel(pending)
            return
        }
        val at = event.time.toInstant().toEpochMilli()
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            // Without the exact-alarm permission Android may delay this by a few minutes.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
}

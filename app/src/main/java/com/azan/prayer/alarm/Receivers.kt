package com.azan.prayer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.azan.prayer.data.AlertMode
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.EventType
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.sync.SyncedTimesStore
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** Fires for both adhan and reminder alarms, then schedules the next ones. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val type = intent.getStringExtra(EXTRA_TYPE)?.let { runCatching { EventType.valueOf(it) }.getOrNull() }
        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
        if (type != null && prayer != null) handle(context, type, prayer, date)
        AlarmScheduler.rescheduleAll(context)
    }

    private fun handle(context: Context, type: EventType, prayer: Prayer, date: LocalDate) {
        val store = SettingsStore(context)
        val settings = store.load()
        val schedule = settings.schedule(SyncedTimesStore(context).load()) ?: return
        when (type) {
            EventType.ADHAN -> {
                // Ignore alarms that arrive very late (e.g. device was off) to avoid a surprise adhan.
                val due = schedule.nextAdhan(prayer, ZonedDateTime.now().minusMinutes(LATE_TOLERANCE_MINUTES))
                if (due.prayerDate != date) return
                when (settings.alertMode(prayer)) {
                    AlertMode.OFF -> Unit
                    AlertMode.NOTIFICATION -> Notifications.showPrayerTime(context, prayer, date)
                    AlertMode.ADHAN -> if (!AdhanService.start(context, prayer)) {
                        Notifications.showPrayerTime(context, prayer, date)
                    }
                }
            }
            EventType.REMINDER -> {
                if (!settings.reminderEnabled || store.isPrayed(prayer, date)) return
                val minutesLeft = Duration.between(ZonedDateTime.now(), schedule.endOf(prayer, date)).toMinutes()
                if (minutesLeft <= 0) return
                Notifications.showReminder(context, prayer, date, minutesLeft + 1)
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.azan.prayer.FIRE"
        const val EXTRA_TYPE = "type"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_DATE = "date"
        private const val LATE_TOLERANCE_MINUTES = 10L
    }
}

/** Handles the buttons on prayer and reminder notifications. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (intent.action == ACTION_PRAYED) {
            val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
            val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
            if (prayer != null) SettingsStore(context).setPrayed(prayer, date, true)
        }
        if (notificationId != -1) NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        const val ACTION_PRAYED = "com.azan.prayer.PRAYED"
        const val ACTION_DISMISS = "com.azan.prayer.DISMISS"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_DATE = "date"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}

/** Alarms are lost on reboot, and must move when the clock or time zone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACTIONS) AlarmScheduler.rescheduleAll(context)
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

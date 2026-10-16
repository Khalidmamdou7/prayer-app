package com.azan.prayer.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.azan.prayer.R
import com.azan.prayer.data.AppLanguage
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.ui.MainActivity
import java.time.LocalDate

object Notifications {
    const val CHANNEL_PRAYER = "prayer_time"
    const val CHANNEL_REMINDER = "prayer_reminder"
    const val CHANNEL_ADHAN = "adhan_playback"

    const val ID_ADHAN_SERVICE = 1

    fun createChannels(base: Context) {
        val context = AppLanguage.localized(base)
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_PRAYER,
                    context.getString(R.string.channel_prayer),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), alarmAudio)
                },
                NotificationChannel(
                    CHANNEL_REMINDER,
                    context.getString(R.string.channel_reminder),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                NotificationChannel(
                    CHANNEL_ADHAN,
                    context.getString(R.string.channel_adhan),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    // The adhan audio itself is played by AdhanService.
                    setSound(null, null)
                },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    fun prayerNotificationId(prayer: Prayer) = 1000 + prayer.ordinal
    fun reminderNotificationId(prayer: Prayer) = 2000 + prayer.ordinal

    /** Plain "it's time for X" notification with the default notification sound. */
    fun showPrayerTime(base: Context, prayer: Prayer, date: LocalDate) {
        val context = AppLanguage.localized(base)
        val name = context.getString(prayer.label)
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_prayer_title, name))
            .setContentText(context.getString(R.string.notif_prayer_text, name))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_prayed), prayedIntent(context, prayer, date, prayerNotificationId(prayer)))
            .build()
        post(context, prayerNotificationId(prayer), notification)
    }

    /** "Have you prayed X?" shown shortly before X's time ends. */
    fun showReminder(base: Context, prayer: Prayer, date: LocalDate, minutesLeft: Long) {
        val context = AppLanguage.localized(base)
        val name = context.getString(prayer.label)
        val id = reminderNotificationId(prayer)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_reminder_title, name))
            .setContentText(context.resources.getQuantityString(R.plurals.notif_reminder_text, minutesLeft.toInt(), name, minutesLeft.toInt()))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_yes_prayed), prayedIntent(context, prayer, date, id))
            .addAction(0, context.getString(R.string.action_not_yet), dismissIntent(context, id))
            .build()
        post(context, id, notification)
    }

    fun adhanPlaying(base: Context, prayerName: String, stopIntent: PendingIntent): Notification {
        val context = AppLanguage.localized(base)
        return NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_prayer_title, prayerName))
            .setContentText(context.getString(R.string.notif_adhan_playing))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context))
            .setDeleteIntent(stopIntent)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.action_stop), stopIntent)
            .build()
    }

    private fun prayedIntent(context: Context, prayer: Prayer, date: LocalDate, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_PRAYED
            putExtra(ReminderActionReceiver.EXTRA_PRAYER, prayer.name)
            putExtra(ReminderActionReceiver.EXTRA_DATE, date.toEpochDay())
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun dismissIntent(context: Context, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_DISMISS
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(context, notificationId + 500, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}

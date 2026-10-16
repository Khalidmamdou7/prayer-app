package com.azan.prayer.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.azan.prayer.data.AppLanguage
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.Prayer

/** Foreground service that plays the adhan once, with a "Stop" button in its notification. */
class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: ""
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        ServiceCompat.startForeground(
            this,
            Notifications.ID_ADHAN_SERVICE,
            Notifications.adhanPlaying(this, prayerName, stop),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        play(intent?.getStringExtra(EXTRA_URI)?.let(Uri::parse))
        return START_NOT_STICKY
    }

    private fun play(customUri: Uri?) {
        player?.release()
        val fallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val candidates = listOfNotNull(customUri, fallback)
        for (uri in candidates) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    setWakeMode(this@AdhanService, PowerManager.PARTIAL_WAKE_LOCK)
                    setDataSource(this@AdhanService, uri)
                    setOnCompletionListener { stopSelf() }
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    prepare()
                    start()
                }
                handler.postDelayed({ stopSelf() }, MAX_PLAY_MILLIS)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Cannot play $uri", e)
                player?.release()
                player = null
            }
        }
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AdhanService"
        private const val ACTION_PLAY = "com.azan.prayer.PLAY_ADHAN"
        private const val ACTION_STOP = "com.azan.prayer.STOP_ADHAN"
        private const val EXTRA_PRAYER_NAME = "prayer_name"
        private const val EXTRA_URI = "uri"
        private const val MAX_PLAY_MILLIS = 6 * 60 * 1000L

        /** Returns false if Android refused to start the service (e.g. background restrictions). */
        fun start(context: Context, prayer: Prayer): Boolean {
            val settings = SettingsStore(context).load()
            val intent = Intent(context, AdhanService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PRAYER_NAME, AppLanguage.localized(context).getString(prayer.label))
                settings.adhanUri?.let { putExtra(EXTRA_URI, it.toString()) }
            }
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start adhan", e)
                false
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AdhanService::class.java))
        }
    }
}

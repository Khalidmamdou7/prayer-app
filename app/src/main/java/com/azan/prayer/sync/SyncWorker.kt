package com.azan.prayer.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.data.SettingsStore
import com.azan.prayer.prayer.Prayer
import com.azan.prayer.prayer.PrayerTimesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Downloads this month's and next month's times whenever the phone is online. The app keeps
 * working offline from this cache, and falls back to the on-device calculation after it runs out.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = SettingsStore(applicationContext).load()
        val lat = settings.latitude
        val lng = settings.longitude
        val calculator = settings.calculator()
        if (!settings.syncEnabled || lat == null || lng == null || calculator == null) return@withContext Result.success()

        val store = SyncedTimesStore(applicationContext)
        val previous = store.load()
        val key = settings.syncKey()
        try {
            val thisMonth = YearMonth.now()
            val downloaded = listOf(thisMonth, thisMonth.plusMonths(1))
                .map { AladhanApi.fetchMonth(lat, lng, settings.method, settings.asrMethod, it.year, it.monthValue) }
                .fold(emptyMap<LocalDate, Map<Prayer, Instant>>()) { acc, m -> acc + m }
            val valid = PrayerTimesSource.validate(calculator, downloaded)
            if (valid.isEmpty()) throw IllegalStateException("No usable times in response")

            // Settings may have changed while we were downloading; don't store stale results.
            if (SettingsStore(applicationContext).load().syncKey() != key) return@withContext Result.success()

            val yesterday = LocalDate.now().minusDays(1)
            val kept = if (previous.key == key) previous.days.filterKeys { !it.isBefore(yesterday) } else emptyMap()
            store.save(SyncedTimes(key, Instant.now(), kept + valid, lastError = null))
            AlarmScheduler.rescheduleAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed", e)
            store.save(previous.copy(lastError = e.message ?: e.javaClass.simpleName))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC = "sync_periodic"
        private const val NOW = "sync_now"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Refreshes the cache roughly twice a day, whenever there is a connection. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Syncs as soon as the phone is online (immediately if it already is). */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

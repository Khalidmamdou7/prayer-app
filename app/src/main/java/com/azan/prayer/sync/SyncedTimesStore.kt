package com.azan.prayer.sync

import android.content.Context
import com.azan.prayer.prayer.Prayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** Times downloaded for one location/method ([key]), kept on disk for offline use. */
data class SyncedTimes(
    val key: String,
    val syncedAt: Instant?,
    val days: Map<LocalDate, Map<Prayer, Instant>>,
    val lastError: String?,
) {
    companion object {
        val EMPTY = SyncedTimes("", null, emptyMap(), null)

        fun toJson(s: SyncedTimes): String = JSONObject().apply {
            put("key", s.key)
            s.syncedAt?.let { put("syncedAt", it.epochSecond) }
            s.lastError?.let { put("lastError", it) }
            put("days", JSONObject().apply {
                s.days.forEach { (date, times) ->
                    put(date.toString(), JSONObject().apply {
                        times.forEach { (prayer, t) -> put(prayer.name, t.epochSecond) }
                    })
                }
            })
        }.toString()

        fun fromJson(json: String): SyncedTimes {
            val o = JSONObject(json)
            val daysJson = o.getJSONObject("days")
            val days = daysJson.keys().asSequence().associate { dateKey ->
                val dayJson = daysJson.getJSONObject(dateKey)
                LocalDate.parse(dateKey) to Prayer.entries
                    .filter { dayJson.has(it.name) }
                    .associateWith { Instant.ofEpochSecond(dayJson.getLong(it.name)) }
            }
            return SyncedTimes(
                key = o.optString("key"),
                syncedAt = if (o.has("syncedAt")) Instant.ofEpochSecond(o.getLong("syncedAt")) else null,
                days = days,
                lastError = if (o.has("lastError")) o.getString("lastError") else null,
            )
        }
    }
}

class SyncedTimesStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "synced_times.json")

    fun load(): SyncedTimes = synchronized(lock) {
        runCatching { SyncedTimes.fromJson(file.readText()) }.getOrDefault(SyncedTimes.EMPTY)
    }

    fun save(times: SyncedTimes) {
        synchronized(lock) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(SyncedTimes.toJson(times))
            tmp.renameTo(file)
        }
        _version.value++
    }

    companion object {
        private val lock = Any()
        private val _version = MutableStateFlow(0)

        /** Changes whenever new times are saved, so the UI can refresh. */
        val version: StateFlow<Int> = _version
    }
}

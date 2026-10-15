package com.azan.prayer.sync

import com.azan.prayer.prayer.AsrMethod
import com.azan.prayer.prayer.CalculationMethod
import com.azan.prayer.prayer.Prayer
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Client for the free Aladhan prayer times API (https://aladhan.com/prayer-times-api). No key, no account. */
object AladhanApi {
    private const val BASE = "https://api.aladhan.com/v1/calendar"
    private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    private val keys = mapOf(
        Prayer.FAJR to "Fajr",
        Prayer.SUNRISE to "Sunrise",
        Prayer.DHUHR to "Dhuhr",
        Prayer.ASR to "Asr",
        Prayer.MAGHRIB to "Maghrib",
        Prayer.ISHA to "Isha",
    )

    fun monthUrl(
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        asr: AsrMethod,
        year: Int,
        month: Int,
    ): String = String.format(
        Locale.US,
        "%s/%d/%d?latitude=%.6f&longitude=%.6f&method=%d&school=%d&iso8601=true",
        BASE, year, month, latitude, longitude, method.aladhanId, asr.aladhanSchool,
    )

    /** Downloads one month. Blocking; call from a background thread. */
    fun fetchMonth(
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        asr: AsrMethod,
        year: Int,
        month: Int,
    ): Map<LocalDate, Map<Prayer, Instant>> {
        val connection = URL(monthUrl(latitude, longitude, method, asr, year, month)).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseCalendar(body)
        } finally {
            connection.disconnect()
        }
    }

    /** Parses a `/v1/calendar` response requested with `iso8601=true`. */
    fun parseCalendar(json: String): Map<LocalDate, Map<Prayer, Instant>> {
        val root = JSONObject(json)
        if (root.optInt("code") != 200) throw IOException("Unexpected response: ${root.opt("status")}")
        val data = root.getJSONArray("data")
        val result = mutableMapOf<LocalDate, Map<Prayer, Instant>>()
        for (i in 0 until data.length()) {
            val day = data.getJSONObject(i)
            val date = LocalDate.parse(day.getJSONObject("date").getJSONObject("gregorian").getString("date"), dateFormat)
            val timings = day.getJSONObject("timings")
            result[date] = keys.mapValues { (_, key) ->
                // Strip any " (EET)" suffix in case the server ignores iso8601.
                OffsetDateTime.parse(timings.getString(key).substringBefore(' ')).toInstant()
            }
        }
        return result
    }
}

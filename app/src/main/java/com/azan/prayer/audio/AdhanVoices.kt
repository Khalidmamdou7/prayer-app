package com.azan.prayer.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A freely licensed adhan recording from Wikimedia Commons, downloaded once and kept offline. */
data class AdhanVoice(
    val id: String,
    val title: String,
    val credit: String,
    val url: String,
    val extension: String,
    val sizeKb: Int,
)

object AdhanVoices {
    // Only recordings whose licence allows redistribution. Famous broadcast recordings
    // (Makkah, Madinah, well-known muezzins) are copyrighted; users can add those with "Choose file".
    val all = listOf(
        AdhanVoice(
            id = "beautiful",
            title = "Beautiful Adhan",
            credit = "Adam-synagda · CC0 · 2:34",
            url = "https://upload.wikimedia.org/wikipedia/commons/b/b0/Beautiful_adhan.ogg",
            extension = "ogg",
            sizeKb = 1200,
        ),
        AdhanVoice(
            id = "andrewler",
            title = "Adhan (long)",
            credit = "Andrewler · CC BY-SA 4.0 · 3:03",
            url = "https://upload.wikimedia.org/wikipedia/commons/8/86/Azan.ogg",
            extension = "ogg",
            sizeKb = 1440,
        ),
        AdhanVoice(
            id = "aaqib",
            title = "Adhan — Aaqib Azeez",
            credit = "Atcovi · CC BY-SA 4.0 · 1:27",
            url = "https://upload.wikimedia.org/wikipedia/commons/7/7d/The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3",
            extension = "mp3",
            sizeKb = 1350,
        ),
        AdhanVoice(
            id = "short",
            title = "Short adhan",
            credit = "Aishatu98 · CC0 · 0:42",
            url = "https://upload.wikimedia.org/wikipedia/commons/e/e7/Adhan.ogg",
            extension = "ogg",
            sizeKb = 330,
        ),
    )

    private const val USER_AGENT = "AzanPrayerApp/1.0 (https://github.com/Khalidmamdou7/prayer-app)"

    private fun dir(context: Context) = File(context.filesDir, "adhan").apply { mkdirs() }

    fun file(context: Context, voice: AdhanVoice) = File(dir(context), "${voice.id}.${voice.extension}")

    fun isDownloaded(context: Context, voice: AdhanVoice) = file(context, voice).let { it.exists() && it.length() > 0 }

    fun uri(context: Context, voice: AdhanVoice): Uri = Uri.fromFile(file(context, voice))

    /** The built-in voice a stored adhan URI points to, if any. */
    fun forUri(context: Context, uri: Uri?): AdhanVoice? = all.firstOrNull { uri == uri(context, it) }

    suspend fun download(context: Context, voice: AdhanVoice, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val target = file(context, voice)
        val tmp = File(target.parentFile, target.name + ".part")
        val connection = URL(voice.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val type = connection.contentType.orEmpty()
            if (!type.startsWith("audio/") && !type.startsWith("application/ogg")) throw IOException("Not audio: $type")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            if (!tmp.renameTo(target)) throw IOException("Could not save file")
        } finally {
            connection.disconnect()
            tmp.delete()
        }
    }
}

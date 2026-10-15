package com.azan.prayer.location

import com.azan.prayer.prayer.CalculationMethod
import java.io.Reader
import java.text.Normalizer
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class City(
    val name: String,
    val asciiName: String,
    val arabicName: String,
    val countryCode: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun countryName(locale: Locale = Locale.getDefault()): String =
        Locale("", countryCode).getDisplayCountry(locale).ifBlank { countryCode }

    /** "Alexandria, Virginia, United States" */
    fun displayName(locale: Locale = Locale.getDefault()): String =
        listOf(name, region, countryName(locale)).filter { it.isNotBlank() }.joinToString(", ")
}

/** Offline search over the bundled GeoNames city list (every city with 15,000+ people). */
class CitySearch(private val cities: List<City>) {
    private val keys: List<List<String>> = cities.map { c ->
        listOf(c.name, c.asciiName, c.arabicName).filter { it.isNotBlank() }.map(::normalize).distinct()
    }

    /** Best matches first: exact, then prefix, then word prefix, then substring; larger cities first within each. */
    fun search(query: String, limit: Int = 30): List<City> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val ranked = ArrayList<Pair<Int, Int>>()
        for (i in cities.indices) {
            val rank = keys[i].minOf { rank(it, q) }
            if (rank < NO_MATCH) ranked += rank to i
        }
        // The list is sorted by population, so a stable sort keeps big cities first within a rank.
        return ranked.sortedBy { it.first }.take(limit).map { cities[it.second] }
    }

    /** Closest city to a point, e.g. to name a GPS fix without internet. Null if none within [maxKm]. */
    fun nearest(latitude: Double, longitude: Double, maxKm: Double = 50.0): City? {
        val best = cities.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) } ?: return null
        return best.takeIf { distanceKm(latitude, longitude, it.latitude, it.longitude) <= maxKm }
    }

    private fun rank(key: String, q: String): Int = when {
        key == q -> 0
        key.startsWith(q) -> 1
        key.split(' ', '-').any { it.startsWith(q) } -> 2
        key.contains(q) -> 3
        else -> NO_MATCH
    }

    companion object {
        private const val NO_MATCH = 99

        fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            return 6371.0 * 2 * asin(sqrt(a))
        }

        fun parse(reader: Reader): List<City> = reader.buffered().lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val c = line.split('\t')
                if (c.size < 7) return@mapNotNull null
                City(
                    name = c[0],
                    asciiName = c[1].ifBlank { c[0] },
                    arabicName = c[2],
                    countryCode = c[3],
                    region = c[4],
                    latitude = c[5].toDoubleOrNull() ?: return@mapNotNull null,
                    longitude = c[6].toDoubleOrNull() ?: return@mapNotNull null,
                )
            }
            .toList()

        /** Lowercase, no Latin accents, no Arabic diacritics, and unified Arabic letter variants. */
        fun normalize(s: String): String {
            val stripped = Normalizer.normalize(s.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "") // accents and Arabic harakat
            val sb = StringBuilder(stripped.length)
            for (ch in stripped) {
                sb.append(
                    when (ch) {
                        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                        'ة' -> 'ه'
                        'ى' -> 'ي'
                        'ـ' -> continue
                        '\'', 'ʼ', '’', '`' -> continue
                        else -> ch
                    },
                )
            }
            var result = sb.toString().replace(Regex("\\s+"), " ")
            // "El Mansoura", "al-mansura" and "Mansoura" should all find the city.
            result = result.replace(Regex("^((al|el)[ -]|ال)(?=\\S{3,})"), "")
            return result
        }

        /** The usual calculation authority for a country, where there is a clear one. */
        fun methodForCountry(countryCode: String): CalculationMethod? = when (countryCode) {
            "EG", "SD", "LY", "SY", "LB", "IQ", "PS" -> CalculationMethod.EGYPT
            "SA", "YE" -> CalculationMethod.UMM_AL_QURA
            "AE", "BH", "OM" -> CalculationMethod.DUBAI
            "KW" -> CalculationMethod.KUWAIT
            "QA" -> CalculationMethod.QATAR
            "JO" -> CalculationMethod.JORDAN
            "TR" -> CalculationMethod.TURKEY
            "IR" -> CalculationMethod.TEHRAN
            "MA" -> CalculationMethod.MOROCCO
            "DZ" -> CalculationMethod.ALGERIA
            "TN" -> CalculationMethod.TUNISIA
            "MY" -> CalculationMethod.JAKIM
            "ID" -> CalculationMethod.KEMENAG
            "SG", "BN" -> CalculationMethod.SINGAPORE
            "PK", "IN", "BD", "AF" -> CalculationMethod.KARACHI
            "US", "CA" -> CalculationMethod.ISNA
            "GB" -> CalculationMethod.MOONSIGHTING
            "FR" -> CalculationMethod.FRANCE
            "RU" -> CalculationMethod.RUSSIA
            else -> null
        }
    }
}

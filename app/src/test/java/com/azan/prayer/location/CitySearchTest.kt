package com.azan.prayer.location

import com.azan.prayer.prayer.CalculationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CitySearchTest {
    companion object {
        // Gradle runs unit tests from the module directory.
        private val search by lazy { CitySearch(File("src/main/assets/cities.tsv").reader().use { CitySearch.parse(it) }) }
    }

    @Test
    fun bundledListIsComplete() {
        val cities = File("src/main/assets/cities.tsv").reader().use { CitySearch.parse(it) }
        assertTrue(cities.size > 30_000)
        assertTrue(cities.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 })
    }

    @Test
    fun findsCitiesByEnglishName() {
        val cairo = search.search("cairo").first()
        assertEquals("Cairo", cairo.name)
        assertEquals("EG", cairo.countryCode)
        // Biggest Alexandria (Egypt) comes before the US ones.
        assertEquals("EG", search.search("alexandria").first().countryCode)
        assertEquals("London", search.search("Lond").first().name)
    }

    @Test
    fun findsCitiesByArabicName() {
        assertEquals("Cairo", search.search("القاهرة").first().name)
        assertEquals("Cairo", search.search("القاهره").first().name) // ta marbuta typed as ha
        assertEquals("Makkah", search.search("مكة").first().name)
    }

    @Test
    fun ignoresAccentsAndArticle() {
        assertEquals(CitySearch.normalize("sao paulo"), CitySearch.normalize("São Paulo"))
        assertEquals("mansoura", CitySearch.normalize("El-Mansoura"))
        assertEquals("alexandria", CitySearch.normalize("Alexandria"))
        assertTrue(search.search("São Paulo").first().name.startsWith("S"))
    }

    @Test
    fun emptyQueryFindsNothing() {
        assertTrue(search.search("  ").isEmpty())
        assertTrue(search.search("qqqzzzxx").isEmpty())
    }

    @Test
    fun nearestCityNamesAGpsFix() {
        assertEquals("Giza", search.nearest(30.01, 31.21)?.name)
        assertNull(search.nearest(0.0, -140.0)) // middle of the Pacific
    }

    @Test
    fun distance() {
        // Cairo to Alexandria is about 180 km.
        val d = CitySearch.distanceKm(30.0626, 31.2497, 31.2018, 29.9158)
        assertTrue(d in 170.0..190.0)
    }

    @Test
    fun methodsForCountries() {
        assertEquals(CalculationMethod.EGYPT, CitySearch.methodForCountry("EG"))
        assertEquals(CalculationMethod.UMM_AL_QURA, CitySearch.methodForCountry("SA"))
        assertNull(CitySearch.methodForCountry("XX"))
    }
}

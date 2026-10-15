package com.azan.prayer.prayer

/**
 * Twilight conventions used by the major authorities.
 *
 * @param fajrAngle sun depression angle for Fajr, in degrees.
 * @param ishaAngle sun depression angle for Isha, or null when [ishaMinutes] is used.
 * @param ishaMinutes fixed interval after Maghrib for Isha.
 * @param maghribAngle sun depression angle for Maghrib, or null for sunset.
 */
enum class CalculationMethod(
    val title: String,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaMinutes: Int = 0,
    val maghribAngle: Double? = null,
) {
    EGYPT("Egyptian General Authority of Survey", 19.5, ishaAngle = 17.5),
    MWL("Muslim World League", 18.0, ishaAngle = 17.0),
    UMM_AL_QURA("Umm al-Qura, Makkah", 18.5, ishaMinutes = 90),
    ISNA("ISNA (North America)", 15.0, ishaAngle = 15.0),
    KARACHI("University of Islamic Sciences, Karachi", 18.0, ishaAngle = 18.0),
    DUBAI("Dubai", 18.2, ishaAngle = 18.2),
    KUWAIT("Kuwait", 18.0, ishaAngle = 17.5),
    QATAR("Qatar", 18.0, ishaMinutes = 90),
    SINGAPORE("Singapore / Malaysia / Indonesia", 20.0, ishaAngle = 18.0),
    TURKEY("Diyanet, Turkey", 18.0, ishaAngle = 17.0),
    TEHRAN("Institute of Geophysics, Tehran", 17.7, ishaAngle = 14.0, maghribAngle = 4.5),
}

/** Shadow factor used for Asr. */
enum class AsrMethod(val title: String, val shadowFactor: Int) {
    STANDARD("Standard (Shafi'i, Maliki, Hanbali)", 1),
    HANAFI("Hanafi", 2),
}

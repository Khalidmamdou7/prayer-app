package com.azan.prayer.prayer

/**
 * Twilight conventions used by the major authorities.
 *
 * @param fajrAngle sun depression angle for Fajr, in degrees.
 * @param ishaAngle sun depression angle for Isha, or null when [ishaMinutes] is used.
 * @param ishaMinutes fixed interval after Maghrib for Isha.
 * @param maghribAngle sun depression angle for Maghrib, or null for sunset.
 * @param aladhanId the method's id in the Aladhan API, used for online sync.
 */
enum class CalculationMethod(
    val title: String,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaMinutes: Int = 0,
    val maghribAngle: Double? = null,
    val aladhanId: Int,
) {
    EGYPT("Egyptian General Authority of Survey", 19.5, ishaAngle = 17.5, aladhanId = 5),
    MWL("Muslim World League", 18.0, ishaAngle = 17.0, aladhanId = 3),
    UMM_AL_QURA("Umm al-Qura, Makkah", 18.5, ishaMinutes = 90, aladhanId = 4),
    ISNA("ISNA (North America)", 15.0, ishaAngle = 15.0, aladhanId = 2),
    KARACHI("University of Islamic Sciences, Karachi", 18.0, ishaAngle = 18.0, aladhanId = 1),
    DUBAI("Dubai", 18.2, ishaAngle = 18.2, aladhanId = 16),
    KUWAIT("Kuwait", 18.0, ishaAngle = 17.5, aladhanId = 9),
    QATAR("Qatar", 18.0, ishaMinutes = 90, aladhanId = 10),
    SINGAPORE("Singapore / Malaysia / Indonesia", 20.0, ishaAngle = 18.0, aladhanId = 11),
    TURKEY("Diyanet, Turkey", 18.0, ishaAngle = 17.0, aladhanId = 13),
    TEHRAN("Institute of Geophysics, Tehran", 17.7, ishaAngle = 14.0, maghribAngle = 4.5, aladhanId = 7),
    JORDAN("Jordan", 18.0, ishaAngle = 18.0, aladhanId = 23),
    MOROCCO("Morocco", 19.0, ishaAngle = 17.0, aladhanId = 21),
    ALGERIA("Algeria", 18.0, ishaAngle = 17.0, aladhanId = 19),
    TUNISIA("Tunisia", 18.0, ishaAngle = 18.0, aladhanId = 18),
    JAKIM("JAKIM, Malaysia", 20.0, ishaAngle = 18.0, aladhanId = 17),
    KEMENAG("Kemenag, Indonesia", 20.0, ishaAngle = 18.0, aladhanId = 20),
    FRANCE("UOIF, France", 12.0, ishaAngle = 12.0, aladhanId = 12),
    RUSSIA("Spiritual Administration, Russia", 16.0, ishaAngle = 15.0, aladhanId = 14),

    // Uses seasonal corrections that only the online source applies; offline it approximates with 18°.
    MOONSIGHTING("Moonsighting Committee (UK, North America)", 18.0, ishaAngle = 18.0, aladhanId = 15),
}

/** Shadow factor used for Asr. */
enum class AsrMethod(val title: String, val shadowFactor: Int, val aladhanSchool: Int) {
    STANDARD("Standard (Shafi'i, Maliki, Hanbali)", 1, 0),
    HANAFI("Hanafi", 2, 1),
}

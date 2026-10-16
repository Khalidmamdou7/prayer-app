package com.azan.prayer.prayer

import androidx.annotation.StringRes
import com.azan.prayer.R

/**
 * Twilight conventions used by the major authorities.
 *
 * @param fajrAngle sun depression angle for Fajr, in degrees.
 * @param ishaAngle sun depression angle for Isha, or null when [ishaMinutes] is used.
 * @param ishaMinutes fixed interval after Maghrib for Isha.
 * @param maghribAngle sun depression angle for Maghrib, or null for sunset.
 * @param aladhanId the method's id in the Aladhan API, used for online sync.
 * @param title the display name (translated).
 */
enum class CalculationMethod(
    @StringRes val title: Int,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaMinutes: Int = 0,
    val maghribAngle: Double? = null,
    val aladhanId: Int,
) {
    EGYPT(R.string.method_egypt, 19.5, ishaAngle = 17.5, aladhanId = 5),
    MWL(R.string.method_mwl, 18.0, ishaAngle = 17.0, aladhanId = 3),
    UMM_AL_QURA(R.string.method_umm_al_qura, 18.5, ishaMinutes = 90, aladhanId = 4),
    ISNA(R.string.method_isna, 15.0, ishaAngle = 15.0, aladhanId = 2),
    KARACHI(R.string.method_karachi, 18.0, ishaAngle = 18.0, aladhanId = 1),
    DUBAI(R.string.method_dubai, 18.2, ishaAngle = 18.2, aladhanId = 16),
    KUWAIT(R.string.method_kuwait, 18.0, ishaAngle = 17.5, aladhanId = 9),
    QATAR(R.string.method_qatar, 18.0, ishaMinutes = 90, aladhanId = 10),
    SINGAPORE(R.string.method_singapore, 20.0, ishaAngle = 18.0, aladhanId = 11),
    TURKEY(R.string.method_turkey, 18.0, ishaAngle = 17.0, aladhanId = 13),
    TEHRAN(R.string.method_tehran, 17.7, ishaAngle = 14.0, maghribAngle = 4.5, aladhanId = 7),
    JORDAN(R.string.method_jordan, 18.0, ishaAngle = 18.0, aladhanId = 23),
    MOROCCO(R.string.method_morocco, 19.0, ishaAngle = 17.0, aladhanId = 21),
    ALGERIA(R.string.method_algeria, 18.0, ishaAngle = 17.0, aladhanId = 19),
    TUNISIA(R.string.method_tunisia, 18.0, ishaAngle = 18.0, aladhanId = 18),
    JAKIM(R.string.method_jakim, 20.0, ishaAngle = 18.0, aladhanId = 17),
    KEMENAG(R.string.method_kemenag, 20.0, ishaAngle = 18.0, aladhanId = 20),
    FRANCE(R.string.method_france, 12.0, ishaAngle = 12.0, aladhanId = 12),
    RUSSIA(R.string.method_russia, 16.0, ishaAngle = 15.0, aladhanId = 14),

    // Uses seasonal corrections that only the online source applies; offline it approximates with 18°.
    MOONSIGHTING(R.string.method_moonsighting, 18.0, ishaAngle = 18.0, aladhanId = 15),
}

/** Shadow factor used for Asr. */
enum class AsrMethod(@StringRes val title: Int, val shadowFactor: Int, val aladhanSchool: Int) {
    STANDARD(R.string.asr_standard, 1, 0),
    HANAFI(R.string.asr_hanafi, 2, 1),
}

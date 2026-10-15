package com.azan.prayer.prayer

import androidx.annotation.StringRes
import com.azan.prayer.R

/** The daily times shown in the app. Sunrise is not a prayer but marks the end of Fajr. */
enum class Prayer(@StringRes val label: Int, @StringRes val arabicLabel: Int, val isPrayer: Boolean) {
    FAJR(R.string.fajr, R.string.fajr_ar, true),
    SUNRISE(R.string.sunrise, R.string.sunrise_ar, false),
    DHUHR(R.string.dhuhr, R.string.dhuhr_ar, true),
    ASR(R.string.asr, R.string.asr_ar, true),
    MAGHRIB(R.string.maghrib, R.string.maghrib_ar, true),
    ISHA(R.string.isha, R.string.isha_ar, true);

    companion object {
        val prayers: List<Prayer> = entries.filter { it.isPrayer }
    }
}

package com.azan.prayer.prayer

import androidx.annotation.StringRes
import com.azan.prayer.R

/** The daily times shown in the app. Sunrise is not a prayer but marks the end of Fajr. */
enum class Prayer(@StringRes val label: Int, val isPrayer: Boolean) {
    FAJR(R.string.fajr, true),
    SUNRISE(R.string.sunrise, false),
    DHUHR(R.string.dhuhr, true),
    ASR(R.string.asr, true),
    MAGHRIB(R.string.maghrib, true),
    ISHA(R.string.isha, true);

    companion object {
        val prayers: List<Prayer> = entries.filter { it.isPrayer }
    }
}

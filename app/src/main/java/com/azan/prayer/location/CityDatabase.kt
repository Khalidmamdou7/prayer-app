package com.azan.prayer.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads the bundled city list once per process. */
object CityDatabase {
    private val mutex = Mutex()
    private var search: CitySearch? = null

    suspend fun get(context: Context): CitySearch = mutex.withLock {
        search ?: withContext(Dispatchers.IO) {
            context.applicationContext.assets.open("cities.tsv").reader(Charsets.UTF_8).use { CitySearch(CitySearch.parse(it)) }
        }.also { search = it }
    }
}

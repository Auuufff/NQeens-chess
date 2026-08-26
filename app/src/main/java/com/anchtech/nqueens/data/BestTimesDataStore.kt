package com.anchtech.nqueens.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.domain.repository.BestTimesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for saving best times, one preference key per board size.
 */
@Singleton
class BestTimesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : BestTimesRepository {

    override val bestTimes: Flow<Map<Int, Duration>> = dataStore.data.map { preferences ->
        Constants.BOARD_SIZES
            .mapNotNull { size -> preferences[keyFor(size)]?.let { size to it.milliseconds } }
            .toMap()
    }

    override suspend fun record(size: Int, time: Duration) {
        dataStore.edit { preferences ->
            val key = keyFor(size)
            val stored = preferences[key]
            val candidate = time.inWholeMilliseconds
            if (stored == null || candidate < stored) {
                preferences[key] = candidate
            }
        }
    }

    private fun keyFor(size: Int) = longPreferencesKey("$KEY_PREFIX$size")

    private companion object {
        const val KEY_PREFIX = "best_time_"
    }
}

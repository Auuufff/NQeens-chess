package com.anchtech.nqueens.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One `Long` of milliseconds per board size, and a `Boolean` for the theme.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val bestTimes: Flow<Map<Int, Duration>> = dataStore.data.map { preferences ->
        Constants.BOARD_SIZES
            .mapNotNull { size -> preferences[bestTimeKey(size)]?.let { size to it.milliseconds } }
            .toMap()
    }

    override val darkTheme: Flow<Boolean?> = dataStore.data.map { preferences -> preferences[DARK_THEME_KEY] }

    override suspend fun recordBestTime(size: Int, time: Duration) {
        dataStore.edit { preferences ->
            val key = bestTimeKey(size)
            val stored = preferences[key]
            val candidate = time.inWholeMilliseconds
            if (stored == null || candidate < stored) {
                preferences[key] = candidate
            }
        }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DARK_THEME_KEY] = enabled }
    }

    private fun bestTimeKey(size: Int) = longPreferencesKey("$BEST_TIME_PREFIX$size")

    private companion object {
        const val BEST_TIME_PREFIX = "best_time_"
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }
}

package com.anchtech.nqueens.domain.repository

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for saving app information
 */
interface SettingsRepository {

    /**
     * Board size to the fastest time. Sizes never solved are absent.
     */
    val bestTimes: Flow<Map<Int, Duration>>

    /**
     * `null` while unset, which means follow the system setting.
     */
    val darkTheme: Flow<Boolean?>

    /**
     * Stores [time] for [size], keeping whichever is faster.
     */
    suspend fun recordBestTime(size: Int, time: Duration)

    suspend fun setDarkTheme(enabled: Boolean)
}

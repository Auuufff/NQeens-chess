package com.anchtech.nqueens.testing

import com.anchtech.nqueens.domain.repository.SettingsRepository
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [SettingsRepository] that keeps the faster of two times, like the real one.
 */
class FakeSettingsRepository(
    initialBestTimes: Map<Int, Duration> = emptyMap(),
    initialDarkTheme: Boolean? = null,
) : SettingsRepository {

    private val times = MutableStateFlow(initialBestTimes)
    private val theme = MutableStateFlow(initialDarkTheme)

    override val bestTimes: Flow<Map<Int, Duration>> = times.asStateFlow()

    override val darkTheme: Flow<Boolean?> = theme.asStateFlow()

    /**
     * Every call to [recordBestTime], in order, including ones that did not beat the stored time.
     */
    var recordedCalls: List<Pair<Int, Duration>> = emptyList()
        private set

    override suspend fun recordBestTime(size: Int, time: Duration) {
        recordedCalls = recordedCalls + (size to time)
        val current = times.value[size]
        if (current == null || time < current) {
            times.value = times.value + (size to time)
        }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        theme.value = enabled
    }
}

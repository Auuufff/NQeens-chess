package com.anchtech.nqueens.testing

import com.anchtech.nqueens.domain.repository.BestTimesRepository
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [BestTimesRepository] that keeps the faster of two times, like the real one.
 */
class FakeBestTimesRepository(
    initial: Map<Int, Duration> = emptyMap(),
) : BestTimesRepository {

    private val state = MutableStateFlow(initial)

    override val bestTimes: Flow<Map<Int, Duration>> = state.asStateFlow()

    /**
     * Every call to [record], in order, including ones that did not beat the stored time.
     */
    var recordedCalls: List<Pair<Int, Duration>> = emptyList()
        private set

    override suspend fun record(size: Int, time: Duration) {
        recordedCalls = recordedCalls + (size to time)
        val current = state.value[size]
        if (current == null || time < current) {
            state.value = state.value + (size to time)
        }
    }
}

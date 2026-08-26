package com.anchtech.nqueens.domain.repository

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/** Fastest recorded solve for each board size. */
interface BestTimesRepository {

    /** Board size to the fastest time. Sizes never solved are absent. */
    val bestTimes: Flow<Map<Int, Duration>>

    /** Stores [time] for [size], keeping whichever is faster. */
    suspend fun record(size: Int, time: Duration)
}

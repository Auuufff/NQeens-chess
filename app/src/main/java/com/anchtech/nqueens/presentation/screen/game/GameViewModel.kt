package com.anchtech.nqueens.presentation.screen.game

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.anchtech.nqueens.common.extension.formatAsClock
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.domain.repository.SettingsRepository
import com.anchtech.nqueens.domain.usecase.EvaluatePositionUseCase
import com.anchtech.nqueens.presentation.base.BaseComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives one game on the board size carried by [GameRoute].
 *
 * A failed best-time read or write is logged and ignored rather than surfaced: it cannot
 * affect play, and an error banner over a victory would be worse than a missing record.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val evaluatePositionUseCase: EvaluatePositionUseCase,
    private val settingsRepository: SettingsRepository,
    private val timeSource: TimeSource,
) : BaseComposeViewModel<GameState, GameAction>(GameState()) {

    private val boardSize: Int = savedStateHandle.toRoute<GameRoute>().size

    private var startMark: TimeMark = timeSource.markNow()
    private var timerJob: Job? = null

    init {
        updateState {
            it.copy(
                size = boardSize,
                onCellClick = ::handleCellClick,
                onResetClick = ::handleReset,
                onLeaveClick = ::handleLeave,
            )
        }
        startTimer()
    }

    private fun handleCellClick(square: Square) {
        if (state.value.isSolved) return

        val placing = square !in state.value.queens
        if (placing && state.value.queens.size >= boardSize) {
            return
        }
        updateState {
            val queens = if (placing) it.queens + square else it.queens - square
            val status = evaluatePositionUseCase(boardSize, queens)
            it.copy(queens = queens, conflicts = status.conflicts, isSolved = status.isSolved)
        }

        val action = if (placing) {
            GameAction.QueenPlaced(hasConflict = square in state.value.conflicts)
        } else {
            GameAction.QueenRemoved
        }
        sendAction(action)
        if (state.value.isSolved) {
            finish()
        }
    }

    /**
     * Settles the game in one step: the clock is read once and frozen, and the record is
     * decided against the stored best before the new time overwrites it.
     */
    private fun finish() = launch {
        timerJob?.cancel()
        val elapsed = startMark.elapsedNow()
        val previousBest = settingsRepository.bestTimes.first()[boardSize]

        updateState {
            it.copy(
                time = elapsed.formatAsClock(),
                isNewRecord = previousBest == null || elapsed < previousBest,
                isVictoryVisible = true,
            )
        }
        sendAction(GameAction.Solved)
        settingsRepository.recordBestTime(boardSize, elapsed)
    }

    private fun handleLeave() {
        updateState { it.copy(isVictoryVisible = false) }
        sendAction(GameAction.Leave)
    }

    private fun handleReset() {
        timerJob?.cancel()
        startMark = timeSource.markNow()
        updateState {
            it.copy(
                queens = emptySet(),
                conflicts = emptySet(),
                isSolved = false,
                isVictoryVisible = false,
                time = Duration.ZERO.formatAsClock(),
                isNewRecord = false,
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = launch {
            while (true) {
                val elapsed = startMark.elapsedNow()
                val formatted = elapsed.formatAsClock()
                updateState { it.copy(time = formatted) }

                val tickMillis = TICK.inWholeMilliseconds
                delay((tickMillis - elapsed.inWholeMilliseconds % tickMillis).milliseconds)
            }
        }
    }

    private companion object {
        val TICK = 1.seconds
    }
}

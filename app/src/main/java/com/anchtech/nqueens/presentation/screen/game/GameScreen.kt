package com.anchtech.nqueens.presentation.screen.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchtech.nqueens.R
import com.anchtech.nqueens.common.extension.collectAsEffect
import com.anchtech.nqueens.common.extension.rememberSingleClick
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.presentation.component.Zoomable
import com.anchtech.nqueens.presentation.screen.game.components.GameBoard
import com.anchtech.nqueens.presentation.screen.game.components.QueensLeft
import com.anchtech.nqueens.presentation.screen.game.components.VictoryOverlay
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import com.anchtech.nqueens.presentation.theme.TimerDisplay

@Composable
internal fun GameScreen(
    onBackClick: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    viewModel.action.collectAsEffect { action ->
        when (action) {
            GameAction.QueenPlaced -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            GameAction.QueenRemoved -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            GameAction.Solved -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    GameScreenContent(state = state, onBackClick = onBackClick)
}

@Composable
private fun GameScreenContent(
    state: GameState,
    onBackClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .safeDrawingPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = rememberSingleClick(onBackClick)) {
                    Text(stringResource(R.string.game_back))
                }
                TextButton(onClick = rememberSingleClick(state.onResetClick)) {
                    Text(stringResource(R.string.game_reset))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = state.time, style = TimerDisplay)
                QueensLeft(count = state.queensLeft)
            }

            Zoomable(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                GameBoard(
                    boardSize = state.size,
                    queens = state.queens,
                    conflicts = state.conflicts,
                    onCellClick = state.onCellClick,
                )
            }
        }

        VictoryOverlay(
            visible = state.isSolved,
            time = state.time,
            isNewRecord = state.isNewRecord,
            onPlayAgainClick = rememberSingleClick(state.onResetClick),
            onBackClick = rememberSingleClick(onBackClick),
        )
    }
}

@PreviewLightDark
@Composable
private fun GameScreenPreview() {
    val queens = setOf(Square(0, 1), Square(1, 3), Square(2, 3))

    NQueensTheme {
        GameScreenContent(
            state = GameState(
                size = 6,
                queens = queens,
                conflicts = setOf(Square(1, 3), Square(2, 3)),
                time = "01:24",
            ),
            onBackClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun GameScreenSolvedPreview() {
    val solution = setOf(Square(0, 1), Square(1, 3), Square(2, 0), Square(3, 2))

    NQueensTheme {
        GameScreenContent(
            state = GameState(
                size = 4,
                queens = solution,
                isSolved = true,
                time = "00:42",
                isNewRecord = true,
            ),
            onBackClick = {},
        )
    }
}

package com.anchtech.nqueens.presentation.screen.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchtech.nqueens.R
import com.anchtech.nqueens.common.extension.collectAsEffect
import com.anchtech.nqueens.common.extension.isLandscape
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
    val feedback = rememberGameFeedback()

    viewModel.action.collectAsEffect { action ->
        when (action) {
            is GameAction.QueenPlaced -> if (action.hasConflict) feedback.queenClashed() else feedback.queenPlaced()
            GameAction.QueenRemoved -> feedback.queenRemoved()
            GameAction.Solved -> feedback.solved()
        }
    }

    GameScreenContent(state = state, onBackClick = onBackClick)
}

@Composable
internal fun GameScreenContent(
    state: GameState,
    onBackClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        if (isLandscape()) {
            GameScreenLandscape(state = state, onBackClick = onBackClick)
        } else {
            GameScreenPortrait(state = state, onBackClick = onBackClick)
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

@Composable
private fun GameScreenPortrait(
    state: GameState,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
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
            BackButton(onClick = onBackClick)
            ResetButton(onClick = state.onResetClick)
        }

        GameStatus(
            time = state.time,
            queensLeft = state.queensLeft,
            modifier = Modifier.fillMaxWidth(),
        )

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
}

@Composable
private fun GameScreenLandscape(
    state: GameState,
    onBackClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Zoomable(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            GameBoard(
                boardSize = state.size,
                queens = state.queens,
                conflicts = state.conflicts,
                onCellClick = state.onCellClick,
                modifier = Modifier.fillMaxHeight(),
            )
        }

        Column(
            modifier = Modifier
                .width(192.dp)
                .fillMaxHeight()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameStatus(
                time = state.time,
                queensLeft = state.queensLeft,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                ResetButton(onClick = state.onResetClick, modifier = Modifier.fillMaxWidth())
                BackButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GameStatus(
    time: String,
    queensLeft: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = time, style = TimerDisplay)
        QueensLeft(count = queensLeft)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = rememberSingleClick(onClick), modifier = modifier) {
        Text(stringResource(R.string.game_back))
    }
}

@Composable
private fun ResetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = rememberSingleClick(onClick), modifier = modifier) {
        Text(stringResource(R.string.game_reset))
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

@Preview(name = "Landscape", widthDp = 800, heightDp = 360)
@Preview(name = "Landscape dark", widthDp = 800, heightDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameScreenLandscapePreview() {
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

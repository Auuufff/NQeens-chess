package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.anchtech.nqueens.R
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.presentation.theme.BoardColors
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import com.anchtech.nqueens.presentation.theme.boardColors

private val ConflictFadeSpec = tween<Float>(durationMillis = 180)

private val PlaceSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

private val RemoveSpec = tween<Float>(durationMillis = 140)

/**
 * The playable board: a checkerboard layer and a pieces layer
 */
@Composable
fun GameBoard(
    boardSize: Int,
    queens: Set<Square>,
    conflicts: Set<Square>,
    onCellClick: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.game_board, boardSize, queens.size)
    val currentOnCellClick by rememberUpdatedState(onCellClick)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .pointerInput(boardSize) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    squareAt(up.position, size.width.toFloat(), boardSize)?.let(currentOnCellClick)
                }
            }
            .semantics { contentDescription = description },
    ) {
        BoardBackground(boardSize)
        BoardPieces(boardSize, queens, conflicts)
    }
}

/**
 * The empty checkerboard.
 */
@Composable
private fun BoxScope.BoardBackground(boardSize: Int) {
    val colors = MaterialTheme.boardColors

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer(),
    ) {
        val cell = size.width / boardSize
        val size = Size(width = cell, height = cell)

        // The light squares are the background, so only the dark half is drawn.
        drawRect(color = colors.lightSquare)

        for (row in 0 until boardSize) {
            val offsetY = row * cell
            for (col in (row + 1) % 2 until boardSize step 2) {
                val offsetX = col * cell
                drawRect(color = colors.darkSquare, topLeft = Offset(x = offsetX, y = offsetY), size = size)
            }
        }
    }
}

/**
 * The queens and the conflict tint, drawn over [BoardBackground].
 */
@Composable
private fun BoxScope.BoardPieces(boardSize: Int, queens: Set<Square>, conflicts: Set<Square>) {
    val colors = MaterialTheme.boardColors
    val conflictAlpha = animateFloatAsState(
        targetValue = if (conflicts.isEmpty()) 0f else 1f,
        animationSpec = ConflictFadeSpec,
        label = "conflict",
    )
    val queenScales = rememberQueenScales(queens)

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer(),
    ) {
        val cell = size.width / boardSize
        val size = Size(width = cell, height = cell)

        conflicts.forEach { square ->
            drawConflict(
                square = square,
                cell = cell,
                size = size,
                colors = colors,
                alpha = conflictAlpha.value,
            )
        }
        queenScales.forEach { (square, scale) ->
            drawQueen(
                square = square,
                cell = cell,
                colors = colors,
                progress = scale.value,
            )
        }
    }
}

/**
 * The scale of every queen on the board, keyed by square, plus any that are still animating
 * off it. A square leaves the map once it has finished retreating.
 */
@Composable
private fun rememberQueenScales(queens: Set<Square>): Map<Square, State<Float>> {
    var drawn by remember { mutableStateOf(queens) }
    val entering = queens - drawn
    if (entering.isNotEmpty()) {
        drawn += entering
    }

    return drawn.associateWith { square ->
        key(square) {
            val isOccupied = square in queens
            val scale = remember { Animatable(if (square in entering) 0f else 1f) }

            LaunchedEffect(isOccupied) {
                scale.animateTo(
                    targetValue = if (isOccupied) 1f else 0f,
                    animationSpec = if (isOccupied) PlaceSpec else RemoveSpec,
                )
                if (!isOccupied) drawn -= square
            }

            scale.asState()
        }
    }
}

private fun DrawScope.drawConflict(square: Square, cell: Float, size: Size, colors: BoardColors, alpha: Float) {
    drawRect(
        color = colors.squareAt(square.row, square.col, isConflicting = true),
        topLeft = Offset(x = square.col * cell, y = square.row * cell),
        size = size,
        alpha = alpha,
    )
}

@PreviewLightDark
@Composable
private fun GameBoardPreview() {
    NQueensTheme {
        Surface {
            Box(modifier = Modifier.size(320.dp)) {
                GameBoard(
                    boardSize = 8,
                    queens = setOf(Square(0, 0), Square(1, 4), Square(2, 7), Square(3, 5), Square(4, 2)),
                    conflicts = setOf(Square(3, 5), Square(4, 2)),
                    onCellClick = {},
                )
            }
        }
    }
}

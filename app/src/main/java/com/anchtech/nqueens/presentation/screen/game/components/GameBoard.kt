package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * The playable board: a checkerboard layer and a pieces layer
 */
@Composable
fun GameBoard(
    boardSize: Int,
    queens: Set<Square>,
    conflicts: Set<Square>,
    onCellClick: (Square) -> Unit,
) {
    val description = stringResource(R.string.game_board, boardSize, queens.size)
    val currentOnCellClick by rememberUpdatedState(onCellClick)

    Box(
        modifier = Modifier
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
        queens.forEach { square ->
            drawQueen(
                square = square,
                cell = cell,
                colors = colors,
            )
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

/**
 * The square at [tap], or null if the tap missed the board. The bounds check is on the
 * offset, not on the derived row and column: `(-0.5f).toInt()` is 0 in Kotlin, so truncating
 * first would put a tap above the board onto row 0.
 */
private fun squareAt(tap: Offset, board: Float, boardSize: Int): Square? {
    if (tap.x < 0f || tap.y < 0f || tap.x >= board || tap.y >= board) {
        return null
    }

    val cell = board / boardSize
    return Square(row = (tap.y / cell).toInt(), col = (tap.x / cell).toInt())
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

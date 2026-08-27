package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.presentation.theme.BoardColors

/**
 * Queen height as a fraction of a cell, leaving the piece a margin inside its square.
 */
private const val QUEEN_SIZE_RATIO = 0.72f

private const val QUEEN_WIDTH = 84f

private const val QUEEN_HEIGHT = 100f

/**
 * Draws the queen centred on [square], on a board whose squares are [cell] wide.
 */
internal fun DrawScope.drawQueen(square: Square, cell: Float, colors: BoardColors) {
    val pieceScale = cell * QUEEN_SIZE_RATIO / QUEEN_HEIGHT

    withTransform(
        {
            translate(
                left = square.col * cell + (cell - QUEEN_WIDTH * pieceScale) / 2f,
                top = square.row * cell + (cell - QUEEN_HEIGHT * pieceScale) / 2f,
            )
            scale(scaleX = pieceScale, scaleY = pieceScale, pivot = Offset.Zero)
        },
    ) {
        drawPath(path = QueenPath, color = colors.queenOn(square.row, square.col))
    }
}

/**
 * The queen: five balled points on a crown, a flared body, and a base. Its ink fills the
 * [QUEEN_WIDTH] by [QUEEN_HEIGHT] box exactly, so scaling the box scales the piece with no
 * margin of its own to allow for.
 *
 * `res/drawable/ic_queen.xml` is the same piece as a drawable, for the one place that wants a
 * painter rather than a draw call. Redraw one and redraw the other.
 */
private val QueenPath = Path().apply {
    addOval(ball(x = 11f, y = 18f), direction = Path.Direction.Clockwise)
    addOval(ball(x = 26.5f, y = 10f), direction = Path.Direction.Clockwise)
    addOval(ball(x = 42f, y = 7f), direction = Path.Direction.Clockwise)
    addOval(ball(x = 57.5f, y = 10f), direction = Path.Direction.Clockwise)
    addOval(ball(x = 73f, y = 18f), direction = Path.Direction.Clockwise)

    // The crown: up to each ball in turn, down into the notch between them, and back along
    // the collar the body meets.
    moveTo(11f, 18f)
    lineTo(18.75f, 32f)
    lineTo(26.5f, 10f)
    lineTo(34.25f, 30f)
    lineTo(42f, 7f)
    lineTo(49.75f, 30f)
    lineTo(57.5f, 10f)
    lineTo(65.25f, 32f)
    lineTo(73f, 18f)
    lineTo(73f, 42f)
    lineTo(11f, 42f)
    close()

    // The body, held in under the collar and flaring onto the base.
    moveTo(60f, 40f)
    quadraticTo(61f, 74f, 76f, 88f)
    lineTo(8f, 88f)
    quadraticTo(23f, 74f, 24f, 40f)
    close()

    addRoundRect(
        RoundRect(
            rect = Rect(left = 0f, top = 86f, right = QUEEN_WIDTH, bottom = QUEEN_HEIGHT),
            cornerRadius = CornerRadius(3f),
        ),
        direction = Path.Direction.Clockwise,
    )
}

private fun ball(x: Float, y: Float) = Rect(center = Offset(x, y), radius = 7f)

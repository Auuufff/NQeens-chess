package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.ui.geometry.Offset
import com.anchtech.nqueens.domain.model.Square

/**
 * Returns the square containing [tap], or `null` if the tap fell outside the board.
 *
 * @param tap a point in board coordinates, measured from the board's top-left corner
 * @param board the board's edge length in pixels
 * @param boardSize the board's edge length in squares
 */
internal fun squareAt(tap: Offset, board: Float, boardSize: Int): Square? {
    if (tap.x < 0f || tap.y < 0f || tap.x >= board || tap.y >= board) {
        return null
    }

    val cell = board / boardSize
    return Square(row = (tap.y / cell).toInt(), col = (tap.x / cell).toInt())
}

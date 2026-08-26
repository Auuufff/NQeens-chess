package com.anchtech.nqueens.presentation.screen.setup.model

/**
 * One row of the record board: a board size and its fastest solve, ready to render.
 */
data class UiBestTime(
    val size: Int,
    val time: String,
)

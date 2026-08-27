package com.anchtech.nqueens.presentation.screen.game

import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.presentation.base.BaseState

/**
 * Everything the game screen renders, plus the callbacks it invokes.
 */
data class GameState(
    val size: Int = Constants.DEFAULT_BOARD_SIZE,
    val queens: Set<Square> = emptySet(),
    val conflicts: Set<Square> = emptySet(),
    val isSolved: Boolean = false,
    val time: String = "00:00",
    val isNewRecord: Boolean = false,
    val onCellClick: (Square) -> Unit = {},
    val onResetClick: () -> Unit = {},
) : BaseState {

    val queensLeft: Int = size - queens.size
}

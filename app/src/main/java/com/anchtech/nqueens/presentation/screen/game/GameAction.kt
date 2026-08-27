package com.anchtech.nqueens.presentation.screen.game

import com.anchtech.nqueens.presentation.base.BaseAction

sealed interface GameAction : BaseAction {

    /**
     * A queen was placed.
     */
    data object QueenPlaced : GameAction

    /**
     * A queen was taken off the board.
     */
    data object QueenRemoved : GameAction

    /**
     * The puzzle was solved. The overlay renders from state; this is the celebration only.
     */
    data object Solved : GameAction
}

package com.anchtech.nqueens.presentation.screen.setup

import com.anchtech.nqueens.presentation.base.BaseAction

sealed interface SetupAction : BaseAction {

    /**
     * Start a game on the selected board.
     */
    data class StartGame(val size: Int) : SetupAction
}

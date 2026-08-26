package com.anchtech.nqueens.presentation.screen.setup

import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.presentation.base.BaseState
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime

/**
 * Board size selection and the record board.
 */
data class SetupState(
    val sizes: IntRange = Constants.BOARD_SIZES,
    val selectedSize: Int = Constants.DEFAULT_BOARD_SIZE,
    val records: List<UiBestTime> = emptyList(),
    val onSizeSelected: (Int) -> Unit = {},
    val onStartClick: () -> Unit = {},
) : BaseState

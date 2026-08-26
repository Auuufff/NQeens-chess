package com.anchtech.nqueens.domain.model

/**
 * The result of evaluating one arrangement of queens.
 */
data class PositionStatus(
    /**
     * Queens that attack, or are attacked by, at least one other queen.
     */
    val conflicts: Set<Square> = emptySet(),
    /**
     * True when every queen is placed and none of them conflict.
     */
    val isSolved: Boolean = false,
)

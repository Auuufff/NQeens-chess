package com.anchtech.nqueens.domain.model

/**
 * Everything the rules have to say about one arrangement of queens.
 *
 * Both facts come from a single evaluation: [isSolved] cannot be decided without knowing
 * [conflicts], so returning them together avoids scanning the board twice.
 */
data class PositionStatus(
    /** Queens that attack, or are attacked by, at least one other queen. */
    val conflicts: Set<Square> = emptySet(),
    /** True when every queen is placed and none of them conflict. */
    val isSolved: Boolean = false,
)

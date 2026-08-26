package com.anchtech.nqueens.domain.usecase

import com.anchtech.nqueens.domain.model.PositionStatus
import com.anchtech.nqueens.domain.model.Square
import javax.inject.Inject
import kotlin.math.abs

/**
 * The rules of N-Queens.
 *
 * Placement never fails: conflicts are reported, not rejected.
 */
class EvaluatePositionUseCase @Inject constructor() {

    /**
     * @param size the board's edge length, needed to decide whether the puzzle is complete
     * @param queens every occupied square
     */
    operator fun invoke(size: Int, queens: Set<Square>): PositionStatus {
        val conflicts = queens.filterTo(mutableSetOf()) { queen ->
            queens.any { it != queen && threatens(it, queen) }
        }
        return PositionStatus(
            conflicts = conflicts,
            isSolved = queens.size == size && conflicts.isEmpty(),
        )
    }

    /**
     * Two queens threaten each other along a shared row, column or diagonal.
     */
    private fun threatens(a: Square, b: Square): Boolean =
        a.row == b.row || a.col == b.col || abs(a.row - b.row) == abs(a.col - b.col)
}

package com.anchtech.nqueens.domain.usecase

import com.anchtech.nqueens.domain.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatePositionUseCaseTest {

    private val evaluate = EvaluatePositionUseCase()

    // ---- the threat rule, one clause at a time ----------------------------------------

    @Test
    fun `queens sharing a row both conflict`() {
        val queens = setOf(Square(0, 0), Square(0, 3))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }

    @Test
    fun `queens sharing a column both conflict`() {
        val queens = setOf(Square(0, 2), Square(3, 2))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }

    @Test
    fun `queens sharing a descending diagonal both conflict`() {
        val queens = setOf(Square(0, 0), Square(3, 3))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }

    @Test
    fun `queens sharing an ascending diagonal both conflict`() {
        val queens = setOf(Square(0, 3), Square(3, 0))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }

    @Test
    fun `queens sharing no row column or diagonal do not conflict`() {
        val queens = setOf(Square(0, 0), Square(1, 2))

        assertEquals(emptySet<Square>(), evaluate(size = 4, queens = queens).conflicts)
    }

    // ---- diagonal boundaries -----------------------------------------------------------

    @Test
    fun `diagonally adjacent queens conflict`() {
        val queens = setOf(Square(2, 2), Square(3, 3))

        assertEquals(queens, evaluate(size = 8, queens = queens).conflicts)
    }

    @Test
    fun `queens at opposite corners of a long diagonal conflict`() {
        val queens = setOf(Square(0, 0), Square(11, 11))

        assertEquals(queens, evaluate(size = 12, queens = queens).conflicts)
    }

    @Test
    fun `a one-square offset from a diagonal is safe`() {
        // (0,0) attacks (3,3); (3,4) is one file past it.
        val queens = setOf(Square(0, 0), Square(3, 4))

        assertEquals(emptySet<Square>(), evaluate(size = 8, queens = queens).conflicts)
    }

    // ---- a queen is never its own attacker ---------------------------------------------

    @Test
    fun `empty board has no conflicts`() {
        assertEquals(emptySet<Square>(), evaluate(size = 8, queens = emptySet()).conflicts)
    }

    @Test
    fun `a lone queen conflicts with nothing`() {
        // Shares its own row and column with itself; the rule must exclude self-comparison.
        assertEquals(emptySet<Square>(), evaluate(size = 8, queens = setOf(Square(3, 3))).conflicts)
    }

    // ---- which queens get reported ------------------------------------------------------

    @Test
    fun `a queen attacked on two axes is reported once`() {
        val attacked = Square(2, 2)
        val queens = setOf(attacked, Square(2, 0), Square(0, 0))

        val conflicts = evaluate(size = 4, queens = queens).conflicts

        assertEquals(queens, conflicts)
        assertEquals(3, conflicts.size)
    }

    @Test
    fun `only the queens involved in a conflict are reported`() {
        val safe = Square(3, 2)
        val queens = setOf(Square(0, 0), Square(1, 1), safe)

        val conflicts = evaluate(size = 8, queens = queens).conflicts

        assertEquals(setOf(Square(0, 0), Square(1, 1)), conflicts)
        assertFalse(safe in conflicts)
    }

    @Test
    fun `two independent conflicting pairs are all reported`() {
        val pairOne = setOf(Square(0, 0), Square(0, 1)) // same row
        val pairTwo = setOf(Square(5, 3), Square(7, 3)) // same column
        val queens = pairOne + pairTwo

        assertEquals(queens, evaluate(size = 8, queens = queens).conflicts)
    }

    @Test
    fun `conflict is symmetric so both ends of a pair are reported`() {
        val attacker = Square(0, 0)
        val attacked = Square(0, 5)

        val conflicts = evaluate(size = 8, queens = setOf(attacker, attacked)).conflicts

        assertTrue(attacker in conflicts)
        assertTrue(attacked in conflicts)
    }

    // ---- isSolved -----------------------------------------------------------------------

    @Test
    fun `a valid full board is solved`() {
        val solution = setOf(Square(0, 1), Square(1, 3), Square(2, 0), Square(3, 2))

        val status = evaluate(size = 4, queens = solution)

        assertTrue(status.isSolved)
        assertEquals(emptySet<Square>(), status.conflicts)
    }

    @Test
    fun `a conflict-free board with too few queens is not solved`() {
        val status = evaluate(size = 4, queens = setOf(Square(0, 1), Square(1, 3)))

        assertFalse(status.isSolved)
        assertEquals(emptySet<Square>(), status.conflicts)
    }

    @Test
    fun `a full board with a conflict is not solved`() {
        val queens = setOf(Square(0, 0), Square(1, 1), Square(2, 2), Square(3, 3))

        assertFalse(evaluate(size = 4, queens = queens).isSolved)
    }

    @Test
    fun `one queen short of a solution is not solved`() {
        val almost = setOf(Square(0, 1), Square(1, 3), Square(2, 0))

        val status = evaluate(size = 4, queens = almost)

        assertFalse(status.isSolved)
        assertEquals(emptySet<Square>(), status.conflicts)
    }

    @Test
    fun `more queens than the board needs is never solved`() {
        val solution = setOf(Square(0, 1), Square(1, 3), Square(2, 0), Square(3, 2))

        val status = evaluate(size = 4, queens = solution + Square(3, 3))

        assertFalse(status.isSolved)
        assertTrue(status.conflicts.isNotEmpty())
    }

    @Test
    fun `an empty board is not solved`() {
        assertFalse(evaluate(size = 4, queens = emptySet()).isSolved)
    }

    // ---- board extremes -------------------------------------------------------------------

    @Test
    fun `every square occupied reports every queen and is not solved`() {
        val queens = (0 until 4).flatMap { row -> (0 until 4).map { col -> Square(row, col) } }.toSet()

        val status = evaluate(size = 4, queens = queens)

        assertEquals(16, status.conflicts.size)
        assertFalse(status.isSolved)
    }

    @Test
    fun `a single queen solves a one by one board`() {
        // Smaller than the game offers, but the rule must not special-case it.
        val status = evaluate(size = 1, queens = setOf(Square(0, 0)))

        assertTrue(status.isSolved)
        assertEquals(emptySet<Square>(), status.conflicts)
    }

    @Test
    fun `a two by two board cannot be solved`() {
        val allPairs = listOf(
            setOf(Square(0, 0), Square(0, 1)),
            setOf(Square(0, 0), Square(1, 0)),
            setOf(Square(0, 0), Square(1, 1)),
            setOf(Square(0, 1), Square(1, 0)),
            setOf(Square(0, 1), Square(1, 1)),
            setOf(Square(1, 0), Square(1, 1)),
        )

        allPairs.forEach { queens ->
            assertFalse("expected a conflict for $queens", evaluate(size = 2, queens = queens).isSolved)
        }
    }

    @Test
    fun `a board of size zero is solved when nothing is placed`() {
        // Unreachable through the UI, but pinned so the boundary is a decision, not an accident.
        assertTrue(evaluate(size = 0, queens = emptySet()).isSolved)
    }

    @Test
    fun `a board of the largest offered size behaves like any other`() {
        val queens = setOf(Square(0, 0), Square(11, 0))

        assertEquals(queens, evaluate(size = 12, queens = queens).conflicts)
    }

    // ---- contract boundaries ---------------------------------------------------------------

    @Test
    fun `coordinates outside the board are evaluated not rejected`() {
        // Bounds are the caller's contract: the grid only ever produces valid squares, so the
        // rule stays total and has no failure path.
        val queens = setOf(Square(0, 0), Square(99, 99))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }

    @Test
    fun `negative coordinates follow the same rule`() {
        val queens = setOf(Square(-1, -1), Square(1, 1))

        assertEquals(queens, evaluate(size = 4, queens = queens).conflicts)
    }
}

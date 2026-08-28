package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.ui.geometry.Offset
import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.domain.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The tap-to-square hit test. An 8x8 board 800px wide, so a cell is 100px.
 */
class BoardGeometryTest {

    private val board = 800f

    private val boardSize = 8

    private val cell = board / boardSize

    private fun squareAt(x: Float, y: Float, board: Float = this.board, boardSize: Int = this.boardSize) =
        squareAt(Offset(x, y), board, boardSize)

    // ---- which square a tap lands on ---------------------------------------------------

    @Test
    fun `a tap in the first cell is the top-left square`() {
        assertEquals(Square(row = 0, col = 0), squareAt(x = 50f, y = 50f))
    }

    @Test
    fun `a tap in the last cell is the bottom-right square`() {
        assertEquals(Square(row = 7, col = 7), squareAt(x = 750f, y = 750f))
    }

    @Test
    fun `x selects the column and y selects the row`() {
        assertEquals(Square(row = 2, col = 5), squareAt(x = 550f, y = 250f))
    }

    @Test
    fun `the very first point of the board is the first square`() {
        assertEquals(Square(row = 0, col = 0), squareAt(x = 0f, y = 0f))
    }

    @Test
    fun `every cell centre maps to its own square`() {
        val squares = (0 until boardSize).flatMap { row ->
            (0 until boardSize).map { col ->
                squareAt(x = col * cell + cell / 2f, y = row * cell + cell / 2f)
            }
        }

        assertEquals((0 until boardSize).flatMap { row -> (0 until boardSize).map { Square(row, it) } }, squares)
    }

    // ---- cell boundaries ---------------------------------------------------------------

    @Test
    fun `the last point before a boundary belongs to the earlier cell`() {
        assertEquals(Square(row = 0, col = 0), squareAt(x = cell - 0.01f, y = cell - 0.01f))
    }

    @Test
    fun `a boundary itself belongs to the later cell`() {
        assertEquals(Square(row = 1, col = 1), squareAt(x = cell, y = cell))
    }

    @Test
    fun `the last point inside the board is the last square`() {
        assertEquals(Square(row = 7, col = 7), squareAt(x = board - 0.01f, y = board - 0.01f))
    }

    // ---- taps that miss ----------------------------------------------------------------

    @Test
    fun `a tap above the board misses rather than landing on row zero`() {
        assertNull(squareAt(x = 50f, y = -0.5f))
    }

    @Test
    fun `a tap left of the board misses rather than landing on column zero`() {
        assertNull(squareAt(x = -0.5f, y = 50f))
    }

    @Test
    fun `a tap well above the board misses`() {
        assertNull(squareAt(x = 50f, y = -400f))
    }

    @Test
    fun `the far edge is off the board`() {
        assertNull(squareAt(x = board, y = 50f))
    }

    @Test
    fun `the bottom edge is off the board`() {
        assertNull(squareAt(x = 50f, y = board))
    }

    @Test
    fun `a tap past the right edge misses`() {
        assertNull(squareAt(x = board + 1f, y = 50f))
    }

    @Test
    fun `a tap past the bottom edge misses`() {
        assertNull(squareAt(x = 50f, y = board + 1f))
    }

    // ---- other board sizes -------------------------------------------------------------

    @Test
    fun `the smallest offered board divides into its cells`() {
        val size = Constants.MIN_BOARD_SIZE

        assertEquals(Square(row = 3, col = 0), squareAt(x = 10f, y = 350f, board = 400f, boardSize = size))
    }

    @Test
    fun `the largest offered board divides into its cells`() {
        val size = Constants.MAX_BOARD_SIZE
        val edge = 27f * 30f

        assertEquals(Square(row = 26, col = 26), squareAt(x = edge - 1f, y = edge - 1f, board = edge, boardSize = size))
    }

    @Test
    fun `a board whose cells do not divide evenly still ends on the last square`() {
        assertEquals(Square(row = 6, col = 6), squareAt(x = 999f, y = 999f, board = 1_000f, boardSize = 7))
    }
}

package com.anchtech.nqueens.presentation.screen.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anchtech.nqueens.R
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.testing.clickSquare
import com.anchtech.nqueens.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port")
class GameScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun setGame(state: GameState) {
        compose.setScreen {
            GameScreenContent(state = state)
        }
    }

    private fun board(size: Int, queens: Int) =
        compose.onNodeWithContentDescription(string(R.string.game_board, size, queens))

    // ---- the board ---------------------------------------------------------------------

    @Test
    fun `the board reports its size and how many queens are on it`() {
        setGame(GameState(size = 6, queens = setOf(Square(0, 1), Square(1, 3))))

        board(size = 6, queens = 2).assertIsDisplayed()
    }

    @Test
    fun `a tap is reported as the square it landed on`() {
        var clicked: Square? = null
        setGame(GameState(size = 8, onCellClick = { clicked = it }))

        board(size = 8, queens = 0).clickSquare(Square(row = 2, col = 5), boardSize = 8)

        assertEquals(Square(row = 2, col = 5), clicked)
    }

    @Test
    fun `taps on opposite corners are told apart`() {
        val clicked = mutableListOf<Square>()
        setGame(GameState(size = 4, onCellClick = clicked::add))

        board(size = 4, queens = 0).clickSquare(Square(row = 0, col = 0), boardSize = 4)
        board(size = 4, queens = 0).clickSquare(Square(row = 3, col = 3), boardSize = 4)

        assertEquals(listOf(Square(row = 0, col = 0), Square(row = 3, col = 3)), clicked)
    }

    @Test
    fun `the counter shows how many queens are still to place`() {
        setGame(GameState(size = 8, queens = setOf(Square(0, 0), Square(1, 2), Square(2, 4))))

        compose.onNodeWithContentDescription("5 ${string(R.string.game_queens_left)}").assertIsDisplayed()
    }

    @Test
    fun `the counter reaches zero on a full board`() {
        val queens = setOf(Square(0, 1), Square(1, 3), Square(2, 0), Square(3, 2))
        setGame(GameState(size = 4, queens = queens))

        compose.onNodeWithContentDescription("0 ${string(R.string.game_queens_left)}").assertIsDisplayed()
    }

    // ---- the controls ------------------------------------------------------------------

    @Test
    fun `reset is wired to the reset callback`() {
        var resets = 0
        setGame(GameState(onResetClick = { resets++ }))

        compose.onNodeWithText(string(R.string.game_reset)).performClick()

        assertEquals(1, resets)
    }

    @Test
    fun `back is wired to the leave callback`() {
        var backs = 0
        setGame(GameState(onLeaveClick = { backs++ }))

        compose.onNodeWithText(string(R.string.game_back)).performClick()

        assertEquals(1, backs)
    }

    // ---- the victory overlay -----------------------------------------------------------

    @Test
    fun `the overlay stays hidden until the board is solved`() {
        setGame(GameState(size = 4, isSolved = false))

        compose.onNodeWithText(string(R.string.game_solved)).assertDoesNotExist()
    }

    @Test
    fun `solving the board raises the overlay`() {
        setGame(GameState(size = 4, isSolved = true, isVictoryVisible = true))

        compose.onNodeWithText(string(R.string.game_solved)).assertIsDisplayed()
    }

    @Test
    fun `a solved board whose overlay was taken down shows no overlay`() {
        setGame(GameState(size = 4, isSolved = true, isVictoryVisible = false))

        compose.onNodeWithText(string(R.string.game_solved)).assertDoesNotExist()
    }

    @Test
    fun `the overlay shows the finishing time`() {
        setGame(GameState(size = 4, isVictoryVisible = true, time = "00:42"))

        compose.onNodeWithText(string(R.string.game_your_time)).assertIsDisplayed()
        compose.onNode(hasText("00:42") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test
    fun `a new record is badged`() {
        setGame(GameState(size = 4, isVictoryVisible = true, isNewRecord = true))

        compose.onNodeWithText(string(R.string.game_new_record)).assertIsDisplayed()
    }

    @Test
    fun `a solve that is not a record is not badged`() {
        setGame(GameState(size = 4, isVictoryVisible = true, isNewRecord = false))

        compose.onNodeWithText(string(R.string.game_new_record)).assertDoesNotExist()
    }

    @Test
    fun `play again is wired to the reset callback`() {
        var resets = 0
        setGame(GameState(size = 4, isVictoryVisible = true, onResetClick = { resets++ }))

        compose.onNodeWithText(string(R.string.game_play_again)).performClick()

        assertEquals(1, resets)
    }

    @Test
    fun `back to menu is wired to the leave callback`() {
        var backs = 0
        setGame(GameState(size = 4, isVictoryVisible = true, onLeaveClick = { backs++ }))

        compose.onNodeWithText(string(R.string.game_back_to_menu)).performClick()

        assertEquals(1, backs)
    }

    // ---- orientation -------------------------------------------------------------------

    @Test
    fun `portrait stacks the controls above the board`() {
        setGame(GameState(size = 6))

        val resetBounds = compose.onNodeWithText(string(R.string.game_reset)).getUnclippedBoundsInRoot()
        val boardBounds = board(size = 6, queens = 0).getUnclippedBoundsInRoot()

        assertTrue("reset=$resetBounds board=$boardBounds", resetBounds.bottom <= boardBounds.top)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `landscape puts the controls beside the board`() {
        setGame(GameState(size = 6))

        val boardBounds = board(size = 6, queens = 0).getUnclippedBoundsInRoot()
        val resetBounds = compose.onNodeWithText(string(R.string.game_reset)).getUnclippedBoundsInRoot()

        assertTrue("board=$boardBounds reset=$resetBounds", boardBounds.right <= resetBounds.left)
    }
}

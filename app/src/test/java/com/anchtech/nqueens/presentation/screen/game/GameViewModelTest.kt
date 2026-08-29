package com.anchtech.nqueens.presentation.screen.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.domain.usecase.EvaluatePositionUseCase
import com.anchtech.nqueens.testing.FakeSettingsRepository
import com.anchtech.nqueens.testing.MainDispatcherRule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric: `SavedStateHandle.toRoute()` and the `SavedStateHandle(route)`
 * test factory both decode through `android.net.Uri` and a `Bundle`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val timeSource = TestTimeSource()
    private lateinit var settings: FakeSettingsRepository
    private var created: GameViewModel? = null

    /** The 4x4 solution used throughout; no intermediate placement conflicts. */
    private val solution = listOf(Square(0, 1), Square(1, 3), Square(2, 0), Square(3, 2))

    /** The timer runs forever, so the scope is cancelled inside the test body. */
    private fun gameTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            body()
        } finally {
            created?.viewModelScope?.cancel()
        }
    }

    private fun viewModel(size: Int = 4, stored: Map<Int, Duration> = emptyMap()): GameViewModel {
        settings = FakeSettingsRepository(initialBestTimes = stored)
        return GameViewModel(
            savedStateHandle = SavedStateHandle(route = GameRoute(size)),
            evaluatePositionUseCase = EvaluatePositionUseCase(),
            settingsRepository = settings,
            timeSource = timeSource,
        ).also { created = it }
    }

    /** Advances the coroutine clock and the time source together — they are two clocks. */
    private fun TestScope.elapse(duration: Duration) {
        timeSource += duration
        advanceTimeBy(duration)
        runCurrent()
    }

    private fun GameViewModel.solve() {
        solution.forEach { state.value.onCellClick(it) }
    }

    // ---- placing and removing ----------------------------------------------------------

    @Test
    fun `board size comes from the route`() = gameTest {
        val viewModel = viewModel(size = 6)

        assertEquals(6, viewModel.state.value.size)
        assertEquals(6, viewModel.state.value.queensLeft)
    }

    @Test
    fun `tapping an empty square places a queen`() = gameTest {
        val viewModel = viewModel()

        viewModel.state.value.onCellClick(Square(1, 1))

        assertEquals(setOf(Square(1, 1)), viewModel.state.value.queens)
    }

    @Test
    fun `tapping an occupied square removes the queen`() = gameTest {
        val viewModel = viewModel()

        viewModel.state.value.onCellClick(Square(1, 1))
        viewModel.state.value.onCellClick(Square(1, 1))

        assertEquals(emptySet<Square>(), viewModel.state.value.queens)
    }

    @Test
    fun `queens left counts down`() = gameTest {
        val viewModel = viewModel(size = 4)

        viewModel.state.value.onCellClick(Square(0, 0))

        assertEquals(3, viewModel.state.value.queensLeft)
    }

    @Test
    fun `a tap on a full board is ignored`() = gameTest {
        val viewModel = viewModel(size = 4)
        // Four queens on one row: the board is full but unsolved.
        val fullRow = (0 until 4).map { Square(0, it) }
        fullRow.forEach { viewModel.state.value.onCellClick(it) }

        viewModel.state.value.onCellClick(Square(2, 2))

        assertEquals(fullRow.toSet(), viewModel.state.value.queens)
        assertEquals(0, viewModel.state.value.queensLeft)
    }

    @Test
    fun `a full board still allows queens to be removed`() = gameTest {
        val viewModel = viewModel(size = 4)
        val fullRow = (0 until 4).map { Square(0, it) }
        fullRow.forEach { viewModel.state.value.onCellClick(it) }

        viewModel.state.value.onCellClick(Square(0, 0))

        assertEquals(1, viewModel.state.value.queensLeft)
    }

    @Test
    fun `a rejected tap emits no action`() = gameTest {
        val viewModel = viewModel(size = 4)
        (0 until 4).forEach { viewModel.state.value.onCellClick(Square(0, it)) }

        viewModel.action.test {
            viewModel.state.value.onCellClick(Square(2, 2))
            expectNoEvents()
        }
    }

    // ---- conflicts -----------------------------------------------------------------------

    @Test
    fun `two queens on a diagonal are both marked`() = gameTest {
        val viewModel = viewModel()

        viewModel.state.value.onCellClick(Square(0, 0))
        viewModel.state.value.onCellClick(Square(1, 1))

        assertEquals(setOf(Square(0, 0), Square(1, 1)), viewModel.state.value.conflicts)
    }

    @Test
    fun `removing one of a conflicting pair clears the conflict`() = gameTest {
        val viewModel = viewModel()

        viewModel.state.value.onCellClick(Square(0, 0))
        viewModel.state.value.onCellClick(Square(1, 1))
        viewModel.state.value.onCellClick(Square(1, 1))

        assertEquals(emptySet<Square>(), viewModel.state.value.conflicts)
    }

    // ---- actions ---------------------------------------------------------------------------

    @Test
    fun `placing and removing emit distinct actions`() = gameTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onCellClick(Square(1, 1))
            assertEquals(GameAction.QueenPlaced(hasConflict = false), awaitItem())

            viewModel.state.value.onCellClick(Square(1, 1))
            assertEquals(GameAction.QueenRemoved, awaitItem())
        }
    }

    @Test
    fun `placing onto an attacked square flags the conflict`() = gameTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onCellClick(Square(0, 0))
            assertEquals(GameAction.QueenPlaced(hasConflict = false), awaitItem())

            viewModel.state.value.onCellClick(Square(0, 2))
            assertEquals(GameAction.QueenPlaced(hasConflict = true), awaitItem())
        }
    }

    @Test
    fun `solving emits Solved after the final placement`() = gameTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.solve()
            runCurrent()

            repeat(solution.size) { assertEquals(GameAction.QueenPlaced(hasConflict = false), awaitItem()) }
            assertEquals(GameAction.Solved, awaitItem())
        }
    }

    // ---- solving -----------------------------------------------------------------------------

    @Test
    fun `completing the puzzle marks it solved`() = gameTest {
        val viewModel = viewModel()

        viewModel.solve()
        runCurrent()

        assertTrue(viewModel.state.value.isSolved)
        assertEquals(0, viewModel.state.value.queensLeft)
    }

    @Test
    fun `solving raises the overlay`() = gameTest {
        val viewModel = viewModel()

        viewModel.solve()
        runCurrent()

        assertTrue(viewModel.state.value.isVictoryVisible)
    }

    @Test
    fun `taps after solving are ignored`() = gameTest {
        val viewModel = viewModel()
        viewModel.solve()
        runCurrent()

        viewModel.state.value.onCellClick(Square(0, 0))

        assertEquals(solution.toSet(), viewModel.state.value.queens)
    }

    @Test
    fun `the solve time is recorded`() = gameTest {
        val viewModel = viewModel()

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()

        assertEquals(listOf(4 to 30.seconds), settings.recordedCalls)
    }

    @Test
    fun `the clock freezes when the puzzle is solved`() = gameTest {
        val viewModel = viewModel()

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()
        elapse(10.seconds)

        assertEquals("00:30", viewModel.state.value.time)
    }

    // ---- records -------------------------------------------------------------------------------

    @Test
    fun `a first solve is a new record`() = gameTest {
        val viewModel = viewModel(stored = emptyMap())

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()

        assertTrue(viewModel.state.value.isNewRecord)
    }

    @Test
    fun `beating the stored time is a new record`() = gameTest {
        val viewModel = viewModel(stored = mapOf(4 to 60.seconds))

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()

        assertTrue(viewModel.state.value.isNewRecord)
    }

    @Test
    fun `a slower solve is not a new record`() = gameTest {
        val viewModel = viewModel(stored = mapOf(4 to 10.seconds))

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()

        assertFalse(viewModel.state.value.isNewRecord)
    }

    @Test
    fun `the record badge does not flip once the new time is stored`() = gameTest {
        val viewModel = viewModel(stored = emptyMap())

        elapse(30.seconds)
        viewModel.solve()
        runCurrent()
        elapse(5.seconds)

        assertTrue(viewModel.state.value.isNewRecord)
    }

    // ---- timer -----------------------------------------------------------------------------------

    @Test
    fun `the clock shows zero before the first tick`() = gameTest {
        val viewModel = viewModel()

        runCurrent()

        assertEquals("00:00", viewModel.state.value.time)
    }

    @Test
    fun `the clock does not skip a second over a long game`() = gameTest {
        val viewModel = viewModel()

        // Step second by second; a drifting tick would eventually land past a boundary
        // and miss one of these values.
        (1..600).forEach { second ->
            elapse(1.seconds)
            assertEquals("%02d:%02d".format(second / 60, second % 60), viewModel.state.value.time)
        }
    }

    @Test
    fun `the clock advances while playing`() = gameTest {
        val viewModel = viewModel()

        elapse(75.seconds)

        assertEquals("01:15", viewModel.state.value.time)
    }

    // ---- leaving -----------------------------------------------------------------------

    @Test
    fun `leaving takes the overlay down before it asks to navigate`() = gameTest {
        val viewModel = viewModel()
        viewModel.solve()
        runCurrent()

        viewModel.action.test {
            viewModel.state.value.onLeaveClick()

            assertFalse(viewModel.state.value.isVictoryVisible)
            assertEquals(GameAction.Leave, awaitItem())
        }
    }

    @Test
    fun `leaving keeps the board solved`() = gameTest {
        val viewModel = viewModel()
        viewModel.solve()
        runCurrent()

        viewModel.state.value.onLeaveClick()

        assertTrue(viewModel.state.value.isSolved)
    }

    @Test
    fun `leaving an unsolved board still asks to navigate`() = gameTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onLeaveClick()

            assertEquals(GameAction.Leave, awaitItem())
        }
    }

    // ---- reset -------------------------------------------------------------------------------------

    @Test
    fun `reset clears the board the conflicts and the clock`() = gameTest {
        val viewModel = viewModel()
        viewModel.state.value.onCellClick(Square(0, 0))
        viewModel.state.value.onCellClick(Square(1, 1))
        elapse(20.seconds)

        viewModel.state.value.onResetClick()

        with(viewModel.state.value) {
            assertEquals(emptySet<Square>(), queens)
            assertEquals(emptySet<Square>(), conflicts)
            assertEquals("00:00", time)
            assertFalse(isSolved)
            assertFalse(isVictoryVisible)
            assertEquals(4, queensLeft)
        }
    }

    @Test
    fun `reset clears the record badge and restarts the clock`() = gameTest {
        val viewModel = viewModel()
        elapse(30.seconds)
        viewModel.solve()
        runCurrent()

        viewModel.state.value.onResetClick()
        elapse(5.seconds)

        assertFalse(viewModel.state.value.isNewRecord)
        assertEquals("00:05", viewModel.state.value.time)
    }

    @Test
    fun `the board is playable again after a reset`() = gameTest {
        val viewModel = viewModel()
        viewModel.solve()
        runCurrent()

        viewModel.state.value.onResetClick()
        viewModel.state.value.onCellClick(Square(2, 2))

        assertEquals(setOf(Square(2, 2)), viewModel.state.value.queens)
    }
}

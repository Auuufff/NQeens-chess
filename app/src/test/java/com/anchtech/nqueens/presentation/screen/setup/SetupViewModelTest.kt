package com.anchtech.nqueens.presentation.screen.setup

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.anchtech.nqueens.common.Constants
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import com.anchtech.nqueens.testing.FakeBestTimesRepository
import com.anchtech.nqueens.testing.MainDispatcherRule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bestTimes: FakeBestTimesRepository
    private var created: SetupViewModel? = null

    /**
     * The best-times collector never completes, so the scope is cancelled inside the test.
     */
    private fun setupTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            body()
        } finally {
            created?.viewModelScope?.cancel()
        }
    }

    private fun viewModel(stored: Map<Int, Duration> = emptyMap()): SetupViewModel {
        bestTimes = FakeBestTimesRepository(stored)
        return SetupViewModel(bestTimes).also { created = it }
    }

    // ---- size selection ---------------------------------------------------------------

    @Test
    fun `the offered sizes are exactly the supported range`() = setupTest {
        assertEquals(Constants.BOARD_SIZES, viewModel().state.value.sizes)
    }

    @Test
    fun `no offered size is below the minimum`() = setupTest {
        val sizes = viewModel().state.value.sizes

        assertTrue(sizes.all { it >= Constants.MIN_BOARD_SIZE })
        assertTrue(sizes.all { it <= Constants.MAX_BOARD_SIZE })
    }

    @Test
    fun `the default size is selected initially`() = setupTest {
        assertEquals(Constants.DEFAULT_BOARD_SIZE, viewModel().state.value.selectedSize)
    }

    @Test
    fun `selecting a size updates the selection`() = setupTest {
        val viewModel = viewModel()

        viewModel.state.value.onSizeSelected(11)

        assertEquals(11, viewModel.state.value.selectedSize)
    }

    @Test
    fun `selecting a size leaves the offered sizes untouched`() = setupTest {
        val viewModel = viewModel()

        viewModel.state.value.onSizeSelected(Constants.MAX_BOARD_SIZE)

        assertEquals(Constants.BOARD_SIZES, viewModel.state.value.sizes)
    }

    // ---- starting a game ----------------------------------------------------------------

    @Test
    fun `starting emits the selected size`() = setupTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onSizeSelected(11)
            viewModel.state.value.onStartClick()

            assertEquals(SetupAction.StartGame(11), awaitItem())
        }
    }

    @Test
    fun `starting without changing the selection emits the default size`() = setupTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onStartClick()

            assertEquals(SetupAction.StartGame(Constants.DEFAULT_BOARD_SIZE), awaitItem())
        }
    }

    @Test
    fun `starting twice emits an action each time`() = setupTest {
        val viewModel = viewModel()

        viewModel.action.test {
            viewModel.state.value.onStartClick()
            assertEquals(SetupAction.StartGame(Constants.DEFAULT_BOARD_SIZE), awaitItem())

            viewModel.state.value.onSizeSelected(5)
            viewModel.state.value.onStartClick()
            assertEquals(SetupAction.StartGame(5), awaitItem())
        }
    }

    @Test
    fun `records carry the formatted solve time`() = setupTest {
        val viewModel = viewModel(stored = mapOf(6 to 74.seconds))

        runCurrent()

        assertEquals(listOf(UiBestTime(size = 6, time = "01:14")), viewModel.state.value.records)
    }

    @Test
    fun `the record list is capped`() = setupTest {
        val stored = (1..40).associateWith { it.seconds }
        val viewModel = viewModel(stored = stored)

        runCurrent()

        assertEquals(30, viewModel.state.value.records.size)
    }

    // ---- best times ---------------------------------------------------------------------

    @Test
    fun `stored best times are surfaced`() = setupTest {
        val viewModel = viewModel(stored = mapOf(4 to 30.seconds, 8 to 90.seconds))

        runCurrent()

        assertEquals(
            listOf(UiBestTime(4, "00:30"), UiBestTime(8, "01:30")),
            viewModel.state.value.records,
        )
    }

    @Test
    fun `records are listed in board order`() = setupTest {
        val viewModel = viewModel(stored = mapOf(8 to 90.seconds, 4 to 30.seconds, 6 to 60.seconds))

        runCurrent()

        assertEquals(listOf(4, 6, 8), viewModel.state.value.records.map { it.size })
    }

    @Test
    fun `an empty record board reports no records`() = setupTest {
        val viewModel = viewModel()

        runCurrent()

        assertEquals(emptyList<UiBestTime>(), viewModel.state.value.records)
    }

    @Test
    fun `a time recorded while the screen is open appears without reopening it`() = setupTest {
        val viewModel = viewModel()
        runCurrent()
        assertEquals(emptyList<UiBestTime>(), viewModel.state.value.records)

        bestTimes.record(size = 5, time = 25.seconds)
        runCurrent()

        assertEquals(listOf(UiBestTime(5, "00:25")), viewModel.state.value.records)
    }
}

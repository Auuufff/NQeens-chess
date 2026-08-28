package com.anchtech.nqueens.presentation.screen.root

import androidx.lifecycle.viewModelScope
import com.anchtech.nqueens.testing.FakeSettingsRepository
import com.anchtech.nqueens.testing.MainDispatcherRule
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settings: FakeSettingsRepository
    private var created: RootViewModel? = null

    /** The theme flow never completes, so the scope is cancelled inside the test. */
    private fun rootTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            body()
        } finally {
            created?.viewModelScope?.cancel()
        }
    }

    private fun viewModel(darkTheme: Boolean? = null): RootViewModel {
        settings = FakeSettingsRepository(initialDarkTheme = darkTheme)
        return RootViewModel(settings).also { created = it }
    }

    @Test
    fun `no stored choice leaves the theme to the system`() = rootTest {
        val viewModel = viewModel()

        runCurrent()

        assertNull(viewModel.darkTheme.value)
    }

    @Test
    fun `the stored choice is surfaced`() = rootTest {
        val viewModel = viewModel(darkTheme = true)

        runCurrent()

        assertEquals(true, viewModel.darkTheme.value)
    }

    @Test
    fun `a choice made while the app is running is surfaced`() = rootTest {
        val viewModel = viewModel()
        runCurrent()

        settings.setDarkTheme(true)
        runCurrent()

        assertEquals(true, viewModel.darkTheme.value)
    }
}

package com.anchtech.nqueens.presentation.screen.setup

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.anchtech.nqueens.R
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import com.anchtech.nqueens.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port")
class SetupScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun setSetup(state: SetupState) {
        compose.setScreen {
            SetupScreenContent(state = state)
        }
    }

    private fun slider() = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

    private fun preview(size: Int) = compose.onNodeWithContentDescription(string(R.string.setup_board_preview, size))

    // ---- board size --------------------------------------------------------------------

    @Test
    fun `the preview describes the selected size`() {
        setSetup(SetupState(selectedSize = 6))

        preview(size = 6).assertIsDisplayed()
    }

    @Test
    fun `the selected size is spelled out`() {
        setSetup(SetupState(selectedSize = 6))

        compose.onNodeWithText(string(R.string.setup_size_label, 6)).assertIsDisplayed()
    }

    @Test
    fun `moving the slider selects the size it lands on`() {
        var selected: Int? = null
        setSetup(SetupState(selectedSize = 8, onSizeSelected = { selected = it }))

        slider().performSemanticsAction(SemanticsActions.SetProgress) { it(10f) }

        assertEquals(10, selected)
    }

    @Test
    fun `holding the slider at the selected size reports nothing`() {
        var selected: Int? = null
        setSetup(SetupState(selectedSize = 8, onSizeSelected = { selected = it }))

        slider().performSemanticsAction(SemanticsActions.SetProgress) { it(8f) }

        assertNull(selected)
    }

    // ---- the controls ------------------------------------------------------------------

    @Test
    fun `start is wired to the start callback`() {
        var starts = 0
        setSetup(SetupState(onStartClick = { starts++ }))

        compose.onNodeWithText(string(R.string.setup_start)).performScrollTo().performClick()

        assertEquals(1, starts)
    }

    @Test
    fun `the theme switch turns dark theme on`() {
        var darkTheme: Boolean? = null
        setSetup(SetupState(darkTheme = false, onDarkThemeChange = { darkTheme = it }))

        compose.onNodeWithContentDescription(string(R.string.setup_dark_theme)).performClick()

        assertEquals(true, darkTheme)
    }

    @Test
    fun `the theme switch turns dark theme off`() {
        var darkTheme: Boolean? = null
        setSetup(SetupState(darkTheme = true, onDarkThemeChange = { darkTheme = it }))

        compose.onNodeWithContentDescription(string(R.string.setup_dark_theme)).performClick()

        assertEquals(false, darkTheme)
    }

    // ---- the record board --------------------------------------------------------------

    @Test
    fun `an empty record board says so`() {
        setSetup(SetupState(records = emptyList()))

        compose.onNodeWithText(string(R.string.setup_no_best_times)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `records are listed with their times`() {
        val records = listOf(UiBestTime(size = 4, time = "00:12"), UiBestTime(size = 6, time = "01:14"))
        setSetup(SetupState(selectedSize = 8, records = records))

        compose.onNodeWithText("00:12").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("01:14").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.setup_no_best_times)).assertDoesNotExist()
    }

    // ---- orientation -------------------------------------------------------------------

    @Test
    fun `portrait stacks the preview above the controls`() {
        setSetup(SetupState(selectedSize = 8))

        val previewBounds = preview(size = 8).getUnclippedBoundsInRoot()
        val startBounds = compose.onNodeWithText(string(R.string.setup_start)).getUnclippedBoundsInRoot()

        assertTrue("preview=$previewBounds start=$startBounds", previewBounds.bottom <= startBounds.top)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `landscape puts the preview beside the controls`() {
        setSetup(SetupState(selectedSize = 8))

        val previewBounds = preview(size = 8).getUnclippedBoundsInRoot()
        val startBounds = compose.onNodeWithText(string(R.string.setup_start)).getUnclippedBoundsInRoot()

        assertTrue("preview=$previewBounds start=$startBounds", previewBounds.right <= startBounds.left)
    }
}

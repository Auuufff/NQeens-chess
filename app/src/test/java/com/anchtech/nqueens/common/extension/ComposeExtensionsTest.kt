package com.anchtech.nqueens.common.extension

import androidx.compose.ui.Modifier
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposeExtensionsTest {

    private var clicks = 0

    private val click = singleClick { clicks++ }

    /** The debounce window, from `SINGLE_CLICK_THRESHOLD_MS`. */
    private val threshold = Duration.ofMillis(600)

    private fun advance(duration: Duration) = ShadowSystemClock.advanceBy(duration)

    // ---- singleClick -------------------------------------------------------------------

    @Test
    fun `the first click gets through`() {
        click()

        assertEquals(1, clicks)
    }

    @Test
    fun `a repeat inside the window is dropped`() {
        click()
        advance(threshold.minusMillis(1))
        click()

        assertEquals(1, clicks)
    }

    @Test
    fun `an immediate repeat is dropped`() {
        repeat(5) { click() }

        assertEquals(1, clicks)
    }

    @Test
    fun `a click on the threshold gets through`() {
        click()
        advance(threshold)
        click()

        assertEquals(2, clicks)
    }

    @Test
    fun `a click after the window gets through`() {
        click()
        advance(threshold.plusMillis(1))
        click()

        assertEquals(2, clicks)
    }

    @Test
    fun `each accepted click restarts the window`() {
        click()
        advance(threshold)
        click()
        advance(threshold.minusMillis(1))
        click()

        assertEquals(2, clicks)
    }

    @Test
    fun `a dropped click does not restart the window`() {
        click()
        advance(threshold.minusMillis(100))
        click()
        advance(Duration.ofMillis(100))
        click()

        assertEquals(2, clicks)
    }

    @Test
    fun `separate handlers debounce independently`() {
        var other = 0
        val otherClick = singleClick { other++ }

        click()
        otherClick()

        assertEquals(1, clicks)
        assertEquals(1, other)
    }

    @Test
    fun `the handler is not called until it is invoked`() {
        singleClick { clicks++ }

        assertEquals(0, clicks)
    }

    // ---- thenIf ------------------------------------------------------------------------

    @Test
    fun `thenIf applies the modifier when the condition holds`() {
        val modifier = Modifier.thenIf(true) { then(Added) }

        assertTrue(modifier.any { it === Added })
    }

    @Test
    fun `thenIf leaves the chain untouched when the condition does not hold`() {
        val base: Modifier = Modifier.then(Existing)

        assertSame(base, base.thenIf(false) { then(Added) })
    }

    @Test
    fun `thenIf appends to the chain rather than replacing it`() {
        val modifier = Modifier
            .then(Existing)
            .thenIf(true) { then(Added) }

        assertTrue(modifier.any { it === Existing })
        assertTrue(modifier.any { it === Added })
    }

    private companion object {
        /** Marker elements, so a chain can be asserted on by identity. */
        object Existing : Modifier.Element

        object Added : Modifier.Element
    }
}

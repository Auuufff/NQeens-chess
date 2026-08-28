package com.anchtech.nqueens.common.extension

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationExtensionsTest {

    // ---- the mm:ss shape ---------------------------------------------------------------

    @Test
    fun `zero reads as a full clock`() {
        assertEquals("00:00", Duration.ZERO.formatAsClock())
    }

    @Test
    fun `seconds are padded to two digits`() {
        assertEquals("00:01", 1.seconds.formatAsClock())
    }

    @Test
    fun `the last second before a minute stays in seconds`() {
        assertEquals("00:59", 59.seconds.formatAsClock())
    }

    @Test
    fun `a whole minute rolls the seconds back to zero`() {
        assertEquals("01:00", 60.seconds.formatAsClock())
    }

    @Test
    fun `minutes and seconds are both reported`() {
        assertEquals("01:15", 75.seconds.formatAsClock())
    }

    @Test
    fun `minutes are padded to two digits`() {
        assertEquals("09:09", (9.minutes + 9.seconds).formatAsClock())
    }

    // ---- minutes are not wrapped at an hour --------------------------------------------

    @Test
    fun `an hour reads as sixty minutes rather than starting over`() {
        assertEquals("60:00", 1.hours.formatAsClock())
    }

    @Test
    fun `a long solve keeps counting minutes past three digits`() {
        assertEquals("72:14", (72.minutes + 14.seconds).formatAsClock())
    }

    @Test
    fun `hours are never a field of their own`() {
        assertEquals("6000:00", 100.hours.formatAsClock())
    }

    // ---- sub-second input --------------------------------------------------------------

    @Test
    fun `a part-second is truncated rather than rounded up`() {
        assertEquals("00:01", 1_900.milliseconds.formatAsClock())
    }

    @Test
    fun `just under a second is still zero`() {
        assertEquals("00:00", 999.milliseconds.formatAsClock())
    }

    @Test
    fun `just under a minute does not round up to one`() {
        assertEquals("00:59", 59_999.milliseconds.formatAsClock())
    }
}

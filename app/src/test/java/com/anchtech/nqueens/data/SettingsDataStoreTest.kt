package com.anchtech.nqueens.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import app.cash.turbine.test
import com.anchtech.nqueens.common.Constants
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Exercises the real DataStore over a temporary file, so what is asserted is what would be
 * written to disk.
 */
class SettingsDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** The store underneath, for asserting on what was persisted and for seeding raw values. */
    private lateinit var preferences: DataStore<Preferences>

    private fun storeTest(body: suspend TestScope.(SettingsDataStore) -> Unit): TestResult = runTest {
        preferences = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "settings.preferences_pb") },
        )
        body(SettingsDataStore(preferences))
    }

    private suspend fun SettingsDataStore.storedTimes(): Map<Int, Duration> = bestTimes.first()

    private fun bestTimeKey(size: Int) = longPreferencesKey("best_time_$size")

    // ---- recording a time --------------------------------------------------------------

    @Test
    fun `a recorded time reads back`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 90.seconds)

        assertEquals(mapOf(8 to 90.seconds), store.storedTimes())
    }

    @Test
    fun `a size that was never solved is absent`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 90.seconds)

        assertNull(store.storedTimes()[9])
    }

    @Test
    fun `an untouched store holds no times`() = storeTest { store ->
        assertEquals(emptyMap<Int, Duration>(), store.storedTimes())
    }

    @Test
    fun `the smallest offered size round-trips`() = storeTest { store ->
        store.recordBestTime(size = Constants.MIN_BOARD_SIZE, time = 3.seconds)

        assertEquals(mapOf(Constants.MIN_BOARD_SIZE to 3.seconds), store.storedTimes())
    }

    @Test
    fun `the largest offered size round-trips`() = storeTest { store ->
        store.recordBestTime(size = Constants.MAX_BOARD_SIZE, time = 40.minutes)

        assertEquals(mapOf(Constants.MAX_BOARD_SIZE to 40.minutes), store.storedTimes())
    }

    // ---- keeping whichever is faster ---------------------------------------------------

    @Test
    fun `a faster time replaces the stored one`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 90.seconds)

        store.recordBestTime(size = 8, time = 60.seconds)

        assertEquals(mapOf(8 to 60.seconds), store.storedTimes())
    }

    @Test
    fun `a slower time is discarded`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 60.seconds)

        store.recordBestTime(size = 8, time = 90.seconds)

        assertEquals(mapOf(8 to 60.seconds), store.storedTimes())
    }

    @Test
    fun `an equal time leaves the record alone`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 60.seconds)

        store.recordBestTime(size = 8, time = 60.seconds)

        assertEquals(mapOf(8 to 60.seconds), store.storedTimes())
    }

    @Test
    fun `a faster time by a single millisecond still wins`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 60_000.milliseconds)

        store.recordBestTime(size = 8, time = 59_999.milliseconds)

        assertEquals(mapOf(8 to 59_999.milliseconds), store.storedTimes())
    }

    @Test
    fun `each size keeps its own record`() = storeTest { store ->
        store.recordBestTime(size = 4, time = 10.seconds)
        store.recordBestTime(size = 8, time = 90.seconds)

        store.recordBestTime(size = 8, time = 60.seconds)

        assertEquals(mapOf(4 to 10.seconds, 8 to 60.seconds), store.storedTimes())
    }

    @Test
    fun `a slower time on one size does not disturb another`() = storeTest { store ->
        store.recordBestTime(size = 4, time = 10.seconds)
        store.recordBestTime(size = 8, time = 60.seconds)

        store.recordBestTime(size = 8, time = 90.seconds)

        assertEquals(mapOf(4 to 10.seconds, 8 to 60.seconds), store.storedTimes())
    }

    // ---- what a millisecond store can hold ---------------------------------------------

    @Test
    fun `a time is kept to the millisecond`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 1_234.milliseconds)

        assertEquals(mapOf(8 to 1_234.milliseconds), store.storedTimes())
    }

    @Test
    fun `finer than a millisecond is truncated`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 1_500.microseconds)

        assertEquals(mapOf(8 to 1.milliseconds), store.storedTimes())
    }

    // ---- what reaches the disk ---------------------------------------------------------

    @Test
    fun `a time is written under a stable key as milliseconds`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 5.seconds)

        assertEquals(5_000L, preferences.data.first()[bestTimeKey(8)])
    }

    @Test
    fun `a discarded time never reaches the store`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 60.seconds)

        store.recordBestTime(size = 8, time = 90.seconds)

        assertEquals(60_000L, preferences.data.first()[bestTimeKey(8)])
    }

    @Test
    fun `a stored time below the smallest offered size is not surfaced`() = storeTest { store ->
        preferences.edit { it[bestTimeKey(Constants.MIN_BOARD_SIZE - 1)] = 1_000L }

        assertEquals(emptyMap<Int, Duration>(), store.storedTimes())
    }

    @Test
    fun `a stored time above the largest offered size is not surfaced`() = storeTest { store ->
        preferences.edit { it[bestTimeKey(Constants.MAX_BOARD_SIZE + 1)] = 1_000L }

        assertEquals(emptyMap<Int, Duration>(), store.storedTimes())
    }

    @Test
    fun `an out-of-range time does not hide the ones in range`() = storeTest { store ->
        preferences.edit { it[bestTimeKey(Constants.MAX_BOARD_SIZE + 1)] = 1_000L }

        store.recordBestTime(size = 8, time = 5.seconds)

        assertEquals(mapOf(8 to 5.seconds), store.storedTimes())
    }

    // ---- observing ---------------------------------------------------------------------

    @Test
    fun `a time recorded while collecting reaches the collector`() = storeTest { store ->
        store.bestTimes.test {
            assertEquals(emptyMap<Int, Duration>(), awaitItem())

            store.recordBestTime(size = 8, time = 90.seconds)

            assertEquals(mapOf(8 to 90.seconds), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- the theme ---------------------------------------------------------------------

    @Test
    fun `an unset theme is left to the system`() = storeTest { store ->
        assertNull(store.darkTheme.first())
    }

    @Test
    fun `a dark theme choice reads back`() = storeTest { store ->
        store.setDarkTheme(enabled = true)

        assertEquals(true, store.darkTheme.first())
    }

    @Test
    fun `a light theme choice reads back as a choice rather than as unset`() = storeTest { store ->
        store.setDarkTheme(enabled = true)

        store.setDarkTheme(enabled = false)

        assertEquals(false, store.darkTheme.first())
    }

    @Test
    fun `the theme and the best times do not overwrite each other`() = storeTest { store ->
        store.recordBestTime(size = 8, time = 90.seconds)

        store.setDarkTheme(enabled = true)

        assertEquals(mapOf(8 to 90.seconds), store.storedTimes())
        assertEquals(true, store.darkTheme.first())
    }
}

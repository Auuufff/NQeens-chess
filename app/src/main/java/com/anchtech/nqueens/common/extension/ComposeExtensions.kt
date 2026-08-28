package com.anchtech.nqueens.common.extension

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects only while the screen is at least [Lifecycle.State.STARTED].
 */
@Composable
fun <T> Flow<T>.collectAsEffect(onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { currentOnEvent(it) }
        }
    }
}

/**
 * Returns a click handler that ignores repeats within [SINGLE_CLICK_THRESHOLD_MS].
 *
 * Not a composable. The timestamp lives in the returned closure, so callers must remember
 * it across recompositions; calling this inline rebuilds the closure every recomposition
 * and defeats the debounce. Inside a composable use [rememberSingleClick].
 */
fun singleClick(onClick: () -> Unit): () -> Unit {
    var lastClickAt = 0L
    return {
        val now = SystemClock.elapsedRealtime()
        if (lastClickAt == 0L || now - lastClickAt >= SINGLE_CLICK_THRESHOLD_MS) {
            lastClickAt = now
            onClick()
        }
    }
}

/**
 * A [singleClick] handler whose timestamp survives recomposition regardless of [onClick]
 * identity, always invoking the latest [onClick].
 */
@Composable
fun rememberSingleClick(onClick: () -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onClick)
    return remember { singleClick { latest() } }
}

/**
 * Applies [other] only when [condition] holds.
 */
fun Modifier.thenIf(condition: Boolean, other: Modifier.() -> Modifier): Modifier =
    if (condition) other() else this

/**
 * Returns true if the window is wider than it is tall.
 */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

private const val SINGLE_CLICK_THRESHOLD_MS = 600L

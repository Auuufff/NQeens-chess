package com.anchtech.nqueens.common.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a ViewModel's one-time actions while the screen is at least [Lifecycle.State.STARTED].
 *
 * `repeatOnLifecycle` is what makes "actions are dropped while off-composition" true: with
 * a bare `LaunchedEffect` the collector would survive backgrounding and fire haptics behind
 * the user's back.
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

/** Applies [other] only when [condition] holds. */
fun Modifier.thenIf(condition: Boolean, other: Modifier.() -> Modifier): Modifier =
    if (condition) other() else this

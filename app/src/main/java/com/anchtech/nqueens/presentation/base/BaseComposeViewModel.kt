package com.anchtech.nqueens.presentation.base

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base for ViewModels backing a Compose screen: one [StateFlow] of state, one
 * [SharedFlow] of transient actions.
 *
 * @param STATE the screen's immutable state
 * @param ACTION the screen's one-time effects
 */
abstract class BaseComposeViewModel<STATE : BaseState, ACTION : BaseAction>(
    initialState: STATE,
) : BaseViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = _state.asStateFlow()

    private val _action = MutableSharedFlow<ACTION>(replay = 0, extraBufferCapacity = 1)

    /**
     * Transient effects. Replay is 0 on purpose: an effect emitted while no screen is
     * collecting should be dropped, not queued and replayed on return.
     */
    val action: SharedFlow<ACTION> = _action.asSharedFlow()

    /**
     * Updates the state.
     *
     * Takes `(STATE) -> STATE` rather than `STATE.() -> STATE` deliberately. With a
     * receiver, the state becomes the innermost implicit receiver, so any field sharing a
     * name with a ViewModel property or an enclosing local is silently shadowed —
     * `copy(size = size)` would assign the state's own value to itself and compile clean.
     *
     * [block] may be re-run under contention, so it must be side-effect free: read clocks
     * and suspend before calling this, then pass the result in.
     */
    protected fun updateState(block: (STATE) -> STATE) {
        _state.update(block)
    }

    /** Emits a transient effect. Buffered so several in one frame cannot be dropped. */
    protected fun sendAction(action: ACTION) {
        _action.tryEmit(action)
    }
}

package com.anchtech.nqueens.presentation.base

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base for ViewModels backing a Compose screen.
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
     * Transient effects. Replay is 0, so emissions with no collector are dropped.
     */
    val action: SharedFlow<ACTION> = _action.asSharedFlow()

    /**
     * Updates the state. [block] may be re-run, so it must be side-effect free.
     *
     * Takes `(STATE) -> STATE`, not `STATE.() -> STATE`: a receiver would shadow
     * same-named ViewModel properties, making `copy(size = size)` a silent no-op.
     */
    protected fun updateState(block: (STATE) -> STATE) {
        _state.update(block)
    }

    /**
     * Emits a transient effect. Buffered, so several in one frame are not dropped.
     */
    protected fun sendAction(action: ACTION) {
        _action.tryEmit(action)
    }
}

package com.anchtech.nqueens.presentation.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

/**
 * Launches into [viewModelScope]; uncaught exceptions reach [onError].
 */
abstract class BaseViewModel : ViewModel(), CoroutineScope {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(this::class.simpleName.orEmpty(), "Unhandled ViewModel exception", throwable)
        onError(throwable)
    }

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext + exceptionHandler

    /**
     * Called for any exception uncaught by a coroutine launched in this scope.
     */
    protected open fun onError(throwable: Throwable) = Unit
}

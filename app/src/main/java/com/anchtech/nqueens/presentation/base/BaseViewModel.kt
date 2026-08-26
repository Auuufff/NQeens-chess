package com.anchtech.nqueens.presentation.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

/**
 * Base for every ViewModel in the app.
 *
 * Implements [CoroutineScope] over [viewModelScope], so coroutines launched here are
 * cancelled when the ViewModel is cleared and one failing child cannot cancel its
 * siblings ([viewModelScope] uses a `SupervisorJob`).
 *
 * Installs a [CoroutineExceptionHandler] that logs and forwards to [onError].
 * Consequence: **subclasses contain no try/catch** — failures land in one place.
 */
abstract class BaseViewModel : ViewModel(), CoroutineScope {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(this::class.simpleName.orEmpty(), "Unhandled ViewModel exception", throwable)
        onError(throwable)
    }

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext + exceptionHandler

    /** Hook for subclasses to surface a failure in their state. */
    protected open fun onError(throwable: Throwable) = Unit
}

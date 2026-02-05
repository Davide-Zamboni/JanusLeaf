package com.janusleaf.app.presentation.viewmodel

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

interface ClearableViewModel {
    fun clear()
}

abstract class KmpViewModel : ClearableViewModel {
    private val rootJob = SupervisorJob()
    protected val scope: CoroutineScope = CoroutineScope(rootJob + Dispatchers.Main.immediate)

    protected fun launchSafely(
        operation: String,
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch {
        try {
            block()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            Napier.e("Unhandled exception in $operation", throwable, tag = "KmpViewModel")
            onError(throwable)
        }
    }

    final override fun clear() {
        scope.cancel()
        onCleared()
    }

    protected open fun onCleared() = Unit
}

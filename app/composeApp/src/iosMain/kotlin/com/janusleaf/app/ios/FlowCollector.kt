package com.janusleaf.app.ios

/**
 * Simple cancellable interface for Swift to cancel Kotlin coroutine jobs.
 * This avoids exposing complex generic types to Swift.
 */
class Cancellable(private val onCancel: () -> Unit) {
    private var isCancelled = false
    
    /**
     * Cancel the operation.
     */
    fun cancel() {
        if (!isCancelled) {
            isCancelled = true
            onCancel()
        }
    }
}

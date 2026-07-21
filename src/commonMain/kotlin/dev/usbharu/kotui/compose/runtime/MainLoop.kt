package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CoroutineScope

/**
 * Starts the platform main loop. Returns true when [block] completed before this
 * function returned (blocking JVM/Native), or false when completion is asynchronous
 * (JavaScript). Asynchronous implementations must invoke [onComplete] exactly once.
 */
internal expect fun runMainLoop(
    block: suspend CoroutineScope.() -> Unit,
    onComplete: (Throwable?) -> Unit,
): Boolean

/**
 * Forcefully terminates the current process. Used on TUI shutdown to guarantee
 * exit even when background workers (e.g. the blocking terminal input reader)
 * cannot be interrupted via coroutine cancellation.
 */
internal expect fun forceExit(status: Int): Nothing

package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CoroutineScope

internal expect fun runMainLoop(block: suspend CoroutineScope.() -> Unit)

/**
 * Forcefully terminates the current process. Used on TUI shutdown to guarantee
 * exit even when background workers (e.g. the blocking terminal input reader)
 * cannot be interrupted via coroutine cancellation.
 */
internal expect fun forceExit(status: Int): Nothing

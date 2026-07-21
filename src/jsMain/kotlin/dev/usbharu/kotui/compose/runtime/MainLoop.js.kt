package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
internal actual fun runMainLoop(
    block: suspend CoroutineScope.() -> Unit,
    onComplete: (Throwable?) -> Unit,
): Boolean {
    // Node.js cannot block the single-threaded event loop, so we schedule the
    // TUI coroutine on GlobalScope and rely on Node keeping the process alive
    // while stdin / timers are active.
    GlobalScope.launch {
        var failure: Throwable? = null
        try {
            block()
        } catch (t: Throwable) {
            failure = t
        } finally {
            onComplete(failure)
        }
        failure?.let { throw it }
    }
    return false
}

internal actual fun forceExit(status: Int): Nothing {
    js("process.exit(status)")
    @Suppress("UNREACHABLE_CODE")
    throw RuntimeException("unreachable")
}

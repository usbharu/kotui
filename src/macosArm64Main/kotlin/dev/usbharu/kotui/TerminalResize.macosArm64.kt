@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import kotlin.concurrent.AtomicInt
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.posix.SA_RESTART
import platform.posix.SIGWINCH
import platform.posix.sigaction

// Signal handlers on Native must stick to async-signal-safe operations. We
// limit ourselves to setting an atomic flag; the watcher coroutine reads the
// flag on a short cadence and performs the heavier `terminalSize()` work.
private const val QUIET_WINDOW_MS = 80L

private val resizePending = AtomicInt(0)

@Suppress("UNUSED_PARAMETER")
private fun sigwinchRaw(sig: Int) {
    resizePending.value = 1
}

private val sigwinchHandler = staticCFunction(::sigwinchRaw)

actual fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    val saved = nativeHeap.alloc<sigaction>()
    memScoped {
        val action = alloc<sigaction>()
        action.__sigaction_u.__sa_handler = sigwinchHandler
        action.sa_flags = SA_RESTART
        sigaction(SIGWINCH, action.ptr, saved.ptr)
    }

    val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)
    scope.launch {
        var last = terminalSize()
        while (isActive) {
            delay(30)
            if (resizePending.value != 0) {
                // Debounce: wait for a quiet window before reading the size
                // so a rapid drag turns into ONE onResize call at the end of
                // the drag rather than dozens of partial updates, each of
                // which would clear the screen and redraw. Without this the
                // main loop can't keep up with the render storm and the TUI
                // appears to hang.
                while (isActive) {
                    resizePending.value = 0
                    delay(QUIET_WINDOW_MS)
                    if (resizePending.value == 0) break
                }
                val now = terminalSize() ?: continue
                if (now != last) {
                    last = now
                    onResize(now)
                }
            }
        }
    }

    return object : TerminalResizeWatcher {
        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
            sigaction(SIGWINCH, saved.ptr, null)
            nativeHeap.free(saved.rawPtr)
            job.cancel()
        }
    }
}

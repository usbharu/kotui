package dev.usbharu.kotui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Windows has no SIGWINCH analogue. WINDOW_BUFFER_SIZE_EVENT is available via
// ReadConsoleInput but integrating that with the existing input pump is a
// larger change — for now we fall back to polling the console-screen-buffer
// size at the same cadence the previous implementation used.
actual fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)
    scope.launch {
        var last = terminalSize()
        while (isActive) {
            delay(150)
            val now = terminalSize() ?: continue
            if (now != last) {
                last = now
                onResize(now)
            }
        }
    }
    return object : TerminalResizeWatcher {
        override fun close() { job.cancel() }
    }
}

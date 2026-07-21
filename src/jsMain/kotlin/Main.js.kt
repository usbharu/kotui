package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun enableRawMode() {
    val stdin = js("process.stdin")
    if (stdin.isTTY != true || jsTypeOf(stdin.setRawMode) != "function") {
        throw IllegalStateException("kotui: runTui requires an interactive stdin terminal.")
    }
    stdin.setRawMode(true)
    stdin.resume()
    stdin.setEncoding("utf8")
}

actual fun disableRawMode() {
    val stdin = js("process.stdin")
    if (jsTypeOf(stdin.setRawMode) == "function") stdin.setRawMode(false)
    stdin.pause()
}

actual suspend fun onInputEvent(onEvent: (InputEvent) -> Boolean) = suspendCancellableCoroutine { continuation ->
    val stdin = js("process.stdin")
    val decoder = AnsiKeyDecoder()
    var stopped = false
    var flushTimer: dynamic = null
    lateinit var listener: (String) -> Unit

    fun cancelFlush() {
        if (flushTimer != null) {
            clearNodeTimeout(flushTimer)
            flushTimer = null
        }
    }

    fun stop() {
        if (stopped) return
        stopped = true
        cancelFlush()
        stdin.removeListener("data", listener)
        if (continuation.isActive) continuation.resume(Unit)
    }

    fun emit(events: List<InputEvent>) {
        if (stopped) return
        for (e in events) {
            if (!onEvent(e)) {
                stop()
                return
            }
        }
    }

    listener = listener@{ data: String ->
        if (stopped) return@listener
        cancelFlush()
        for (c in data) {
            emit(decoder.feed(c))
            if (stopped) return@listener
        }
        // A Node data chunk is not a protocol boundary: CSI and paste sequences
        // may be split across chunks. Resolve a lone Escape only after a quiet window.
        if (decoder.hasPending()) {
            flushTimer = setNodeTimeout({
                flushTimer = null
                if (!stopped && decoder.hasPending()) emit(decoder.flush())
            }, 40)
        }
    }
    stdin.on("data", listener)
    continuation.invokeOnCancellation {
        if (!stopped) {
            stopped = true
            cancelFlush()
            stdin.removeListener("data", listener)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun setNodeTimeout(handler: () -> Unit, milliseconds: Int): dynamic =
    js("setTimeout(handler, milliseconds)")

@Suppress("UNUSED_PARAMETER")
private fun clearNodeTimeout(id: dynamic) {
    js("clearTimeout(id)")
}

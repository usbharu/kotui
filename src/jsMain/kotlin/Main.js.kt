package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent

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

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
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

    fun emit(events: List<InputEvent>) {
        if (stopped) return
        for (e in events) {
            if (!onEvent(e)) {
                stopped = true
                cancelFlush()
                stdin.removeListener("data", listener)
                stdin.pause()
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
}

@Suppress("UNUSED_PARAMETER")
private fun setNodeTimeout(handler: () -> Unit, milliseconds: Int): dynamic =
    js("setTimeout(handler, milliseconds)")

@Suppress("UNUSED_PARAMETER")
private fun clearNodeTimeout(id: dynamic) {
    js("clearTimeout(id)")
}

package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent

actual fun enableRawMode() {
    val stdin = js("process.stdin")
    stdin.setRawMode(true)
    stdin.resume()
    stdin.setEncoding("utf8")
}

actual fun disableRawMode() {
    val stdin = js("process.stdin")
    stdin.setRawMode(false)
    stdin.pause()
}

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val stdin = js("process.stdin")
    val decoder = AnsiKeyDecoder()
    var stopped = false

    fun emit(events: List<InputEvent>) {
        if (stopped) return
        for (e in events) {
            if (!onEvent(e)) {
                stopped = true
                stdin.removeAllListeners("data")
                stdin.pause()
                return
            }
        }
    }

    val listener: (String) -> Unit = listener@{ data: String ->
        if (stopped) return@listener
        for (c in data) {
            emit(decoder.feed(c))
            if (stopped) return@listener
        }
        // Flush at the end of each data event so ESC not followed by a sequence
        // resolves to a plain ESCAPE.
        if (decoder.hasPending()) emit(decoder.flush())
    }
    stdin.on("data", listener)
}

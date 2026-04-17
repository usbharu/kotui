package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import java.io.InputStream

actual fun enableRawMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty raw -echo < /dev/tty")).waitFor()
}

actual fun disableRawMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty sane < /dev/tty")).waitFor()
}

private const val ESC_TIMEOUT_MS = 40L
private const val ESC_POLL_MS = 5L

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val decoder = AnsiKeyDecoder()
    val input = System.`in`

    fun emit(events: List<InputEvent>): Boolean {
        for (e in events) {
            if (!onEvent(e)) return false
        }
        return true
    }

    while (true) {
        if (decoder.hasPending()) {
            // Wait briefly for continuation bytes. If none arrive, flush pending state
            // (disambiguates ESC alone from ESC-initiated sequences).
            var waited = 0L
            while (input.available() == 0 && waited < ESC_TIMEOUT_MS) {
                Thread.sleep(ESC_POLL_MS)
                waited += ESC_POLL_MS
            }
            if (input.available() == 0) {
                if (!emit(decoder.flush())) return
                continue
            }
        }
        val ch = readUtf8Char(input) ?: break
        if (!emit(decoder.feed(ch))) return
    }
}

private fun readUtf8Char(input: InputStream): Char? {
    val b = input.read()
    if (b == -1) return null
    if (b < 0x80) return b.toChar()
    val len = when {
        b and 0xE0 == 0xC0 -> 2
        b and 0xF0 == 0xE0 -> 3
        b and 0xF8 == 0xF0 -> 4
        else -> 1
    }
    if (len == 1) return b.toChar()
    val bytes = ByteArray(len)
    bytes[0] = b.toByte()
    for (i in 1 until len) bytes[i] = input.read().toByte()
    return bytes.decodeToString()[0]
}

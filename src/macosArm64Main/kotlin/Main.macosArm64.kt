@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import kotlinx.cinterop.*
import platform.posix.*

private val originalTermios = nativeHeap.alloc<termios>()

actual fun enableRawMode() {
    if (isatty(STDIN_FILENO) == 0) {
        throw IllegalStateException("kotui: runTui requires an interactive stdin terminal.")
    }
    if (tcgetattr(STDIN_FILENO, originalTermios.ptr) != 0) {
        throw IllegalStateException("kotui: unable to read terminal state.")
    }

    memScoped {
        val raw = alloc<termios>()
        if (tcgetattr(STDIN_FILENO, raw.ptr) != 0) {
            throw IllegalStateException("kotui: unable to read terminal state.")
        }

        raw.c_lflag = raw.c_lflag and (ECHO or ICANON or IEXTEN or ISIG).inv().toULong()
        raw.c_iflag = raw.c_iflag and (BRKINT or ICRNL or INPCK or ISTRIP or IXON).inv().toULong()
        raw.c_cflag = raw.c_cflag or CS8.toULong()
        raw.c_oflag = raw.c_oflag and OPOST.inv().toULong()

        // Short read timeout so ESC alone can be distinguished from ESC-prefixed sequences.
        // We use read() directly (not getchar) to avoid stdio's sticky EOF flag on timeout.
        raw.c_cc[VMIN] = 0.toUByte()
        raw.c_cc[VTIME] = 1.toUByte() // tenths of a second (100ms)

        if (tcsetattr(STDIN_FILENO, TCSAFLUSH, raw.ptr) != 0) {
            throw IllegalStateException("kotui: unable to enable raw mode.")
        }
    }
}

actual fun disableRawMode() {
    tcsetattr(STDIN_FILENO, TCSAFLUSH, originalTermios.ptr)
}

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val decoder = AnsiKeyDecoder()

    fun emit(events: List<InputEvent>): Boolean {
        for (e in events) if (!onEvent(e)) return false
        return true
    }

    memScoped {
        val buf = alloc<ByteVar>()
        fun readByte(): Int {
            val n = read(STDIN_FILENO, buf.ptr, 1.convert())
            if (n <= 0) return -1
            return buf.value.toInt() and 0xFF
        }

        while (true) {
            val b = readByte()
            if (b < 0) {
                // VTIME timeout or EOF. Flush any pending escape sequence.
                if (decoder.hasPending()) {
                    if (!emit(decoder.flush())) return
                }
                continue
            }
            val ch = decodeUtf8Char(b, ::readByte) ?: continue
            if (!emit(decoder.feed(ch))) return
        }
    }
}

private fun decodeUtf8Char(first: Int, readByte: () -> Int): Char? {
    if (first < 0x80) return first.toChar()
    val len = when {
        first and 0xE0 == 0xC0 -> 2
        first and 0xF0 == 0xE0 -> 3
        first and 0xF8 == 0xF0 -> 4
        else -> 1
    }
    if (len == 1) return first.toChar()
    val bytes = ByteArray(len)
    bytes[0] = first.toByte()
    for (i in 1 until len) {
        // Continuation bytes follow immediately; retry briefly on timeout.
        var next = readByte()
        var retries = 0
        while (next < 0 && retries < 5) {
            next = readByte()
            retries++
        }
        if (next < 0) return null
        bytes[i] = next.toByte()
    }
    return bytes.decodeToString()[0]
}

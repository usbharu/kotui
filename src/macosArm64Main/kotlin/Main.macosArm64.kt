@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import dev.usbharu.kotui.compose.runtime.Utf8ByteDecoder
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
    val utf8 = Utf8ByteDecoder()

    fun emit(events: List<InputEvent>): Boolean {
        for (e in events) if (!onEvent(e)) return false
        return true
    }

    fun emitDecoded(text: String): Boolean {
        for (ch in text) if (!emit(decoder.feed(ch))) return false
        return true
    }

    memScoped {
        val buf = alloc<ByteVar>()
        fun readByte(): Int {
            val n = read(STDIN_FILENO, buf.ptr, 1.convert())
            if (n == 0L) return -1
            if (n < 0) return if (errno == EINTR) -1 else -2
            return buf.value.toInt() and 0xFF
        }

        while (true) {
            val b = readByte()
            if (b == -2) throw IllegalStateException("kotui: terminal input read failed (errno=$errno).")
            if (b < 0) {
                if (!emitDecoded(utf8.flush())) return
                // VTIME timeout or EOF. Flush any pending escape sequence.
                if (decoder.hasPending()) {
                    if (!emit(decoder.flush())) return
                }
                continue
            }
            if (!emitDecoded(utf8.feed(b))) return
        }
    }
}

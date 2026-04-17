@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import kotlinx.cinterop.*
import platform.windows.*

private var originalMode: UInt = 0u
private var hStdin: HANDLE? = null

actual fun enableRawMode() {
    hStdin = GetStdHandle(STD_INPUT_HANDLE)
    if (hStdin == INVALID_HANDLE_VALUE) return

    memScoped {
        val mode = alloc<UIntVar>()
        GetConsoleMode(hStdin, mode.ptr)
        originalMode = mode.value

        // Enable virtual-terminal input so the console emits ANSI escape sequences
        // for keys like arrows, matching UNIX terminals.
        val rawMode = (mode.value and
            (ENABLE_ECHO_INPUT or ENABLE_LINE_INPUT or ENABLE_PROCESSED_INPUT).toUInt().inv()
            ) or ENABLE_VIRTUAL_TERMINAL_INPUT.toUInt()
        SetConsoleMode(hStdin, rawMode)
    }
}

actual fun disableRawMode() {
    hStdin?.let {
        SetConsoleMode(it, originalMode)
    }
}

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val handle = GetStdHandle(STD_INPUT_HANDLE)
    if (handle == INVALID_HANDLE_VALUE) return

    val decoder = AnsiKeyDecoder()

    fun emit(events: List<InputEvent>): Boolean {
        for (e in events) if (!onEvent(e)) return false
        return true
    }

    memScoped {
        val buffer = alloc<WCHARVar>()
        val readChars = alloc<DWORDVar>()

        while (true) {
            if (decoder.hasPending()) {
                val waitResult = WaitForSingleObject(handle, 40u)
                if (waitResult == WAIT_TIMEOUT.toUInt()) {
                    if (!emit(decoder.flush())) return
                    continue
                }
            }
            val ok = ReadConsoleW(handle, buffer.ptr, 1u, readChars.ptr, null)
            if (ok == 0 || readChars.value == 0u) break
            val ch = buffer.value.toInt().toChar()
            if (!emit(decoder.feed(ch))) return
        }
    }
}

@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import kotlinx.cinterop.*
import platform.windows.*

private var originalMode: UInt = 0u
private var hStdin: HANDLE? = null
private var rawModeEnabled = false

actual fun enableRawMode() {
    hStdin = GetStdHandle(STD_INPUT_HANDLE)
    if (hStdin == null || hStdin == INVALID_HANDLE_VALUE) {
        hStdin = null
        throw IllegalStateException("kotui: runTui requires an interactive console input handle.")
    }

    memScoped {
        val mode = alloc<UIntVar>()
        if (GetConsoleMode(hStdin, mode.ptr) == 0) {
            hStdin = null
            throw IllegalStateException("kotui: unable to read console input mode (error=${GetLastError()}).")
        }
        originalMode = mode.value

        // Enable virtual-terminal input so the console emits ANSI escape sequences
        // for keys like arrows, matching UNIX terminals.
        val rawMode = (mode.value and
            (ENABLE_ECHO_INPUT or ENABLE_LINE_INPUT or ENABLE_PROCESSED_INPUT).toUInt().inv()
            ) or ENABLE_VIRTUAL_TERMINAL_INPUT.toUInt()
        if (SetConsoleMode(hStdin, rawMode) == 0) {
            hStdin = null
            throw IllegalStateException("kotui: unable to enable raw console input (error=${GetLastError()}).")
        }
        rawModeEnabled = true
    }
}

actual fun disableRawMode() {
    if (rawModeEnabled) hStdin?.let {
        SetConsoleMode(it, originalMode)
    }
    rawModeEnabled = false
    hStdin = null
}

actual suspend fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val handle = GetStdHandle(STD_INPUT_HANDLE)
    if (handle == null || handle == INVALID_HANDLE_VALUE) {
        throw IllegalStateException("kotui: console input handle became unavailable.")
    }

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
                if (waitResult == WAIT_FAILED.toUInt()) {
                    throw IllegalStateException("kotui: waiting for console input failed (error=${GetLastError()}).")
                }
            }
            val ok = ReadConsoleW(handle, buffer.ptr, 1u, readChars.ptr, null)
            if (ok == 0) {
                throw IllegalStateException("kotui: reading console input failed (error=${GetLastError()}).")
            }
            if (readChars.value == 0u) break
            val ch = buffer.value.toInt().toChar()
            if (!emit(decoder.feed(ch))) return
        }
    }
}

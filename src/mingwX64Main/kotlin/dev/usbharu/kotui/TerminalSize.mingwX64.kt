@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.CONSOLE_SCREEN_BUFFER_INFO
import platform.windows.GetConsoleScreenBufferInfo
import platform.windows.GetStdHandle
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.STD_OUTPUT_HANDLE

actual fun terminalSize(): TerminalSize? = memScoped {
    val handle = GetStdHandle(STD_OUTPUT_HANDLE) ?: return@memScoped null
    if (handle == INVALID_HANDLE_VALUE) return@memScoped null
    val info = alloc<CONSOLE_SCREEN_BUFFER_INFO>()
    if (GetConsoleScreenBufferInfo(handle, info.ptr) == 0) return@memScoped null
    val cols = info.srWindow.Right - info.srWindow.Left + 1
    val rows = info.srWindow.Bottom - info.srWindow.Top + 1
    if (cols <= 0 || rows <= 0) null else TerminalSize(cols, rows)
}

@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.STDOUT_FILENO
import platform.posix.TIOCGWINSZ
import platform.posix.ioctl
import platform.posix.winsize

actual fun terminalSize(): TerminalSize? = memScoped {
    val ws = alloc<winsize>()
    val rc = ioctl(STDOUT_FILENO, TIOCGWINSZ, ws.ptr)
    if (rc != 0) return@memScoped null
    val cols = ws.ws_col.toInt()
    val rows = ws.ws_row.toInt()
    if (cols <= 0 || rows <= 0) null else TerminalSize(cols, rows)
}

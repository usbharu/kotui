package dev.usbharu.kotui

actual fun terminalSize(): TerminalSize? {
    val cols = (js("process.stdout.columns") as? Int) ?: return null
    val rows = (js("process.stdout.rows") as? Int) ?: return null
    if (cols <= 0 || rows <= 0) return null
    return TerminalSize(cols, rows)
}

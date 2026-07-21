package dev.usbharu.kotui

actual fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    val stdout = js("process.stdout")
    val tracker = TerminalSizeChangeTracker(terminalSize())
    val listener: () -> Unit = {
        tracker.changedTo(terminalSize())?.let(onResize)
    }
    stdout.on("resize", listener)
    return object : TerminalResizeWatcher {
        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
            stdout.removeListener("resize", listener)
        }
    }
}

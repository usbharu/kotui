package dev.usbharu.kotui

import sun.misc.Signal
import sun.misc.SignalHandler
import java.util.concurrent.atomic.AtomicBoolean

actual fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    // Prefer SIGWINCH when available (Unix-like JVMs). The signal handler
    // itself must be fast — it runs on the JVM's signal dispatcher thread
    // which serializes all signal delivery, so calling a heavyweight
    // `terminalSize()` (which forks `stty size`) inside the handler can
    // back up under rapid resize storms and freeze the UI. Instead we set
    // a coalescing flag and let a dedicated thread read the size and invoke
    // the callback on a short cadence.
    val winch = try {
        Signal("WINCH")
    } catch (_: IllegalArgumentException) {
        return pollingFallback(onResize)
    }

    val pending = AtomicBoolean(false)
    val handler = SignalHandler { _ -> pending.set(true) }

    val prev = try {
        Signal.handle(winch, handler)
    } catch (_: IllegalArgumentException) {
        return pollingFallback(onResize)
    }

    val thread = Thread {
        var last = terminalSize()
        try {
            while (!Thread.currentThread().isInterrupted) {
                Thread.sleep(30)
                if (pending.get()) {
                    // Debounce: wait for a quiet window before reading size
                    // so a rapid drag coalesces into one callback rather than
                    // dozens of full-screen re-renders that overwhelm the
                    // main loop.
                    while (!Thread.currentThread().isInterrupted) {
                        pending.set(false)
                        Thread.sleep(80)
                        if (!pending.get()) break
                    }
                    val now = terminalSize() ?: continue
                    if (now != last) {
                        last = now
                        onResize(now)
                    }
                }
            }
        } catch (_: InterruptedException) {
            // Normal shutdown path.
        }
    }.apply {
        isDaemon = true
        name = "kotui-sigwinch"
        start()
    }

    return object : TerminalResizeWatcher {
        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
            thread.interrupt()
            try {
                Signal.handle(winch, prev ?: SignalHandler.SIG_DFL)
            } catch (_: Throwable) {
            }
        }
    }
}

private fun pollingFallback(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    val thread = Thread {
        var last = terminalSize()
        try {
            while (!Thread.currentThread().isInterrupted) {
                Thread.sleep(150)
                val now = terminalSize() ?: continue
                if (now != last) {
                    last = now
                    onResize(now)
                }
            }
        } catch (_: InterruptedException) {
            // Normal shutdown path.
        }
    }.apply {
        isDaemon = true
        name = "kotui-resize-poll"
        start()
    }
    return object : TerminalResizeWatcher {
        override fun close() { thread.interrupt() }
    }
}

package dev.usbharu.kotui

import sun.misc.Signal
import sun.misc.SignalHandler

actual fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher {
    // Prefer SIGWINCH when available (Unix-like JVMs). SignalHandler callbacks
    // run on the JVM's signal dispatcher thread, so calling back into regular
    // Kotlin code is safe — no need for the async-signal-safety dance required
    // on Kotlin/Native.
    val winch = try {
        Signal("WINCH")
    } catch (_: IllegalArgumentException) {
        return pollingFallback(onResize)
    }

    val lastRef = java.util.concurrent.atomic.AtomicReference<TerminalSize?>(terminalSize())
    val handler = SignalHandler { _ ->
        val now = terminalSize() ?: return@SignalHandler
        val prevLast = lastRef.getAndSet(now)
        if (now != prevLast) {
            onResize(now)
        }
    }

    val prev = try {
        Signal.handle(winch, handler)
    } catch (_: IllegalArgumentException) {
        return pollingFallback(onResize)
    }

    return object : TerminalResizeWatcher {
        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
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

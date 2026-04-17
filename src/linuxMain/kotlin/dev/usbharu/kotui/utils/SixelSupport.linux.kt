@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui.utils

import kotlinx.cinterop.*
import platform.posix.*

actual object SixelSupport {
    private var cachedValue: TerminalCaps? = null

    actual val cached: TerminalCaps? get() = cachedValue

    actual fun overrideForTesting(caps: TerminalCaps?) {
        cachedValue = caps
    }

    actual fun detect(timeoutMillis: Long): TerminalCaps {
        val probe = probe(timeoutMillis)
        val env = detectCapsFromEnv { name -> getenv(name)?.toKString() }
        val caps = mergeCaps(probe, env)
        cachedValue = caps
        return caps
    }

    private fun probe(timeoutMillis: Long): TerminalCaps? {
        if (isatty(STDIN_FILENO) == 0 || isatty(STDOUT_FILENO) == 0) return null

        val bytes = TERMINAL_PROBE.encodeToByteArray()
        bytes.usePinned { pinned ->
            write(STDOUT_FILENO, pinned.addressOf(0), bytes.size.convert())
        }

        val buf = StringBuilder()
        var terminators = 0
        var budget = timeoutMillis.toInt().coerceAtLeast(0)
        val step = 10

        memScoped {
            val readBuf = alloc<ByteVar>()
            val pfd = alloc<pollfd>()
            pfd.fd = STDIN_FILENO
            pfd.events = POLLIN.toShort()

            while (budget > 0 && terminators < TERMINAL_PROBE_TERMINATORS) {
                pfd.revents = 0
                val wait = minOf(step, budget)
                val r = poll(pfd.ptr, 1.convert(), wait)
                if (r > 0 && (pfd.revents.toInt() and POLLIN) != 0) {
                    val n = read(STDIN_FILENO, readBuf.ptr, 1.convert())
                    if (n <= 0) break
                    val c = (readBuf.value.toInt() and 0xFF).toChar()
                    buf.append(c)
                    if (c == 'c' || c == 't') terminators++
                }
                budget -= wait
            }
        }

        if (buf.isEmpty()) return null
        return parseTerminalResponses(buf.toString())
    }
}

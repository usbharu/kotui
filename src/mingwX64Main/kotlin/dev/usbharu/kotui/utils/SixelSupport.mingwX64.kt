@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

// Windows terminals (Windows Terminal, ConEmu) can support sixel, but probing
// via DA1 requires ENABLE_VIRTUAL_TERMINAL_INPUT and a byte-oriented read path
// that the rest of the codebase does not currently wire up. We still honour
// env-var hints so users on known-good terminals get sixel output.
actual object SixelSupport {
    private var cachedValue: TerminalCaps? = null

    actual val cached: TerminalCaps? get() = cachedValue

    actual fun overrideForTesting(caps: TerminalCaps?) {
        cachedValue = caps
    }

    actual fun detect(timeoutMillis: Long): TerminalCaps {
        normalizedProbeTimeout(timeoutMillis)
        val env = detectCapsFromEnv { getenv(it)?.toKString() }
        val caps = mergeCaps(probe = null, env = env)
        cachedValue = caps
        return caps
    }
}

package dev.usbharu.kotui.utils

// Node.js TTY probing is asynchronous by design; faithfully implementing DA1
// here would require exposing a suspend API. We still honour env-var hints so
// users running under known-good terminals get sixel output.
private external val process: dynamic

actual object SixelSupport {
    private var cachedValue: TerminalCaps? = null

    actual val cached: TerminalCaps? get() = cachedValue

    actual fun overrideForTesting(caps: TerminalCaps?) {
        cachedValue = caps
    }

    actual fun detect(timeoutMillis: Long): TerminalCaps {
        val env = detectCapsFromEnv { name ->
            val v = process.env[name]
            if (v == null || v == undefined) null else v.toString()
        }
        val caps = mergeCaps(probe = null, env = env)
        cachedValue = caps
        return caps
    }
}

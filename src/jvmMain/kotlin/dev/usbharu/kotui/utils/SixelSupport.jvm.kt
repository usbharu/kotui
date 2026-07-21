package dev.usbharu.kotui.utils

actual object SixelSupport {
    @Volatile
    private var cachedValue: TerminalCaps? = null

    actual val cached: TerminalCaps? get() = cachedValue

    actual fun overrideForTesting(caps: TerminalCaps?) {
        cachedValue = caps
    }

    actual fun detect(timeoutMillis: Long): TerminalCaps {
        val probe = probe(normalizedProbeTimeout(timeoutMillis))
        val env = detectCapsFromEnv { System.getenv(it) }
        val caps = mergeCaps(probe, env)
        cachedValue = caps
        return caps
    }

    private fun probe(timeoutMillis: Int): TerminalCaps? {
        if (System.console() == null) return null
        val out = System.out
        val input = System.`in`
        try {
            out.print(TERMINAL_PROBE)
            out.flush()
        } catch (e: Throwable) {
            return null
        }

        val buf = StringBuilder()
        var terminators = 0
        val deadline = System.currentTimeMillis() + timeoutMillis.toLong()
        while (System.currentTimeMillis() < deadline && terminators < TERMINAL_PROBE_TERMINATORS) {
            try {
                if (input.available() > 0) {
                    val b = input.read()
                    if (b == -1) break
                    val c = b.toChar()
                    buf.append(c)
                    if (c == 'c' || c == 't') terminators++
                } else {
                    Thread.sleep(5)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Throwable) {
                break
            }
        }
        if (buf.isEmpty()) return null
        return parseTerminalResponses(buf.toString())
    }
}

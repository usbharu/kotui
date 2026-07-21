package dev.usbharu.kotui.utils

/** Terminal capabilities probed at runtime. */
data class TerminalCaps(
    val sixelSupported: Boolean,
    /** Whether the terminal supports the Kitty graphics protocol. */
    val kittySupported: Boolean = false,
    /** Terminal cell width in pixels. Defaults to 10 when the terminal does not report. */
    val cellPixelWidth: Int = 10,
    /** Terminal cell height in pixels. Defaults to 20 when the terminal does not report. */
    val cellPixelHeight: Int = 20,
) {
    init {
        require(cellPixelWidth > 0 && cellPixelHeight > 0) { "terminal cell pixel dimensions must be positive" }
    }

    companion object {
        /** Safe fallback used when detection cannot be performed. */
        val UNSUPPORTED = TerminalCaps(sixelSupported = false, kittySupported = false)
    }
}

/**
 * Detects whether the attached terminal supports the sixel graphics protocol
 * and what its cell-to-pixel ratio is.
 *
 * The implementation sends DA1 (`CSI c`) and the window-manipulation queries
 * CSI 14 t / CSI 18 t to stdout, then reads responses from stdin with a short
 * timeout. [detect] **must** be called after raw mode has been enabled and
 * before the normal input loop starts consuming stdin, otherwise the replies
 * will be interpreted as key events. The result is cached in [cached].
 */
expect object SixelSupport {
    /** Runs the DA1 / window-size probe once and caches the result. */
    fun detect(timeoutMillis: Long = 200L): TerminalCaps

    /** The last value returned by [detect], or null if [detect] has never run. */
    val cached: TerminalCaps?

    /** Replaces the cached capabilities (primarily for tests). */
    fun overrideForTesting(caps: TerminalCaps?)
}

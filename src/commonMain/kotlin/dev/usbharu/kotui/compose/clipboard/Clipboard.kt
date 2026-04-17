package dev.usbharu.kotui.compose.clipboard

import androidx.compose.runtime.staticCompositionLocalOf
import dev.usbharu.kotui.utils.Ansi

/**
 * Abstraction over a clipboard. Reads are best-effort; terminals generally do not
 * expose a read path over OSC 52 for security, so pasted content is delivered via
 * bracketed paste events instead.
 */
interface Clipboard {
    fun read(): String
    fun write(text: String)
}

/** In-memory clipboard scoped to the app. Useful for tests and headless scenarios. */
class InMemoryClipboard(initial: String = "") : Clipboard {
    private var contents: String = initial
    override fun read(): String = contents
    override fun write(text: String) { contents = text }
}

/**
 * Default clipboard used by [dev.usbharu.kotui.compose.runtime.runTui]. Stores
 * content in-memory so Ctrl+V within the same TUI process works, attempts to invoke
 * the OS clipboard helper (`pbcopy` / `xclip` / `wl-copy` / `clip`), and emits OSC 52
 * as a remote-friendly fallback (works through SSH when the terminal supports it).
 */
class SystemClipboard(
    private val memory: InMemoryClipboard = InMemoryClipboard(),
    private val emitOsc52: Boolean = true,
    private val useSystemHelper: Boolean = true,
) : Clipboard {
    override fun read(): String = memory.read()
    override fun write(text: String) {
        memory.write(text)
        if (useSystemHelper) writeToSystemClipboard(text)
        if (emitOsc52) print(Ansi.osc52Copy(text))
    }
}

/**
 * Platform hook that attempts to place [text] on the OS clipboard via a native helper.
 * Returns false if no helper is available or the command failed.
 */
internal expect fun writeToSystemClipboard(text: String): Boolean

val LocalClipboard = staticCompositionLocalOf<Clipboard> {
    error("No Clipboard provided. Are you inside runTui {}?")
}

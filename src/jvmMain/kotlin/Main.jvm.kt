package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.AnsiKeyDecoder
import dev.usbharu.kotui.compose.runtime.InputEvent
import dev.usbharu.kotui.compose.runtime.Utf8ByteDecoder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private var savedSttyState: String? = null

private fun runStty(cmd: String, captureStdout: Boolean = false): Pair<Int, String?> {
    val proc = ProcessBuilder("sh", "-c", cmd)
        .redirectErrorStream(true)
        .start()
    val out = if (captureStdout) {
        BufferedReader(InputStreamReader(proc.inputStream)).use { it.readLine()?.trim() }
    } else {
        proc.inputStream.close()
        null
    }
    val finished = proc.waitFor(2, TimeUnit.SECONDS)
    if (!finished) {
        proc.destroyForcibly()
        proc.waitFor(500, TimeUnit.MILLISECONDS)
        return -1 to out
    }
    return proc.exitValue() to out
}

actual fun enableRawMode() {
    // Capture current tty settings so we can fully restore them later. If the
    // process is not attached to a tty (CI, Docker w/o -t, piped stdin) this
    // step fails — surface it rather than silently writing escape codes to a
    // terminal we don't actually control.
    val (saveRc, saved) = runStty("stty -g < /dev/tty", captureStdout = true)
    if (saveRc != 0 || saved.isNullOrBlank()) {
        throw IllegalStateException(
            "kotui: unable to read tty state via `stty -g < /dev/tty` (exit=$saveRc). " +
                "runTui requires an interactive terminal."
        )
    }
    savedSttyState = saved

    val (enableRc, _) = runStty("stty raw -echo < /dev/tty")
    if (enableRc != 0) {
        savedSttyState = null
        throw IllegalStateException(
            "kotui: `stty raw -echo < /dev/tty` failed (exit=$enableRc)."
        )
    }
}

actual fun disableRawMode() {
    val saved = savedSttyState
    savedSttyState = null
    if (saved != null) {
        // `stty <saved>` restores the exact flags captured in enableRawMode.
        val (rc, _) = runStty("stty $saved < /dev/tty")
        if (rc == 0) return
    }
    // Fallback: best-effort reset so the terminal stays usable even if the
    // saved state is missing or restoration failed.
    runStty("stty sane < /dev/tty")
}

private const val ESC_TIMEOUT_MS = 40L
private const val ESC_POLL_MS = 5L

actual fun onInputEvent(onEvent: (InputEvent) -> Boolean) {
    val decoder = AnsiKeyDecoder()
    val utf8 = Utf8ByteDecoder()
    val input = System.`in`

    fun emit(events: List<InputEvent>): Boolean {
        for (e in events) {
            if (!onEvent(e)) return false
        }
        return true
    }

    fun emitDecoded(text: String): Boolean {
        for (ch in text) if (!emit(decoder.feed(ch))) return false
        return true
    }

    while (true) {
        if (decoder.hasPending() || utf8.hasPending()) {
            // Wait briefly for continuation bytes. If none arrive, flush pending state
            // (disambiguates ESC alone from ESC-initiated sequences).
            var waited = 0L
            while (input.available() == 0 && waited < ESC_TIMEOUT_MS) {
                Thread.sleep(ESC_POLL_MS)
                waited += ESC_POLL_MS
            }
            if (input.available() == 0) {
                if (!emitDecoded(utf8.flush())) return
                if (!emit(decoder.flush())) return
                continue
            }
        }
        val byte = input.read()
        if (byte < 0) {
            if (!emitDecoded(utf8.flush())) return
            emit(decoder.flush())
            return
        }
        if (!emitDecoded(utf8.feed(byte))) return
    }
}

actual fun terminalSize(): TerminalSize? {
    // Primary: `stty size < /dev/tty` prints "<rows> <cols>". Reliable on any
    // TTY-bound process (including Gradle JavaExec when standardInput is tty).
    runCatching {
        val proc = ProcessBuilder("sh", "-c", "stty size < /dev/tty")
            .redirectErrorStream(true)
            .start()
        val out = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readLine()?.trim() }
        proc.waitFor()
        if (!out.isNullOrEmpty()) {
            parseTerminalSizeOutput(out)?.let { return it }
        }
    }
    // Fallback: environment variables exposed by some shells.
    val envCols = System.getenv("COLUMNS")?.toIntOrNull()
    val envRows = System.getenv("LINES")?.toIntOrNull()
    if (envCols != null && envRows != null && envCols > 0 && envRows > 0) {
        return TerminalSize(envCols, envRows)
    }
    return null
}

internal fun parseTerminalSizeOutput(output: String): TerminalSize? {
    val parts = output.trim().split(Regex("\\s+"))
    if (parts.size != 2) return null
    val rows = parts[0].toIntOrNull() ?: return null
    val cols = parts[1].toIntOrNull() ?: return null
    if (rows <= 0 || cols <= 0) return null
    return TerminalSize(cols, rows)
}

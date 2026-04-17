package dev.usbharu.kotui.compose.clipboard

import java.util.concurrent.TimeUnit

internal actual fun writeToSystemClipboard(text: String): Boolean {
    val os = System.getProperty("os.name").lowercase()
    val cmd: Array<String> = when {
        "mac" in os || "darwin" in os -> arrayOf("pbcopy")
        "win" in os -> arrayOf("cmd", "/c", "clip")
        else -> arrayOf(
            "sh", "-c",
            "command -v wl-copy >/dev/null 2>&1 && wl-copy || " +
                "command -v xclip >/dev/null 2>&1 && xclip -selection clipboard -in || " +
                "command -v xsel >/dev/null 2>&1 && xsel --clipboard --input"
        )
    }
    return try {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        p.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        if (!p.waitFor(2, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            false
        } else {
            p.exitValue() == 0
        }
    } catch (_: Exception) {
        false
    }
}

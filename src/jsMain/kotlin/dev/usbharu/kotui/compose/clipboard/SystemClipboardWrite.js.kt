package dev.usbharu.kotui.compose.clipboard

internal actual fun writeToSystemClipboard(text: String): Boolean {
    return try {
        val child = js("require('child_process')")
        val platform = js("process.platform").toString()
        val opts: dynamic = js("({})")
        opts.input = text
        val proc = when (platform) {
            "darwin" -> child.spawnSync("pbcopy", arrayOf<String>(), opts)
            "win32" -> child.spawnSync("clip", arrayOf<String>(), opts)
            else -> {
                val shellCmd = "command -v wl-copy >/dev/null 2>&1 && wl-copy || " +
                    "command -v xclip >/dev/null 2>&1 && xclip -selection clipboard -in || " +
                    "command -v xsel >/dev/null 2>&1 && xsel --clipboard --input"
                child.spawnSync("sh", arrayOf("-c", shellCmd), opts)
            }
        }
        proc.status == 0
    } catch (_: Throwable) {
        false
    }
}

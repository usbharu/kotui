@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui.compose.clipboard

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.fwrite
import platform.posix.pclose
import platform.posix.popen

internal actual fun writeToSystemClipboard(text: String): Boolean {
    // popen invokes /bin/sh so we can chain helpers.
    val cmd = "command -v wl-copy >/dev/null 2>&1 && wl-copy || " +
        "command -v xclip >/dev/null 2>&1 && xclip -selection clipboard -in || " +
        "command -v xsel >/dev/null 2>&1 && xsel --clipboard --input"
    val pipe = popen(cmd, "w") ?: return false
    val bytes = text.encodeToByteArray()
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), pipe)
        }
    }
    val rc = pclose(pipe)
    return rc == 0
}

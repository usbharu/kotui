@file:OptIn(ExperimentalForeignApi::class)
package dev.usbharu.kotui.compose.clipboard

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix._popen
import platform.posix._pclose
import platform.posix.fwrite

internal actual fun writeToSystemClipboard(text: String): Boolean {
    val pipe = _popen("clip", "w") ?: return false
    val bytes = text.encodeToByteArray()
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), pipe)
        }
    }
    val rc = _pclose(pipe)
    return rc == 0
}

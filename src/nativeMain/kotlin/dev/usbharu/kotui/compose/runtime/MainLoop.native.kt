package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import platform.posix.exit

internal actual fun runMainLoop(block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
}

internal actual fun forceExit(status: Int): Nothing {
    exit(status)
    error("unreachable")
}

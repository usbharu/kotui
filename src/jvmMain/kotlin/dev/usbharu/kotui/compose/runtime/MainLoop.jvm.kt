package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

internal actual fun runMainLoop(
    block: suspend CoroutineScope.() -> Unit,
    onComplete: (Throwable?) -> Unit,
): Boolean {
    runBlocking { block() }
    return true
}

internal actual fun forceExit(status: Int): Nothing {
    kotlin.system.exitProcess(status)
}

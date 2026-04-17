package dev.usbharu.kotui.compose.runtime

import kotlin.time.TimeSource

private val timeSource = TimeSource.Monotonic
private val startMark = timeSource.markNow()

actual fun frameTimeNanos(): Long = startMark.elapsedNow().inWholeNanoseconds

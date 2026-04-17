package dev.usbharu.kotui.compose.runtime

actual fun frameTimeNanos(): Long = (js("Date.now()").unsafeCast<Double>()).toLong() * 1_000_000L

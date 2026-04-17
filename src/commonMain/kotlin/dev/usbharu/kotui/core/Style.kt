package dev.usbharu.kotui.core

data class Style(
    val fg: String? = null,
    val bg: String? = null,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val reverse: Boolean = false
)

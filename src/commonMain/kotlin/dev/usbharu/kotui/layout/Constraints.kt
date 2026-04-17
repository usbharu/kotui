package dev.usbharu.kotui.layout

data class Constraints(
    val maxWidth: Int,
    val maxHeight: Int,
    val minWidth: Int = 0,
    val minHeight: Int = 0
)

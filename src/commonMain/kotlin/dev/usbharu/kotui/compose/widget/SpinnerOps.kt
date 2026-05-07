package dev.usbharu.kotui.compose.widget

internal fun spinnerFrame(frame: Int, chars: List<Char>): String =
    if (chars.isEmpty()) " " else chars[((frame % chars.size) + chars.size) % chars.size].toString()

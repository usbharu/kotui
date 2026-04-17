package dev.usbharu.kotui.core

data class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(px: Int, py: Int): Boolean =
        px in x until (x + width) && py in y until (y + height)

    companion object {
        val ZERO = Rect(0, 0, 0, 0)
    }
}

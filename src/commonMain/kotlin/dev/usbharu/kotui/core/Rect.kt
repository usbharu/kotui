package dev.usbharu.kotui.core

data class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(px: Int, py: Int): Boolean =
        width > 0 && height > 0 &&
            px.toLong() >= x.toLong() && px.toLong() < x.toLong() + width.toLong() &&
            py.toLong() >= y.toLong() && py.toLong() < y.toLong() + height.toLong()

    companion object {
        val ZERO = Rect(0, 0, 0, 0)
    }
}

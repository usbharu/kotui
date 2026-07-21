package dev.usbharu.kotui.image

internal fun hasPngSignature(bytes: ByteArray): Boolean =
    bytes.size >= 8 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'N'.code.toByte() &&
        bytes[3] == 'G'.code.toByte() &&
        bytes[4] == 0x0D.toByte() &&
        bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() &&
        bytes[7] == 0x0A.toByte()

internal fun rgbaByteCount(width: Int, height: Int): Int {
    require(width > 0 && height > 0) { "image dimensions must be positive" }
    val pixels = width.toLong() * height.toLong()
    require(pixels <= Int.MAX_VALUE / 4L) { "image is too large" }
    return (pixels * 4L).toInt()
}

/** Converts premultiplied RGBA bytes to straight-alpha RGBA in place. */
internal fun unpremultiplyRgbaInPlace(rgba: ByteArray) {
    require(rgba.size % 4 == 0) { "rgba byte count must be divisible by four" }
    var i = 0
    while (i < rgba.size) {
        val alpha = rgba[i + 3].toInt() and 0xFF
        when (alpha) {
            0 -> {
                rgba[i] = 0
                rgba[i + 1] = 0
                rgba[i + 2] = 0
            }
            255 -> Unit
            else -> {
                for (channel in 0..2) {
                    val premultiplied = rgba[i + channel].toInt() and 0xFF
                    val straight = ((premultiplied * 255 + alpha / 2) / alpha).coerceAtMost(255)
                    rgba[i + channel] = straight.toByte()
                }
            }
        }
        i += 4
    }
}

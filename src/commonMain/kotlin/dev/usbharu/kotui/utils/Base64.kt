package dev.usbharu.kotui.utils

internal object Base64 {
    private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(text: String): String {
        val bytes = text.encodeToByteArray()
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i + 3 <= bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            out.append(TABLE[b0 ushr 2])
            out.append(TABLE[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            out.append(TABLE[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
            out.append(TABLE[b2 and 0x3F])
            i += 3
        }
        val rem = bytes.size - i
        if (rem == 1) {
            val b0 = bytes[i].toInt() and 0xFF
            out.append(TABLE[b0 ushr 2])
            out.append(TABLE[(b0 and 0x03) shl 4])
            out.append("==")
        } else if (rem == 2) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            out.append(TABLE[b0 ushr 2])
            out.append(TABLE[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            out.append(TABLE[(b1 and 0x0F) shl 2])
            out.append('=')
        }
        return out.toString()
    }
}

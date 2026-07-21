package dev.usbharu.kotui.compose.runtime

/** Incremental strict UTF-8 decoder for byte-oriented terminal backends. */
internal class Utf8ByteDecoder {
    private var codePoint = 0
    private var remaining = 0
    private var minimum = 0

    fun hasPending(): Boolean = remaining != 0

    fun feed(byte: Int): String {
        require(byte in 0..255) { "byte must be in 0..255" }
        if (remaining == 0) return start(byte)

        if (byte !in 0x80..0xBF) {
            reset()
            return "\uFFFD" + start(byte)
        }

        codePoint = (codePoint shl 6) or (byte and 0x3F)
        remaining--
        if (remaining != 0) return ""

        val completed = codePoint
        val valid = completed >= minimum &&
            completed <= 0x10FFFF &&
            completed !in 0xD800..0xDFFF
        reset()
        return if (valid) encodeCodePoint(completed) else "\uFFFD"
    }

    fun flush(): String {
        if (remaining == 0) return ""
        reset()
        return "\uFFFD"
    }

    private fun start(byte: Int): String = when (byte) {
        in 0x00..0x7F -> byte.toChar().toString()
        in 0xC2..0xDF -> begin(byte and 0x1F, 1, 0x80)
        in 0xE0..0xEF -> begin(byte and 0x0F, 2, 0x800)
        in 0xF0..0xF4 -> begin(byte and 0x07, 3, 0x10000)
        else -> "\uFFFD"
    }

    private fun begin(initial: Int, continuationCount: Int, minimumValue: Int): String {
        codePoint = initial
        remaining = continuationCount
        minimum = minimumValue
        return ""
    }

    private fun reset() {
        codePoint = 0
        remaining = 0
        minimum = 0
    }

    private fun encodeCodePoint(value: Int): String {
        if (value <= 0xFFFF) return value.toChar().toString()
        val adjusted = value - 0x10000
        val high = (0xD800 + (adjusted ushr 10)).toChar()
        val low = (0xDC00 + (adjusted and 0x3FF)).toChar()
        return "$high$low"
    }
}

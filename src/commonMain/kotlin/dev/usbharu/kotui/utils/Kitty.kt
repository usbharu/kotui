package dev.usbharu.kotui.utils

/**
 * Encodes raw pixel data into the Kitty graphics protocol (kgp).
 *
 * Used by Kitty, Ghostty, WezTerm, Konsole (recent) and other terminals that
 * implement https://sw.kovidgoyal.net/kitty/graphics-protocol/. Unlike sixel,
 * kgp does not quantize colours — the image ships verbatim as base64-encoded
 * RGBA and the terminal handles scaling.
 */
object Kitty {
    private const val APC = "\u001B_G"
    private const val ST = "\u001B\\"

    /** Maximum base64 payload per APC message. The protocol requires ≤ 4096. */
    private const val CHUNK_CHARS = 4096

    /**
     * Encodes [rgba] (row-major, 4 bytes per pixel) of size [width] × [height]
     * into a sequence of APC messages that display the image at the current
     * cursor position. `q=2` is set so the terminal does not reply to us.
     *
     * `C=1` suppresses cursor movement so the caller can place multiple images
     * without the cursor drifting between them.
     */
    /** Escape sequence that removes every kgp image currently on the screen. */
    const val DELETE_ALL: String = "\u001B_Ga=d,d=A;\u001B\\"

    fun encode(rgba: ByteArray, width: Int, height: Int): String {
        require(width > 0 && height > 0) { "width and height must be positive" }
        val pixelCount = width.toLong() * height.toLong()
        require(pixelCount <= Int.MAX_VALUE / 4L) { "image is too large" }
        val pixelBytesLong = pixelCount * 4L
        val pixelBytes = pixelBytesLong.toInt()
        require(rgba.size >= pixelBytes) { "rgba buffer too small: ${rgba.size} < $pixelBytes" }

        val b64 = Base64.encode(rgba, 0, pixelBytes)
        val out = StringBuilder(b64.length + 128)

        var i = 0
        var first = true
        while (i < b64.length) {
            val end = minOf(i + CHUNK_CHARS, b64.length)
            val more = if (end < b64.length) 1 else 0

            out.append(APC)
            if (first) {
                out.append("a=T,f=32,s=").append(width)
                    .append(",v=").append(height)
                    .append(",q=2,C=1,m=").append(more)
                first = false
            } else {
                out.append("m=").append(more)
            }
            out.append(';')
            out.append(b64, i, end)
            out.append(ST)
            i = end
        }
        return out.toString()
    }
}

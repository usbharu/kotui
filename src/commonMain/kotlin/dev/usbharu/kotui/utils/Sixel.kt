package dev.usbharu.kotui.utils

/**
 * Encodes raw pixel data into the sixel escape sequence understood by xterm,
 * wezterm, foot, mlterm, Ghostty, Konsole, mintty and other compatible terminals.
 *
 * The encoder does not perform image decoding — callers pass an RGBA byte array
 * (4 bytes per pixel, row-major, top-to-bottom) or a pre-quantized palette.
 */
object Sixel {
    private const val ESC = "\u001B"
    private const val ST = "\u001B\\"
    private const val DCS_Q = "${ESC}Pq"

    /**
     * Encodes [rgba] (row-major, 4 bytes per pixel) of size [width] × [height]
     * into a sixel escape string. The alpha channel is ignored.
     *
     * If the image contains no more than [maxColors] distinct colours the exact
     * palette is used; otherwise a uniform 3/3/2-bit RGB quantization is applied.
     */
    fun encode(rgba: ByteArray, width: Int, height: Int, maxColors: Int = 256): String {
        require(width > 0 && height > 0) { "width and height must be positive" }
        require(rgba.size >= width * height * 4) { "rgba buffer too small: ${rgba.size} < ${width * height * 4}" }
        require(maxColors in 2..256) { "maxColors must be in 2..256" }

        val indices = IntArray(width * height)
        val palette = quantize(rgba, width, height, maxColors, indices)
        return encodeIndexed(indices, width, height, palette)
    }

    /**
     * Encodes a pre-quantized image. [pixels] holds palette indices (row-major),
     * [palette] holds packed 0xRRGGBB values, one per index.
     */
    fun encodeIndexed(pixels: IntArray, width: Int, height: Int, palette: IntArray): String {
        require(pixels.size >= width * height) { "pixels buffer too small" }
        require(palette.isNotEmpty()) { "palette must not be empty" }

        val out = StringBuilder()
        out.append(DCS_Q)
        out.append("\"1;1;").append(width).append(';').append(height)

        for (i in palette.indices) {
            val rgb = palette[i]
            val r = (rgb ushr 16) and 0xFF
            val g = (rgb ushr 8) and 0xFF
            val b = rgb and 0xFF
            out.append('#').append(i)
                .append(";2;").append(r * 100 / 255)
                .append(';').append(g * 100 / 255)
                .append(';').append(b * 100 / 255)
        }

        val strip = IntArray(width)
        var y = 0
        while (y < height) {
            val rows = minOf(6, height - y)
            val colorsInStrip = collectStripColors(pixels, width, y, rows, palette.size)

            var colorsEmitted = 0
            for (color in colorsInStrip) {
                if (colorsEmitted > 0) out.append('$')
                out.append('#').append(color)
                for (x in 0 until width) {
                    var bits = 0
                    for (r in 0 until rows) {
                        if (pixels[(y + r) * width + x] == color) bits = bits or (1 shl r)
                    }
                    strip[x] = bits
                }
                emitRle(out, strip, width)
                colorsEmitted++
            }

            if (y + rows < height) out.append('-')
            y += rows
        }

        out.append(ST)
        return out.toString()
    }

    private fun collectStripColors(pixels: IntArray, width: Int, y: Int, rows: Int, paletteSize: Int): IntArray {
        val seen = BooleanArray(paletteSize)
        val order = IntArray(paletteSize)
        var count = 0
        for (r in 0 until rows) {
            val rowStart = (y + r) * width
            for (x in 0 until width) {
                val c = pixels[rowStart + x]
                if (c >= 0 && c < paletteSize && !seen[c]) {
                    seen[c] = true
                    order[count++] = c
                }
            }
        }
        return order.copyOf(count)
    }

    private fun emitRle(out: StringBuilder, strip: IntArray, width: Int) {
        var x = 0
        while (x < width) {
            val bits = strip[x]
            var run = 1
            while (x + run < width && strip[x + run] == bits) run++
            val ch = (0x3F + bits).toChar()
            if (run >= 4) {
                out.append('!').append(run).append(ch)
            } else {
                repeat(run) { out.append(ch) }
            }
            x += run
        }
    }

    /**
     * Builds a palette for [rgba] and fills [outIndices] with palette indices.
     * Returns packed 0xRRGGBB palette. Uses exact colours when possible,
     * otherwise falls back to uniform 3/3/2 quantization.
     */
    private fun quantize(
        rgba: ByteArray,
        width: Int,
        height: Int,
        maxColors: Int,
        outIndices: IntArray,
    ): IntArray {
        val n = width * height
        val packed = IntArray(n)
        for (i in 0 until n) {
            val base = i * 4
            val r = rgba[base].toInt() and 0xFF
            val g = rgba[base + 1].toInt() and 0xFF
            val b = rgba[base + 2].toInt() and 0xFF
            packed[i] = (r shl 16) or (g shl 8) or b
        }

        // Try exact palette first (cheap when the image has few colours).
        val exactLookup = HashMap<Int, Int>()
        val exactPalette = IntArray(maxColors)
        var exactCount = 0
        var overflow = false
        for (i in 0 until n) {
            val rgb = packed[i]
            var idx = exactLookup[rgb]
            if (idx == null) {
                if (exactCount >= maxColors) {
                    overflow = true
                    break
                }
                idx = exactCount
                exactLookup[rgb] = idx
                exactPalette[exactCount] = rgb
                exactCount++
            }
            outIndices[i] = idx
        }

        if (!overflow) {
            return exactPalette.copyOf(exactCount)
        }

        // Uniform 3-3-2 quantization (256 buckets). We only populate buckets
        // that actually appear and renumber them compactly so the palette is
        // as short as possible even under this branch.
        val bucketToIndex = IntArray(256) { -1 }
        val palette = IntArray(256)
        var count = 0
        for (i in 0 until n) {
            val rgb = packed[i]
            val r = (rgb ushr 16) and 0xFF
            val g = (rgb ushr 8) and 0xFF
            val b = rgb and 0xFF
            val bucket = ((r and 0xE0)) or ((g and 0xE0) ushr 3) or ((b and 0xC0) ushr 6)
            var idx = bucketToIndex[bucket]
            if (idx == -1) {
                idx = count
                bucketToIndex[bucket] = idx
                val br = bucket and 0xE0
                val bg = (bucket and 0x1C) shl 3
                val bb = (bucket and 0x03) shl 6
                // Centre each bucket on its midpoint for better average colour.
                val cr = (br or 0x10).coerceAtMost(0xFF)
                val cg = (bg or 0x10).coerceAtMost(0xFF)
                val cb = (bb or 0x20).coerceAtMost(0xFF)
                palette[count] = (cr shl 16) or (cg shl 8) or cb
                count++
            }
            outIndices[i] = idx
        }
        return palette.copyOf(count)
    }
}

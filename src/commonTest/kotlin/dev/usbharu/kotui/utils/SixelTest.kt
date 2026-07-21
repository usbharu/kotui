package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SixelTest {
    private fun rgbaOf(vararg pixels: Int): ByteArray {
        val out = ByteArray(pixels.size * 4)
        for (i in pixels.indices) {
            val rgb = pixels[i]
            out[i * 4]     = ((rgb ushr 16) and 0xFF).toByte()
            out[i * 4 + 1] = ((rgb ushr 8) and 0xFF).toByte()
            out[i * 4 + 2] = (rgb and 0xFF).toByte()
            out[i * 4 + 3] = 0xFF.toByte()
        }
        return out
    }

    @Test
    fun `encodes a 2x2 two-colour image with exact palette`() {
        val rgba = rgbaOf(
            0xFF0000, 0xFF0000,
            0x0000FF, 0x0000FF,
        )
        val actual = Sixel.encode(rgba, 2, 2)
        val expected = "\u001BPq\"1;1;2;2" +
            "#0;2;100;0;0" +
            "#1;2;0;0;100" +
            "#0@@" +
            "\$#1AA" +
            "\u001B\\"
        assertEquals(expected, actual)
    }

    @Test
    fun `uses RLE when a column pattern repeats four or more times`() {
        val rgba = rgbaOf(0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000)
        val actual = Sixel.encode(rgba, 10, 1)
        val expected = "\u001BPq\"1;1;10;1#0;2;100;0;0#0!10@\u001B\\"
        assertEquals(expected, actual)
    }

    @Test
    fun `does not use RLE for short runs`() {
        val rgba = rgbaOf(0xFF0000, 0xFF0000, 0xFF0000)
        val actual = Sixel.encode(rgba, 3, 1)
        assertTrue(actual.contains("#0@@@"), "expected uncompressed run '@@@' in: $actual")
        assertTrue(!actual.contains("!3"), "did not expect RLE marker for run of 3: $actual")
    }

    @Test
    fun `splits output into 6-row strips`() {
        // 7 rows of a single red column forces one full strip (rows 0..5) plus a
        // trailing 1-row strip separated by '-'.
        val pixels = IntArray(7) { 0xFF0000 }
        val rgba = rgbaOf(*pixels)
        val actual = Sixel.encode(rgba, 1, 7)
        val expected = "\u001BPq\"1;1;1;7#0;2;100;0;0" +
            "#0~" +    // strip 0: 6 rows all set => 0x3F bits => '~'
            "-" +
            "#0@" +    // strip 1: 1 row set => bit0 => '@'
            "\u001B\\"
        assertEquals(expected, actual)
    }

    @Test
    fun `strip with multiple colours uses carriage-return between them`() {
        // 2x1 image: red then blue, single strip.
        val rgba = rgbaOf(0xFF0000, 0x0000FF)
        val actual = Sixel.encode(rgba, 2, 1)
        val expected = "\u001BPq\"1;1;2;1" +
            "#0;2;100;0;0" +
            "#1;2;0;0;100" +
            "#0@?" +   // red present in col 0 only
            "\$#1?@" + // blue present in col 1 only
            "\u001B\\"
        assertEquals(expected, actual)
    }

    @Test
    fun `falls back to uniform quantization when colours exceed maxColors`() {
        // 257 unique colours with maxColors=256 triggers 3-3-2 quantization.
        val width = 257
        val pixels = IntArray(width) { i -> (i shl 8) or (i and 0xFF) }
        val rgba = rgbaOf(*pixels)
        val actual = Sixel.encode(rgba, width, 1, maxColors = 256)
        assertTrue(actual.startsWith("\u001BPq"), "header missing: ${actual.take(20)}")
        assertTrue(actual.endsWith("\u001B\\"), "terminator missing")
        // Count palette definitions. In 3-3-2 space many colours collapse, so
        // the palette must be bounded by the 256 buckets.
        val paletteCount = Regex("#\\d+;2;").findAll(actual).count()
        assertTrue(paletteCount in 1..256, "palette count out of range: $paletteCount")
    }

    @Test
    fun `rejects invalid inputs`() {
        assertFailsWith<IllegalArgumentException> { Sixel.encode(ByteArray(16), 0, 2) }
        assertFailsWith<IllegalArgumentException> { Sixel.encode(ByteArray(4), 2, 2) }  // buffer too small
        assertFailsWith<IllegalArgumentException> { Sixel.encode(ByteArray(16), 2, 2, maxColors = 0) }
        assertFailsWith<IllegalArgumentException> { Sixel.encode(ByteArray(4), Int.MAX_VALUE, Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { Sixel.encodeIndexed(intArrayOf(0), 0, 1, intArrayOf(0)) }
        assertFailsWith<IllegalArgumentException> {
            Sixel.encodeIndexed(intArrayOf(0), Int.MAX_VALUE, Int.MAX_VALUE, intArrayOf(0))
        }
        assertFailsWith<IllegalArgumentException> { Sixel.encodeIndexed(intArrayOf(0), 2, 1, intArrayOf(0)) }
        assertFailsWith<IllegalArgumentException> { Sixel.encodeIndexed(intArrayOf(1), 1, 1, intArrayOf(0)) }
        assertFailsWith<IllegalArgumentException> { Sixel.encodeIndexed(intArrayOf(0), 1, 1, IntArray(257)) }
    }

    @Test
    fun `one-color palette limit is supported`() {
        val rgba = rgbaOf(0xFF0000, 0x00FF00)

        val actual = Sixel.encode(rgba, 2, 1, maxColors = 1)

        assertEquals(1, Regex("#\\d+;2;").findAll(actual).count())
    }

    @Test
    fun `quantized palette respects a small maxColors limit`() {
        val rgba = rgbaOf(0x000000, 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF)

        val actual = Sixel.encode(rgba, 5, 1, maxColors = 2)

        val paletteCount = Regex("#\\d+;2;").findAll(actual).count()
        assertTrue(paletteCount in 1..2, "palette exceeds requested limit: $paletteCount")
    }

    @Test
    fun `fully transparent rgba does not emit painted sixels`() {
        val actual = Sixel.encode(byteArrayOf(255.toByte(), 0, 0, 0), 1, 1)

        assertEquals(0, Regex("#\\d+;2;").findAll(actual).count())
        assertTrue(!actual.contains("#0@"), "transparent pixel was painted: $actual")
    }

    @Test
    fun `transparent colors do not consume requested palette slots`() {
        val rgba = byteArrayOf(
            255.toByte(), 0, 0, 0,
            0, 255.toByte(), 0, 255.toByte(),
            0, 0, 255.toByte(), 0,
        )

        val actual = Sixel.encode(rgba, 3, 1, maxColors = 2)

        assertEquals(1, Regex("#\\d+;2;").findAll(actual).count())
    }

    @Test
    fun `indexed encoder accepts minusOne as transparent pixel`() {
        val actual = Sixel.encodeIndexed(intArrayOf(-1, 0), 2, 1, intArrayOf(0xFF0000))

        assertTrue(actual.contains("#0?@"), "expected only second column to be painted: $actual")
    }

    @Test
    fun `empty indexed palette is accepted only when every pixel is transparent`() {
        Sixel.encodeIndexed(intArrayOf(-1), 1, 1, intArrayOf())
        assertFailsWith<IllegalArgumentException> {
            Sixel.encodeIndexed(intArrayOf(0), 1, 1, intArrayOf())
        }
    }
}

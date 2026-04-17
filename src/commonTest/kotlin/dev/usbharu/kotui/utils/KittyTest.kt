package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KittyTest {
    @Test
    fun singleChunkForSmallImage() {
        // 2x2 RGBA red image
        val rgba = ByteArray(2 * 2 * 4)
        for (i in 0 until 4) {
            rgba[i * 4]     = 0xFF.toByte()
            rgba[i * 4 + 1] = 0
            rgba[i * 4 + 2] = 0
            rgba[i * 4 + 3] = 0xFF.toByte()
        }
        val encoded = Kitty.encode(rgba, 2, 2)

        assertTrue(encoded.startsWith("\u001B_G"), "missing APC prefix")
        assertTrue(encoded.endsWith("\u001B\\"), "missing ST terminator")
        assertTrue(encoded.contains("a=T"), "missing transmit action")
        assertTrue(encoded.contains("f=32"), "missing RGBA format")
        assertTrue(encoded.contains("s=2"), "missing width")
        assertTrue(encoded.contains("v=2"), "missing height")
        assertTrue(encoded.contains("m=0"), "expected final chunk marker")
        assertTrue(encoded.contains("q=2"), "expected quiet level")
        // Only one APC message for small payload
        assertEquals(1, encoded.windowed(3).count { it == "\u001B_G" })
    }

    @Test
    fun chunksPayloadsLargerThan4096Chars() {
        // 100x100 RGBA = 40_000 bytes. base64 is ~53_334 chars -> multiple chunks.
        val rgba = ByteArray(100 * 100 * 4) { (it and 0xFF).toByte() }
        val encoded = Kitty.encode(rgba, 100, 100)

        val chunks = encoded.split("\u001B_G").filter { it.isNotEmpty() }
        assertTrue(chunks.size >= 2, "expected multiple APC messages, got ${chunks.size}")

        // First chunk should declare a=T and m=1
        assertTrue(chunks.first().contains("a=T"))
        assertTrue(chunks.first().contains("m=1"))
        // Last chunk should have m=0
        assertTrue(chunks.last().contains("m=0;"), "last chunk must be final: ${chunks.last().take(30)}")
    }

    @Test
    fun rejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> { Kitty.encode(ByteArray(16), 0, 2) }
        assertFailsWith<IllegalArgumentException> { Kitty.encode(ByteArray(4), 2, 2) }
    }
}

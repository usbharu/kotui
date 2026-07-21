package dev.usbharu.kotui.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Utf8ByteDecoderTest {
    private fun decode(vararg bytes: Int): String {
        val decoder = Utf8ByteDecoder()
        return buildString {
            bytes.forEach { append(decoder.feed(it)) }
            append(decoder.flush())
        }
    }

    @Test
    fun preservesSupplementaryCodePointAsSurrogatePair() {
        assertEquals("😀", decode(0xF0, 0x9F, 0x98, 0x80))
    }

    @Test
    fun truncatedSequenceFlushesAsReplacementInsteadOfInventingBytes() {
        val decoder = Utf8ByteDecoder()
        assertEquals("", decoder.feed(0xE3))
        assertTrue(decoder.hasPending())
        assertEquals("�", decoder.flush())
        assertFalse(decoder.hasPending())
    }

    @Test
    fun invalidContinuationDoesNotConsumeFollowingAscii() {
        assertEquals("�A", decode(0xE3, 0x41))
    }

    @Test
    fun rejectsOverlongEncoding() {
        assertEquals("��", decode(0xC0, 0xAF))
    }

    @Test
    fun rejectsUtf8EncodedSurrogate() {
        assertEquals("�", decode(0xED, 0xA0, 0x80))
    }

    @Test
    fun rejectsCodePointAboveUnicodeMaximum() {
        assertEquals("����", decode(0xF5, 0x80, 0x80, 0x80))
    }

    @Test
    fun decoderRecoversAfterMalformedSequence() {
        assertEquals("�(€", decode(0xE2, 0x28, 0xE2, 0x82, 0xAC))
    }
}

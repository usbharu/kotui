package dev.usbharu.kotui.render

import dev.usbharu.kotui.core.Style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderBufferTest {
    @Test
    fun asciiWrite() {
        val buf = RenderBuffer(10, 1)
        buf.writeString(0, 0, "hi", Style(), 0)
        assertEquals("h", buf.get(0, 0).content)
        assertEquals("i", buf.get(1, 0).content)
        assertEquals(1, buf.get(0, 0).width)
    }

    @Test
    fun cjkOccupiesTwoCells() {
        val buf = RenderBuffer(10, 1)
        buf.writeString(0, 0, "あい", Style(), 0)
        assertEquals("あ", buf.get(0, 0).content)
        assertEquals(2, buf.get(0, 0).width)
        assertFalse(buf.get(0, 0).isContinuation)

        assertTrue(buf.get(1, 0).isContinuation)
        assertEquals(0, buf.get(1, 0).width)

        assertEquals("い", buf.get(2, 0).content)
        assertTrue(buf.get(3, 0).isContinuation)
    }

    @Test
    fun emojiStoredIntactInSingleCellPair() {
        val buf = RenderBuffer(10, 1)
        val emoji = "\uD83D\uDC4D" // 👍
        buf.writeString(0, 0, emoji, Style(), 0)
        assertEquals(emoji, buf.get(0, 0).content)
        assertEquals(2, buf.get(0, 0).width)
        assertTrue(buf.get(1, 0).isContinuation)
    }

    @Test
    fun combiningMarkAttachesToPreviousCell() {
        val buf = RenderBuffer(10, 1)
        buf.writeString(0, 0, "e\u0301", Style(), 0) // é
        assertEquals("e\u0301", buf.get(0, 0).content)
        assertEquals(1, buf.get(0, 0).width)
    }

    @Test
    fun wideCharNotPlacedAtLastColumnFallsBackToSpace() {
        val buf = RenderBuffer(3, 1)
        // position 2 is the last column; width-2 char cannot fit
        buf.writeString(2, 0, "あ", Style(), 0)
        // expect space or at least no continuation past the right edge
        assertEquals(" ", buf.get(2, 0).content)
    }

    @Test
    fun overwritingWideCharClearsContinuation() {
        val buf = RenderBuffer(5, 1)
        buf.writeString(0, 0, "あ", Style(), 0)
        assertTrue(buf.get(1, 0).isContinuation)

        // overwrite the right half with an ascii char at higher z
        buf.set(1, 0, 'X', Style(), 1)
        assertEquals("X", buf.get(1, 0).content)
        // The original wide-char left cell should have been cleared (logically dangling)
        assertFalse(buf.get(1, 0).isContinuation)
    }

    @Test
    fun clear() {
        val buf = RenderBuffer(3, 1)
        buf.writeString(0, 0, "abc", Style(), 0)
        buf.clear()
        assertEquals(" ", buf.get(0, 0).content)
        assertEquals(" ", buf.get(1, 0).content)
    }
}

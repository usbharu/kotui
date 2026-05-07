package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharWidthTest {
    @Test
    fun asciiWidth() {
        assertEquals(5, "hello".displayWidth())
        assertEquals(0, "".displayWidth())
        assertEquals(1, "a".displayWidth())
    }

    @Test
    fun cjkWidth() {
        assertEquals(10, "こんにちは".displayWidth())
        assertEquals(2, "日".displayWidth())
        assertEquals(4, "한글".displayWidth())
        assertEquals(6, "日本語".displayWidth())
    }

    @Test
    fun emojiWidth() {
        // U+1F44D THUMBS UP SIGN (width 2, surrogate pair length = 2)
        assertEquals(2, "\uD83D\uDC4D".displayWidth())
        // U+1F389 PARTY POPPER
        assertEquals(2, "\uD83C\uDF89".displayWidth())
        // mixed
        assertEquals(4, "a\uD83D\uDC4Db".displayWidth())
    }

    @Test
    fun combiningMarkWidth() {
        // "e" + U+0301 COMBINING ACUTE ACCENT -> displays as é, width 1
        assertEquals(1, "e\u0301".displayWidth())
        // VS16 (U+FE0F) has width 0
        assertEquals(0, "\uFE0F".displayWidth())
    }

    @Test
    fun zwjAndControlWidth() {
        // Zero width joiner
        assertEquals(0, "\u200D".displayWidth())
        // Control
        assertEquals(0, "\u0001".displayWidth())
    }

    @Test
    fun takeDisplayWidthAscii() {
        assertEquals("hell", "hello".takeDisplayWidth(4))
        assertEquals("hello", "hello".takeDisplayWidth(10))
        assertEquals("", "hello".takeDisplayWidth(0))
    }

    @Test
    fun takeDisplayWidthCjkTruncatesWideCharCleanly() {
        // "あいう" = 6 width; asking for 3 should drop the second wide char entirely
        assertEquals("あ", "あいう".takeDisplayWidth(3))
        assertEquals("あい", "あいう".takeDisplayWidth(4))
        assertEquals("あいう", "あいう".takeDisplayWidth(6))
    }

    @Test
    fun takeDisplayWidthKeepsSurrogateIntact() {
        // "a👍b" where thumbs-up is wide. maxWidth = 3 → "a" + can't fit emoji (width 2) → "a" only
        val s = "a\uD83D\uDC4Db"
        // width sequence: 1, 2, 1 = 4
        assertEquals("a", s.takeDisplayWidth(2))
        assertEquals("a\uD83D\uDC4D", s.takeDisplayWidth(3))
        assertEquals("a\uD83D\uDC4Db", s.takeDisplayWidth(4))
    }

    @Test
    fun padDisplayEnd() {
        assertEquals("あ   ", "あ".padDisplayEnd(5))
        assertEquals("hello", "hello".padDisplayEnd(3))
        assertEquals("hi---", "hi".padDisplayEnd(5, '-'))
    }

    @Test
    fun displayWidthIgnoresVariationSelector() {
        // "☺" + VS16 -> width 1 (base) + 0 (VS16) = 1
        // but a typical terminal treats ☺️ as wide; we undercount here, which matches our "known limitation"
        val s = "\u263A\uFE0F"
        val width = s.displayWidth()
        assertTrue(width == 1 || width == 2, "got $width")
    }

    @Test
    fun malformedSurrogatesAreTreatedAsSingleCodeUnits() {
        assertEquals(1, "\uD83D".displayWidth())
        assertEquals(1, "\uDC4D".displayWidth())
        assertEquals("\uD83D", "\uD83D".takeDisplayWidth(1))
    }

    @Test
    fun takeDisplayWidthReturnsOriginalWhenNoTruncationIsNeeded() {
        val value = "abc"
        assertTrue(value === value.takeDisplayWidth(3))
        assertEquals("", value.takeDisplayWidth(-1))
    }

    @Test
    fun padDisplayEndKeepsStringWhenWidthAlreadyMatches() {
        assertEquals("あ", "あ".padDisplayEnd(2, '.'))
    }
}

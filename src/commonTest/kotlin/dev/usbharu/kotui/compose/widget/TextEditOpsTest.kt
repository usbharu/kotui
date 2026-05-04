package dev.usbharu.kotui.compose.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextEditOpsTest {

    @Test
    fun nextAndPrevCodePointAscii() {
        val s = "hello"
        assertEquals(1, TextEditOps.nextCodePoint(s, 0))
        assertEquals(2, TextEditOps.nextCodePoint(s, 1))
        assertEquals(5, TextEditOps.nextCodePoint(s, 5)) // past end clamps
        assertEquals(4, TextEditOps.prevCodePoint(s, 5))
        assertEquals(0, TextEditOps.prevCodePoint(s, 0))
    }

    @Test
    fun surrogateMovesBoth() {
        val s = "a\uD83D\uDE00b" // a + 😀 + b
        // 'a' at 0, emoji at 1..2, 'b' at 3
        assertEquals(1, TextEditOps.nextCodePoint(s, 0))
        assertEquals(3, TextEditOps.nextCodePoint(s, 1))
        assertEquals(4, TextEditOps.nextCodePoint(s, 3))
        assertEquals(3, TextEditOps.prevCodePoint(s, 4))
        assertEquals(1, TextEditOps.prevCodePoint(s, 3))
    }

    @Test
    fun nextWordSkipsPunctuationAndWord() {
        val s = "foo bar"
        // cursor at 0 → end of "foo" = 3
        assertEquals(3, TextEditOps.nextWordBoundary(s, 0))
        // cursor at 3 (between foo and space) → skip space, then skip "bar" = 7
        assertEquals(7, TextEditOps.nextWordBoundary(s, 3))
    }

    @Test
    fun prevWordBoundary() {
        val s = "foo bar"
        // cursor at 7 → skip back over "bar" = 4
        assertEquals(4, TextEditOps.prevWordBoundary(s, 7))
        // cursor at 4 → skip space and "foo" = 0
        assertEquals(0, TextEditOps.prevWordBoundary(s, 4))
    }

    @Test
    fun replaceAtCursor() {
        val (v, c) = TextEditOps.replace("hello", 5, null, " world")
        assertEquals("hello world", v)
        assertEquals(11, c)
    }

    @Test
    fun replaceSelection() {
        // select "ell" (1..4) and replace with "I"
        val (v, c) = TextEditOps.replace("hello", 4, 1..4, "I")
        assertEquals("hIo", v)
        assertEquals(2, c)
    }

    @Test
    fun replaceClampsInvalidBounds() {
        val (cursorValue, cursorPos) = TextEditOps.replace("hello", 99, null, "!")
        assertEquals("hello!", cursorValue)
        assertEquals(6, cursorPos)

        val (selectionValue, selectionPos) = TextEditOps.replace("hello", 0, 1..99, "!")
        assertEquals("h!", selectionValue)
        assertEquals(2, selectionPos)
    }

    @Test
    fun deleteRange() {
        val (v, c) = TextEditOps.deleteRange("hello", 1, 4)
        assertEquals("ho", v)
        assertEquals(1, c)
    }

    @Test
    fun replaceIfValidAcceptsValidCandidate() {
        val replacement = TextEditOps.replaceIfValid(
            value = "12",
            cursor = 2,
            selection = null,
            insert = "3",
            inputValidator = TextInputValidator.AsciiDigitsOnly,
        )

        assertEquals("123" to 3, replacement)
    }

    @Test
    fun replaceIfValidRejectsWholeInsert() {
        val replacement = TextEditOps.replaceIfValid(
            value = "12",
            cursor = 2,
            selection = null,
            insert = "a3",
            inputValidator = TextInputValidator.AsciiDigitsOnly,
        )

        assertNull(replacement)
    }

    @Test
    fun deleteRangeIsNotBlockedByValidator() {
        val (v, c) = TextEditOps.deleteRange("12a3", 2, 3)
        assertEquals("123", v)
        assertEquals(2, c)
    }

    @Test
    fun clampBoundaryAvoidsMidSurrogate() {
        val s = "a\uD83D\uDE00b"
        // index 2 lands in the middle of the surrogate pair → snap to 1
        assertEquals(1, TextEditOps.clampToBoundary(s, 2))
        assertEquals(3, TextEditOps.clampToBoundary(s, 3))
        assertEquals(0, TextEditOps.clampToBoundary(s, 0))
    }
}

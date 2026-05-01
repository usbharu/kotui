package dev.usbharu.kotui.compose.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class TextAreaOpsTest {

    @Test
    fun lineStartAndEndSingleLine() {
        val s = "hello"
        assertEquals(0, TextAreaOps.lineStart(s, 0))
        assertEquals(0, TextAreaOps.lineStart(s, 3))
        assertEquals(0, TextAreaOps.lineStart(s, 5))
        assertEquals(5, TextAreaOps.lineEnd(s, 0))
        assertEquals(5, TextAreaOps.lineEnd(s, 3))
        assertEquals(5, TextAreaOps.lineEnd(s, 5))
    }

    @Test
    fun lineStartAndEndMultiLine() {
        //            0 1 2 3 4 5 6 7 8
        //            a b \n c d \n e f
        val s = "ab\ncd\nef"
        assertEquals(0, TextAreaOps.lineStart(s, 0))
        assertEquals(0, TextAreaOps.lineStart(s, 2))
        assertEquals(3, TextAreaOps.lineStart(s, 3))
        assertEquals(3, TextAreaOps.lineStart(s, 5))
        assertEquals(6, TextAreaOps.lineStart(s, 6))
        assertEquals(6, TextAreaOps.lineStart(s, 8))

        assertEquals(2, TextAreaOps.lineEnd(s, 0))
        assertEquals(2, TextAreaOps.lineEnd(s, 2))
        assertEquals(5, TextAreaOps.lineEnd(s, 3))
        assertEquals(5, TextAreaOps.lineEnd(s, 5))
        assertEquals(8, TextAreaOps.lineEnd(s, 6))
        assertEquals(8, TextAreaOps.lineEnd(s, 8))
    }

    @Test
    fun cursorToRowColBasic() {
        val s = "ab\ncd\nef"
        assertEquals(0 to 0, TextAreaOps.cursorToRowCol(s, 0))
        assertEquals(0 to 1, TextAreaOps.cursorToRowCol(s, 1))
        assertEquals(0 to 2, TextAreaOps.cursorToRowCol(s, 2))
        // At cursor=3 (right after '\n'), we're at start of row 1.
        assertEquals(1 to 0, TextAreaOps.cursorToRowCol(s, 3))
        assertEquals(1 to 2, TextAreaOps.cursorToRowCol(s, 5))
        assertEquals(2 to 0, TextAreaOps.cursorToRowCol(s, 6))
        assertEquals(2 to 2, TextAreaOps.cursorToRowCol(s, 8))
    }

    @Test
    fun rowColToCursorRoundTrip() {
        val s = "ab\ncd\nef"
        for (pos in 0..s.length) {
            val (row, col) = TextAreaOps.cursorToRowCol(s, pos)
            assertEquals(pos, TextAreaOps.rowColToCursor(s, row, col))
        }
    }

    @Test
    fun rowColToCursorClampsToLineEnd() {
        val s = "ab\nx\nefghi"
        // row 1 has only "x" — column 5 should clamp to line end (cursor=4).
        assertEquals(4, TextAreaOps.rowColToCursor(s, 1, 5))
        // row 0 has "ab" — column 99 should clamp to cursor=2.
        assertEquals(2, TextAreaOps.rowColToCursor(s, 0, 99))
    }

    @Test
    fun rowColToCursorNegativeRowClampsToZero() {
        val s = "ab\ncd"
        assertEquals(0, TextAreaOps.rowColToCursor(s, -1, 0))
    }

    @Test
    fun rowColToCursorBeyondLastRowClampsToEnd() {
        val s = "ab\ncd"
        assertEquals(5, TextAreaOps.rowColToCursor(s, 99, 0))
    }

    @Test
    fun cursorMathOnTrailingNewline() {
        // Trailing '\n' means there's a phantom empty line after it.
        val s = "ab\n"
        assertEquals(0 to 0, TextAreaOps.cursorToRowCol(s, 0))
        assertEquals(0 to 2, TextAreaOps.cursorToRowCol(s, 2))
        assertEquals(1 to 0, TextAreaOps.cursorToRowCol(s, 3))
        assertEquals(3, TextAreaOps.rowColToCursor(s, 1, 0))
    }

    @Test
    fun wideColumnRespectsDisplayWidth() {
        //   cursor:  0  1  2   3(=末)
        //   chars:  あ  い  う
        // Each of あいう has display width 2.
        val s = "あいう"
        assertEquals(0 to 0, TextAreaOps.cursorToRowCol(s, 0))
        assertEquals(0 to 2, TextAreaOps.cursorToRowCol(s, 1))
        assertEquals(0 to 4, TextAreaOps.cursorToRowCol(s, 2))
        assertEquals(0 to 6, TextAreaOps.cursorToRowCol(s, 3))
        // col=3 lands between あ(2) and い(2→4). No cell fits — we stop at あ.
        assertEquals(1, TextAreaOps.rowColToCursor(s, 0, 3))
        assertEquals(2, TextAreaOps.rowColToCursor(s, 0, 4))
    }
}

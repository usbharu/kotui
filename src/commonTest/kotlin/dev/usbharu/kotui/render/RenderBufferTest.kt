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

    @Test
    fun outOfBoundsWritesAreIgnoredAndReadsReturnBlankCell() {
        val buf = RenderBuffer(2, 1)
        buf.set(-1, 0, 'X', Style(), 0)
        buf.set(0, -1, 'Y', Style(), 0)
        buf.set(3, 0, 'Z', Style(), 0)
        buf.setGrapheme(0, 0, "", 0, Style(), 0)

        assertEquals(" ", buf.get(0, 0).content)
        assertEquals(" ", buf.get(-1, 0).content)
        assertEquals(" ", buf.get(0, 99).content)
    }

    @Test
    fun lowerZIndexCannotOverwriteHigherZIndexCell() {
        val buf = RenderBuffer(3, 1)
        buf.set(0, 0, 'A', Style(), 5)
        buf.set(0, 0, 'b', Style(), 4)

        assertEquals("A", buf.get(0, 0).content)
        assertEquals(5, buf.get(0, 0).zIndex)
    }

    @Test
    fun wideWriteRefusesToCrossHigherZIndexContinuationCell() {
        val buf = RenderBuffer(3, 1)
        buf.set(1, 0, 'X', Style(), 10)
        buf.writeString(0, 0, "あ", Style(), 0)

        assertEquals(" ", buf.get(0, 0).content)
        assertEquals("X", buf.get(1, 0).content)
    }

    @Test
    fun overwritingLeftHalfOfWideCharLeavesRightContinuationUntouched() {
        val buf = RenderBuffer(4, 1)
        buf.writeString(1, 0, "あ", Style(), 0)

        buf.set(1, 0, 'A', Style(), 1)

        assertEquals("A", buf.get(1, 0).content)
        assertTrue(buf.get(2, 0).isContinuation)
    }

    @Test
    fun resizeNoOpPreservesCellsAndResizeClearsCellsAndPlacements() {
        val buf = RenderBuffer(3, 1)
        buf.writeString(0, 0, "abc", Style(), 0)
        buf.resize(3, 1)
        assertEquals("a", buf.get(0, 0).content)

        buf.placeImage(0, 0, dev.usbharu.kotui.compose.widget.TerminalImage(ByteArray(4), 1, 1), 0)
        assertTrue(buf.imagePlacements().isNotEmpty())

        buf.resize(2, 2)
        assertEquals(2, buf.width)
        assertEquals(2, buf.height)
        assertEquals(" ", buf.get(0, 0).content)
        assertTrue(buf.imagePlacements().isEmpty())
    }

    @Test
    fun writeStringSkipsRowsOutsideBuffer() {
        val buf = RenderBuffer(3, 1)
        buf.writeString(0, 2, "abc", Style(), 0)

        assertEquals(" ", buf.get(0, 0).content)
    }

    @Test
    fun setGraphemeRejectsInvalidCoordinatesAndWidths() {
        val buf = RenderBuffer(2, 1)
        buf.setGrapheme(0, -1, "X", 1, Style(), 0)
        buf.setGrapheme(0, 0, "X", 0, Style(), 0)
        buf.setGrapheme(-1, 0, "X", 1, Style(), 0)
        buf.setGrapheme(2, 0, "X", 1, Style(), 0)

        assertEquals(" ", buf.get(0, 0).content)
        assertEquals(" ", buf.get(1, 0).content)
    }

    @Test
    fun wideWriteClearsFarContinuationWhenOverlappingWideCellToTheRight() {
        val buf = RenderBuffer(4, 1)
        buf.writeString(1, 0, "あ", Style(), 0)

        buf.writeString(0, 0, "い", Style(), 1)

        assertEquals("い", buf.get(0, 0).content)
        assertTrue(buf.get(1, 0).isContinuation)
        assertEquals(" ", buf.get(2, 0).content)
    }

    @Test
    fun lowerZIndexImagePlacementCannotClearHigherCellsAndClipsBounds() {
        val buf = RenderBuffer(3, 2)
        buf.set(1, 0, 'X', Style(), 10)
        val image = dev.usbharu.kotui.compose.widget.TerminalImage(
            rgba = ByteArray(16),
            pixelWidth = 2,
            pixelHeight = 2,
            cellPixelWidth = 1,
            cellPixelHeight = 1,
        )

        buf.placeImage(-1, -1, image, zIndex = 0)

        assertEquals("X", buf.get(1, 0).content)
        assertEquals(" ", buf.get(0, 0).content)
        assertEquals(ImagePlacement(-1, -1, 2, 2, image, 0), buf.imagePlacements().single())
    }

    @Test
    fun supplementaryCodePointIsWrittenAsSurrogatePair() {
        val buf = RenderBuffer(3, 1)
        val musical = "\uD834\uDD1E"

        buf.writeString(0, 0, musical, Style(), 0)

        assertEquals(musical, buf.get(0, 0).content)
    }
}

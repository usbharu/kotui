package dev.usbharu.kotui.render

import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Kitty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ImagePlacementTest {
    private fun solidImage(pixelW: Int, pixelH: Int, cellPxW: Int = 10, cellPxH: Int = 20): TerminalImage {
        val rgba = ByteArray(pixelW * pixelH * 4) { i ->
            when (i % 4) {
                0 -> 0xFF.toByte()
                3 -> 0xFF.toByte()
                else -> 0
            }
        }
        return TerminalImage(rgba, pixelW, pixelH, cellPixelWidth = cellPxW, cellPixelHeight = cellPxH)
    }

    @Test
    fun placementReservesRectangleOfCells() {
        val buf = RenderBuffer(10, 5)
        val img = solidImage(pixelW = 20, pixelH = 40)  // => 2 × 2 cells at default 10×20
        buf.placeImage(1, 1, img, 0)

        for (y in 1..2) {
            for (x in 1..2) {
                assertEquals(" ", buf.get(x, y).content, "cell ($x,$y) should be blanked")
            }
        }
        assertEquals(" ", buf.get(0, 0).content, "untouched cell preserved")
    }

    @Test
    fun placementsAreExposedForRenderer() {
        val buf = RenderBuffer(10, 5)
        val img = solidImage(pixelW = 30, pixelH = 20)
        buf.placeImage(2, 1, img, 3)

        val placements = buf.imagePlacements()
        assertEquals(1, placements.size)
        val p = placements[0]
        assertEquals(2, p.x)
        assertEquals(1, p.y)
        assertEquals(3, p.cellWidth)
        assertEquals(1, p.cellHeight)
        assertEquals(3, p.zIndex)
    }

    @Test
    fun clearAlsoDropsPlacements() {
        val buf = RenderBuffer(10, 5)
        buf.placeImage(0, 0, solidImage(10, 20), 0)
        assertEquals(1, buf.imagePlacements().size)
        buf.clear()
        assertEquals(0, buf.imagePlacements().size)
    }

    @Test
    fun higherZIndexPlacementEvictsLowerContent() {
        val buf = RenderBuffer(5, 2)
        buf.writeString(0, 0, "hello", Style(), 0)
        assertEquals("h", buf.get(0, 0).content)

        buf.placeImage(0, 0, solidImage(30, 20), 5)
        // 3 cells overwritten with spaces
        assertEquals(" ", buf.get(0, 0).content)
        assertEquals(" ", buf.get(1, 0).content)
        assertEquals(" ", buf.get(2, 0).content)
        // cell 3 untouched by image (only 3 cells wide)
        assertEquals("l", buf.get(3, 0).content)
    }

    @Test
    fun lowerZIndexPlacementCannotOverwriteHigherContent() {
        val buf = RenderBuffer(5, 2)
        buf.writeString(0, 0, "X", Style(), 10)
        buf.placeImage(0, 0, solidImage(20, 20), 1)
        // Cell (0,0) was at zIndex 10; the low-z image must not overwrite it.
        assertEquals("X", buf.get(0, 0).content)
        assertTrue(buf.imagePlacements().isEmpty(), "blocked image must not be emitted over higher content")
    }

    @Test
    fun fullyOffscreenImageIsNotRecorded() {
        val buf = RenderBuffer(2, 2)

        buf.placeImage(5, 5, solidImage(10, 20), 1)

        assertTrue(buf.imagePlacements().isEmpty())
    }

    @Test
    fun partiallyOffscreenProtocolImageIsRejectedInsteadOfRecordedButNeverFlushed() {
        val buf = RenderBuffer(2, 2)

        val placed = buf.placeImage(-1, 0, solidImage(20, 20), 1)

        assertEquals(false, placed)
        assertTrue(buf.imagePlacements().isEmpty())
    }

    @Test
    fun imageExtendingPastViewportIsRejected() {
        val buf = RenderBuffer(2, 2)

        val placed = buf.placeImage(1, 1, solidImage(20, 40), 1)

        assertEquals(false, placed)
        assertTrue(buf.imagePlacements().isEmpty())
    }

    @Test
    fun terminalImageRejectsOverflowingPixelBufferSize() {
        assertFailsWith<IllegalArgumentException> {
            TerminalImage(ByteArray(4), Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    @Test
    fun placementListIsADefensiveSnapshot() {
        val buf = RenderBuffer(2, 2)
        buf.placeImage(0, 0, solidImage(10, 20), 0)
        val snapshot = buf.imagePlacements()
        buf.clear()

        assertEquals(1, snapshot.size)
        assertTrue(buf.imagePlacements().isEmpty())
    }

    @Test
    fun terminalImageSnapshotsCallerOwnedPixelBytes() {
        val rgba = byteArrayOf(10, 20, 30, 40)
        val expected = Kitty.encode(rgba.copyOf(), 1, 1)
        val image = TerminalImage(rgba, 1, 1)

        rgba.fill(0)

        assertEquals(expected, image.kitty)
    }

    @Test
    fun terminalImageRejectsInvalidPaletteLimitEagerly() {
        assertFailsWith<IllegalArgumentException> { TerminalImage(ByteArray(4), 1, 1, maxColors = 0) }
        assertFailsWith<IllegalArgumentException> { TerminalImage(ByteArray(4), 1, 1, maxColors = 257) }
    }
}

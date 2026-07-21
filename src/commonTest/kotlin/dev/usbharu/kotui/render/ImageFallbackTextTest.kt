package dev.usbharu.kotui.render

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.render.TuiRenderer
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.SixelSupport
import dev.usbharu.kotui.utils.TerminalCaps
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageFallbackTextTest {
    private fun solidImage(pixelW: Int, pixelH: Int, fallback: String?): TerminalImage {
        val rgba = ByteArray(pixelW * pixelH * 4)
        return TerminalImage(
            rgba = rgba,
            pixelWidth = pixelW,
            pixelHeight = pixelH,
            cellPixelWidth = 10,
            cellPixelHeight = 20,
            fallbackText = fallback,
        )
    }

    @AfterTest
    fun resetCaps() {
        SixelSupport.overrideForTesting(null)
    }

    /** Directly exercises the RenderBuffer branch that TuiRenderer goes through. */
    @Test
    fun fallback_text_overwrites_reserved_image_cells_on_first_row() {
        val buf = RenderBuffer(20, 5)
        val img = solidImage(pixelW = 100, pixelH = 40, fallback = null) // 10 × 2 cells
        buf.placeImage(0, 0, img, 0)

        val text = "NO SIXEL"
        buf.writeString(0, 0, text, Style(), 0)

        for ((i, expected) in text.withIndex()) {
            assertEquals(expected.toString(), buf.get(i, 0).content, "col $i")
        }
        for (i in text.length until 10) {
            assertEquals(" ", buf.get(i, 0).content, "blank col $i")
        }
        for (i in 0 until 10) {
            assertEquals(" ", buf.get(i, 1).content, "row 1 col $i stays blank")
        }
    }

    /** End-to-end through TuiRenderer: with UNSUPPORTED caps, fallback text lands in the buffer. */
    @Test
    fun renderer_writes_fallback_text_when_neither_sixel_nor_kitty_is_supported() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val renderer = TuiRenderer(20, 5)
        val root = TuiNode("Root").apply {
            layoutPolicy = LayoutPolicy.BOX
            bounds = Rect(0, 0, 20, 5)
        }
        val imageNode = TuiNode("Image").apply {
            layoutPolicy = LayoutPolicy.LEAF
            image = solidImage(pixelW = 100, pixelH = 40, fallback = "FALLBACK")
            bounds = Rect(0, 0, 10, 2)
        }
        root.insertAt(0, imageNode)

        renderer.renderToBuffer(root, FocusManager())

        val row0 = (0 until 8).joinToString("") { renderer.buffer.get(it, 0).content }
        assertEquals("FALLBACK", row0)
    }

    /** Sanity: with sixel support, the renderer leaves the image area blank for the
     *  graphics layer and does NOT stamp fallback text into cells. */
    @Test
    fun renderer_skips_fallback_text_when_sixel_is_supported() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = true))
        val renderer = TuiRenderer(20, 5)
        val root = TuiNode("Root").apply {
            layoutPolicy = LayoutPolicy.BOX
            bounds = Rect(0, 0, 20, 5)
        }
        val imageNode = TuiNode("Image").apply {
            layoutPolicy = LayoutPolicy.LEAF
            image = solidImage(pixelW = 100, pixelH = 40, fallback = "FALLBACK")
            bounds = Rect(0, 0, 10, 2)
        }
        root.insertAt(0, imageNode)

        renderer.renderToBuffer(root, FocusManager())

        for (i in 0 until 8) {
            assertEquals(" ", renderer.buffer.get(i, 0).content, "col $i should be blank")
        }
    }
}

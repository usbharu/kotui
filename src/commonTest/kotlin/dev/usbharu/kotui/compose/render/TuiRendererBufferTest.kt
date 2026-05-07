package dev.usbharu.kotui.compose.render

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.SixelSupport
import dev.usbharu.kotui.utils.TerminalCaps
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TuiRendererBufferTest {
    @AfterTest
    fun resetCaps() {
        SixelSupport.overrideForTesting(null)
    }

    private fun root(vararg children: TuiNode): TuiNode =
        TuiNode("Root").apply {
            layoutPolicy = LayoutPolicy.BOX
            bounds = Rect(0, 0, 12, 4)
            children.forEachIndexed { index, child -> insertAt(index, child) }
        }

    private fun image(fallback: String? = null): TerminalImage =
        TerminalImage(
            rgba = ByteArray(2 * 2 * 4) { 0x7F },
            pixelWidth = 2,
            pixelHeight = 2,
            fallbackText = fallback,
        )

    @Test
    fun focusedNodeUsesFocusedStyleWhenRenderingText() {
        val focusManager = FocusManager()
        val node = TuiNode("Input").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "ok"
            focusable = true
            focusId = focusManager.allocateFocusId()
            style = Style(fg = Ansi.FG_BLUE)
            focusedStyle = Style(fg = Ansi.FG_RED, bold = true)
        }
        focusManager.requestFocus(node.focusId)
        val renderer = TuiRenderer(12, 4)

        renderer.renderToBuffer(root(node), focusManager)

        assertEquals("o", renderer.buffer.get(0, 0).content)
        assertEquals(Ansi.FG_RED, renderer.buffer.get(0, 0).style.fg)
        assertTrue(renderer.buffer.get(0, 0).style.bold)
    }

    @Test
    fun unfocusedNodeFallsBackToBaseStyleWhenFocusedStyleExists() {
        val node = TuiNode("Input").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "ok"
            focusable = true
            focusId = 10
            style = Style(fg = Ansi.FG_BLUE)
            focusedStyle = Style(fg = Ansi.FG_RED)
        }
        val renderer = TuiRenderer(12, 4)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals(Ansi.FG_BLUE, renderer.buffer.get(0, 0).style.fg)
    }

    @Test
    fun borderTitleIsClippedInsideSmallBorder() {
        val panel = TuiNode("Panel").apply {
            bounds = Rect(0, 0, 6, 3)
            drawBorder = true
            borderTitle = "abcdef"
        }
        val renderer = TuiRenderer(8, 4)

        renderer.renderToBuffer(root(panel), FocusManager())

        assertEquals("+", renderer.buffer.get(0, 0).content)
        assertEquals("+", renderer.buffer.get(5, 0).content)
        assertEquals(" ", renderer.buffer.get(2, 0).content)
        assertEquals("a", renderer.buffer.get(3, 0).content)
        assertEquals("|", renderer.buffer.get(0, 1).content)
        assertEquals("+", renderer.buffer.get(5, 2).content)
    }

    @Test
    fun oneCellBorderAvoidsRightAndBottomEdges() {
        val panel = TuiNode("Panel").apply {
            bounds = Rect(0, 0, 1, 1)
            drawBorder = true
        }
        val renderer = TuiRenderer(3, 2)

        renderer.renderToBuffer(root(panel), FocusManager())

        assertEquals("+", renderer.buffer.get(0, 0).content)
        assertEquals(" ", renderer.buffer.get(1, 0).content)
    }

    @Test
    fun highlightMergesWithBaseAndExistingCellStyle() {
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "abcd"
            style = Style(fg = Ansi.FG_BLUE)
            textHighlights = listOf(TextHighlight(1, 3, Style(bg = Ansi.BG_RED, bold = true)))
        }
        val renderer = TuiRenderer(8, 2)

        renderer.renderToBuffer(root(node), FocusManager())

        val highlighted = renderer.buffer.get(1, 0)
        assertEquals("b", highlighted.content)
        assertEquals(Ansi.FG_BLUE, highlighted.style.fg)
        assertEquals(Ansi.BG_RED, highlighted.style.bg)
        assertTrue(highlighted.style.bold)
        assertEquals("d", renderer.buffer.get(3, 0).content)
        assertFalse(renderer.buffer.get(3, 0).style.bold)
    }

    @Test
    fun highlightSkipsWideCharacterContinuationCells() {
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "あb"
            textHighlights = listOf(TextHighlight(0, 2, Style(underline = true)))
        }
        val renderer = TuiRenderer(8, 2)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals("あ", renderer.buffer.get(0, 0).content)
        assertTrue(renderer.buffer.get(0, 0).style.underline)
        assertTrue(renderer.buffer.get(1, 0).isContinuation)
    }

    @Test
    fun imageFallbackIsSkippedWhenKittyIsSupported() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        val node = TuiNode("Image").apply {
            bounds = Rect(0, 0, 4, 2)
            image = image(fallback = "ALT")
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 0).content)
        assertEquals(1, renderer.buffer.imagePlacements().size)
    }

    @Test
    fun imageFallbackRendersWhenNoImageProtocolIsSupported() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val node = TuiNode("Image").apply {
            bounds = Rect(0, 0, 2, 1)
            image = image(fallback = "ALT")
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals("A", renderer.buffer.get(0, 0).content)
        assertEquals(" ", renderer.buffer.get(1, 0).content)
        assertEquals(1, renderer.buffer.imagePlacements().size)
    }

    @Test
    fun imageFallbackIsSkippedWhenSixelIsSupported() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = true, kittySupported = false))
        val node = TuiNode("Image").apply {
            bounds = Rect(0, 0, 4, 2)
            image = image(fallback = "ALT")
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 0).content)
        assertEquals(1, renderer.buffer.imagePlacements().size)
    }

    @Test
    fun parentZIndexContributesToChildren() {
        val child = TuiNode("Text").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "low"
            zIndex = 1
        }
        val parent = TuiNode("Box").apply {
            bounds = Rect(0, 0, 4, 1)
            zIndex = 5
            insertAt(0, child)
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(parent), FocusManager())

        assertEquals("l", renderer.buffer.get(0, 0).content)
        assertEquals(6, renderer.buffer.get(0, 0).zIndex)
    }

    @Test
    fun highlightClampsNegativeStartAndEmptyCellsUseBlankContent() {
        val node = TuiNode("Text").apply {
            bounds = Rect(1, 0, 4, 1)
            text = "a"
            textHighlights = listOf(TextHighlight(-2, 3, Style(reverse = true)))
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals("a", renderer.buffer.get(1, 0).content)
        assertTrue(renderer.buffer.get(1, 0).style.reverse)
        assertEquals(" ", renderer.buffer.get(2, 0).content)
        assertTrue(renderer.buffer.get(2, 0).style.reverse)
    }

    @Test
    fun fillCharRendersOnlyInsideNodeBounds() {
        val node = TuiNode("Fill").apply {
            bounds = Rect(1, 1, 2, 2)
            fillChar = '#'
        }
        val renderer = TuiRenderer(5, 4)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 1).content)
        assertEquals("#", renderer.buffer.get(1, 1).content)
        assertEquals("#", renderer.buffer.get(2, 2).content)
        assertEquals(" ", renderer.buffer.get(3, 2).content)
    }

    @Test
    fun resizeNoOpKeepsDimensionsAndResizeReallocatesBuffer() {
        val renderer = TuiRenderer(4, 2)

        renderer.resize(4, 2)
        assertEquals(4, renderer.screenWidth)
        assertEquals(2, renderer.screenHeight)

        renderer.resize(5, 3)
        assertEquals(5, renderer.screenWidth)
        assertEquals(3, renderer.screenHeight)
        assertEquals(5, renderer.buffer.width)
        assertEquals(3, renderer.buffer.height)
    }
}

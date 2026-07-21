package dev.usbharu.kotui.compose.render

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.SixelSupport
import dev.usbharu.kotui.utils.TerminalCaps
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import dev.usbharu.kotui.utils.Ansi

class TuiRendererBufferTest {
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
    fun styleColorFieldsCannotInjectNonSgrTerminalCommands() {
        assertTrue(isSafeSgrSequence("\u001B[38;2;1;2;3m"))
        assertFalse(isSafeSgrSequence("\u001B[2J"))
        assertFalse(isSafeSgrSequence("plain text"))
        assertFalse(isSafeSgrSequence("\u001B]52;c;payload\u0007"))
    }

    @AfterTest
    fun clearTerminalCaps() {
        SixelSupport.overrideForTesting(null)
    }

    @Test
    fun renderToBufferDrawsBorderFillTextAndHighlights() {
        val root = TuiNode("root").apply {
            layoutPolicy = LayoutPolicy.BOX
            bounds = Rect(0, 0, 10, 5)
        }
        val panel = TuiNode("panel").apply {
            bounds = Rect(0, 0, 10, 4)
            drawBorder = true
            borderTitle = "T"
            fillChar = '.'
            style = Style(fg = "base")
        }
        val text = TuiNode("text").apply {
            bounds = Rect(2, 2, 5, 1)
            this.text = "hello"
            style = Style(fg = "text")
            textHighlights = listOf(TextHighlight(1, 4, Style(bg = "hl", bold = true)))
        }
        root.insertAt(0, panel)
        root.insertAt(1, text)
        root.insertAt(2, TuiNode("fill").apply {
            bounds = Rect(0, 4, 3, 1)
            fillChar = '#'
        })

        val renderer = TuiRenderer(10, 5)
        renderer.renderToBuffer(root, FocusManager())

        assertEquals("+", renderer.buffer.get(0, 0).content)
        assertEquals("-", renderer.buffer.get(1, 0).content)
        assertEquals(" ", renderer.buffer.get(2, 0).content)
        assertEquals("T", renderer.buffer.get(3, 0).content)
        assertEquals(" ", renderer.buffer.get(8, 2).content)
        assertEquals("#", renderer.buffer.get(1, 4).content)
        assertEquals("h", renderer.buffer.get(2, 2).content)
        assertEquals("e", renderer.buffer.get(3, 2).content)
        assertEquals("hl", renderer.buffer.get(3, 2).style.bg)
        assertTrue(renderer.buffer.get(3, 2).style.bold)
        assertFalse(renderer.buffer.get(2, 2).style.bold)
    }

    @Test
    fun focusedNodeUsesFocusedStyle() {
        val root = TuiNode("root")
        val focused = TuiNode("focused").apply {
            bounds = Rect(0, 0, 2, 1)
            text = "ok"
            focusable = true
            focusId = 12
            style = Style(fg = "normal")
            focusedStyle = Style(fg = "focused", underline = true)
        }
        root.insertAt(0, focused)
        val focusManager = FocusManager()
        focusManager.requestFocus(12)

        val renderer = TuiRenderer(4, 1)
        renderer.renderToBuffer(root, focusManager)

        assertEquals("focused", renderer.buffer.get(0, 0).style.fg)
        assertTrue(renderer.buffer.get(0, 0).style.underline)
    }

    @Test
    fun imageFallbackTextIsRenderedWhenGraphicsAreUnsupported() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val image = TerminalImage(
            rgba = ByteArray(4) { 0x7f },
            pixelWidth = 1,
            pixelHeight = 1,
            fallbackText = "img",
        )
        val root = TuiNode("root")
        val node = TuiNode("image").apply {
            bounds = Rect(1, 0, 3, 1)
            this.image = image
            style = Style(fg = "image")
        }
        root.insertAt(0, node)

        val renderer = TuiRenderer(5, 2)
        renderer.renderToBuffer(root, FocusManager())

        assertEquals("i", renderer.buffer.get(1, 0).content)
        assertEquals("image", renderer.buffer.get(1, 0).style.fg)
        assertEquals(1, renderer.buffer.imagePlacements().size)
        assertEquals(1, renderer.buffer.imagePlacements().single().x)
    }

    @Test
    fun zeroSizedBorderDrawsNothing() {
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("empty-border").apply {
            bounds = Rect(0, 0, 0, 0)
            drawBorder = true
        })
        val renderer = TuiRenderer(2, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 0).content)
    }

    @Test
    fun highlightTouchingWideContinuationStylesWholeGlyph() {
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("wide").apply {
            bounds = Rect(0, 0, 2, 1)
            text = "界"
            textHighlights = listOf(TextHighlight(1, 2, Style(bg = "selected")))
        })
        val renderer = TuiRenderer(2, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals("selected", renderer.buffer.get(0, 0).style.bg)
        assertTrue(renderer.buffer.get(1, 0).isContinuation)
    }

    @Test
    fun fallbackTextIsClippedToNodeWidth() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("image").apply {
            bounds = Rect(0, 0, 1, 1)
            image = TerminalImage(ByteArray(8) { 1 }, 2, 1, cellPixelWidth = 1, fallbackText = "ab")
        })
        val renderer = TuiRenderer(3, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals("a", renderer.buffer.get(0, 0).content)
        assertEquals(" ", renderer.buffer.get(1, 0).content)
    }

    @Test
    fun effectiveZIndexSaturatesInsteadOfWrapping() {
        val root = TuiNode("root").apply { zIndex = Int.MAX_VALUE }
        root.insertAt(0, TuiNode("child").apply {
            bounds = Rect(0, 0, 1, 1)
            text = "x"
            zIndex = 1
        })
        val renderer = TuiRenderer(1, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals(Int.MAX_VALUE, renderer.buffer.get(0, 0).zIndex)
    }

    @Test
    fun zIndexOverflowKeepsPrecisionForDeeperDescendants() {
        val root = TuiNode("root").apply { zIndex = Int.MAX_VALUE }
        val overflow = TuiNode("overflow").apply { zIndex = 10 }
        val recover = TuiNode("recover").apply {
            zIndex = -10
            text = "x"
            bounds = Rect(0, 0, 1, 1)
        }
        root.insertAt(0, overflow)
        overflow.insertAt(0, recover)

        val renderer = TuiRenderer(1, 1)
        renderer.renderToBuffer(root, FocusManager())

        assertEquals(Int.MAX_VALUE, renderer.buffer.get(0, 0).zIndex)
    }

    @Test
    fun failedResizeLeavesRendererDimensionsUnchanged() {
        val renderer = TuiRenderer(3, 2)

        assertFailsWith<IllegalArgumentException> { renderer.resize(-1, 2) }

        assertEquals(3, renderer.screenWidth)
        assertEquals(2, renderer.screenHeight)
    }

    @Test
    fun wideFillCharacterIsRepeatedWithoutDanglingCells() {
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("fill").apply {
            bounds = Rect(0, 0, 5, 1)
            fillChar = '界'
        })
        val renderer = TuiRenderer(5, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals("界", renderer.buffer.get(0, 0).content)
        assertTrue(renderer.buffer.get(1, 0).isContinuation)
        assertEquals("界", renderer.buffer.get(2, 0).content)
        assertTrue(renderer.buffer.get(3, 0).isContinuation)
        assertEquals(" ", renderer.buffer.get(4, 0).content)
    }

    @Test
    fun zeroWidthFillCharacterDoesNotCorruptBuffer() {
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("fill").apply {
            bounds = Rect(0, 0, 2, 1)
            fillChar = '\u0301'
        })
        val renderer = TuiRenderer(2, 1)

        renderer.renderToBuffer(root, FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 0).content)
        assertEquals(" ", renderer.buffer.get(1, 0).content)
    }

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
    fun oneColumnTallBorderSkipsRightEdges() {
        val panel = TuiNode("Panel").apply {
            bounds = Rect(0, 0, 1, 3)
            drawBorder = true
        }
        val renderer = TuiRenderer(3, 4)

        renderer.renderToBuffer(root(panel), FocusManager())

        assertEquals("+", renderer.buffer.get(0, 0).content)
        assertEquals("|", renderer.buffer.get(0, 1).content)
        assertEquals("+", renderer.buffer.get(0, 2).content)
        assertEquals(" ", renderer.buffer.get(1, 1).content)
        assertEquals(" ", renderer.buffer.get(1, 2).content)
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
    fun imageWithoutFallbackDoesNotWriteTextWhenCapsAreUnknown() {
        val node = TuiNode("Image").apply {
            bounds = Rect(0, 0, 2, 1)
            image = image(fallback = null)
        }
        val renderer = TuiRenderer(8, 3)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals(" ", renderer.buffer.get(0, 0).content)
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
    fun highlightEndClampsToNodeWidthAndPreservesExistingStyleFallbacks() {
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 3, 1)
            text = "abc"
            style = Style(fg = Ansi.FG_GREEN, bg = Ansi.BG_BLUE)
            textHighlights = listOf(TextHighlight(1, 99, Style()))
        }
        val renderer = TuiRenderer(6, 2)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals("b", renderer.buffer.get(1, 0).content)
        assertEquals(Ansi.FG_GREEN, renderer.buffer.get(1, 0).style.fg)
        assertEquals(Ansi.BG_BLUE, renderer.buffer.get(1, 0).style.bg)
        assertEquals(" ", renderer.buffer.get(3, 0).content)
    }

    @Test
    fun borderWithoutTitleDrawsInteriorAndBottomForLargerBounds() {
        val panel = TuiNode("Panel").apply {
            bounds = Rect(1, 1, 4, 4)
            drawBorder = true
        }
        val renderer = TuiRenderer(8, 6)

        renderer.renderToBuffer(root(panel), FocusManager())

        assertEquals("+", renderer.buffer.get(1, 1).content)
        assertEquals("-", renderer.buffer.get(2, 1).content)
        assertEquals("|", renderer.buffer.get(1, 2).content)
        assertEquals(" ", renderer.buffer.get(2, 2).content)
        assertEquals("|", renderer.buffer.get(4, 2).content)
        assertEquals("+", renderer.buffer.get(1, 4).content)
        assertEquals("-", renderer.buffer.get(2, 4).content)
        assertEquals("+", renderer.buffer.get(4, 4).content)
    }

    @Test
    fun focusedNodeWithoutFocusedStyleUsesBaseStyle() {
        val focusManager = FocusManager()
        val node = TuiNode("Input").apply {
            bounds = Rect(0, 0, 4, 1)
            text = "ok"
            focusable = true
            focusId = focusManager.allocateFocusId()
            style = Style(fg = Ansi.FG_YELLOW)
        }
        focusManager.requestFocus(node.focusId)
        val renderer = TuiRenderer(8, 2)

        renderer.renderToBuffer(root(node), focusManager)

        assertEquals(Ansi.FG_YELLOW, renderer.buffer.get(0, 0).style.fg)
    }

    @Test
    fun textIsClippedToNodeDisplayWidth() {
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 3, 1)
            text = "abcd"
        }
        val renderer = TuiRenderer(8, 2)

        renderer.renderToBuffer(root(node), FocusManager())

        assertEquals("a", renderer.buffer.get(0, 0).content)
        assertEquals("b", renderer.buffer.get(1, 0).content)
        assertEquals("c", renderer.buffer.get(2, 0).content)
        assertEquals(" ", renderer.buffer.get(3, 0).content)
    }

    @Test
    fun childrenRenderAfterParentAndCanOverlayByZIndex() {
        val parent = TuiNode("Fill").apply {
            bounds = Rect(0, 0, 3, 1)
            fillChar = '.'
        }
        val child = TuiNode("Text").apply {
            bounds = Rect(1, 0, 1, 1)
            text = "X"
            zIndex = 2
        }
        parent.insertAt(0, child)
        val renderer = TuiRenderer(6, 2)

        renderer.renderToBuffer(root(parent), FocusManager())

        assertEquals(".", renderer.buffer.get(0, 0).content)
        assertEquals("X", renderer.buffer.get(1, 0).content)
        assertEquals(2, renderer.buffer.get(1, 0).zIndex)
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

    @Test
    fun renderFlushesStyledCellsAndHidesCursorWhenNoCursorNodeExists() {
        var output = ""
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 2, 1)
            text = "ab"
            style = Style(fg = Ansi.FG_RED, bold = true)
        }
        val renderer = TuiRenderer(3, 1) { output = it }

        renderer.render(root(node), FocusManager())

        assertTrue(output.startsWith(Ansi.CURSOR_HIDE))
        assertTrue(output.contains(Ansi.cursorTo(1, 1)))
        assertTrue(output.contains(Ansi.FG_RED))
        assertTrue(output.contains(Ansi.BOLD))
        assertTrue(output.endsWith(Ansi.CURSOR_HIDE))
    }

    @Test
    fun renderFlushSkipsWideContinuationCells() {
        var output = ""
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 2, 1)
            text = "あ"
        }
        val renderer = TuiRenderer(3, 1) { output = it }

        renderer.render(root(node), FocusManager())

        assertEquals(1, output.count { it == 'あ' })
    }

    @Test
    fun renderFlushesBackgroundUnderlineAndReverseStyles() {
        var output = ""
        val node = TuiNode("Text").apply {
            bounds = Rect(0, 0, 1, 1)
            text = "x"
            style = Style(bg = Ansi.BG_BLUE, underline = true, reverse = true)
        }
        val renderer = TuiRenderer(1, 1) { output = it }

        renderer.render(root(node), FocusManager())

        assertTrue(output.contains(Ansi.BG_BLUE))
        assertTrue(output.contains(Ansi.UNDERLINE))
        assertTrue(output.contains(Ansi.REVERSE))
    }

    @Test
    fun renderShowsCursorAtFocusedCursorNode() {
        var output = ""
        val focusManager = FocusManager()
        val node = TuiNode("Input").apply {
            bounds = Rect(2, 1, 4, 1)
            text = "abcd"
            focusable = true
            focusId = focusManager.allocateFocusId()
            cursorCol = 2
            cursorRow = 0
        }
        focusManager.requestFocus(node.focusId)
        val renderer = TuiRenderer(8, 3) { output = it }

        renderer.render(root(node), focusManager)

        assertTrue(output.contains(Ansi.cursorTo(2, 5)))
        assertTrue(output.endsWith(Ansi.CURSOR_SHOW))
    }

    @Test
    fun renderFindsCursorNodeNestedInChildren() {
        var output = ""
        val focusManager = FocusManager()
        val child = TuiNode("Input").apply {
            bounds = Rect(1, 1, 2, 1)
            text = "xy"
            focusable = true
            focusId = focusManager.allocateFocusId()
            cursorCol = 1
        }
        val parent = TuiNode("Box").apply { insertAt(0, child) }
        focusManager.requestFocus(child.focusId)
        val renderer = TuiRenderer(6, 3) { output = it }

        renderer.render(root(parent), focusManager)

        assertTrue(output.contains(Ansi.cursorTo(2, 3)))
        assertTrue(output.endsWith(Ansi.CURSOR_SHOW))
    }

    @Test
    fun renderFlushesKittyImagesAndDeletesPreviousKittyImages() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        var output = ""
        val node = TuiNode("Image").apply {
            bounds = Rect(0, 0, 1, 1)
            image = image(fallback = "ALT")
        }
        val renderer = TuiRenderer(4, 2) { output = it }

        renderer.render(root(node), FocusManager())

        assertTrue(output.contains(dev.usbharu.kotui.utils.Kitty.DELETE_ALL))
        assertTrue(output.contains(Ansi.cursorTo(1, 1)))
        assertTrue(output.contains(node.image!!.kitty))
    }

    @Test
    fun renderFlushesSixelImagesWhenSixelIsSupported() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = true, kittySupported = false))
        var output = ""
        val node = TuiNode("Image").apply {
            bounds = Rect(1, 1, 1, 1)
            image = image(fallback = "ALT")
        }
        val renderer = TuiRenderer(4, 2) { output = it }

        renderer.render(root(node), FocusManager())

        assertTrue(output.contains(Ansi.cursorTo(2, 2)))
        assertTrue(output.contains(node.image!!.sixel))
    }

}

package dev.usbharu.kotui.compose.render

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.Kitty
import dev.usbharu.kotui.utils.SixelSupport
import dev.usbharu.kotui.utils.TerminalCaps
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TuiRendererFlushTest {
    @AfterTest
    fun clearTerminalCaps() {
        SixelSupport.overrideForTesting(null)
    }

    @Test
    fun renderFlushesTextAndFocusedCursorToStdout() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val root = TuiNode("root")
        val text = TuiNode("text").apply {
            bounds = Rect(0, 0, 2, 1)
            this.text = "hi"
            focusable = true
            focusId = 1
            cursorCol = 1
            cursorRow = 0
        }
        root.insertAt(0, text)
        val focusManager = FocusManager()
        focusManager.requestFocus(1)

        val output = captureStdout {
            TuiRenderer(3, 1).render(root, focusManager)
        }

        assertContains(output, Ansi.CURSOR_HIDE)
        assertContains(output, "hi ")
        assertContains(output, Ansi.cursorTo(1, 2))
        assertTrue(output.endsWith(Ansi.CURSOR_SHOW))
    }

    @Test
    fun renderFlushesKittyImageAfterClearingPersistentImages() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        val image = TerminalImage(
            rgba = byteArrayOf(
                0xff.toByte(), 0, 0, 0xff.toByte(),
            ),
            pixelWidth = 1,
            pixelHeight = 1,
        )
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("image").apply {
            bounds = Rect(1, 0, 1, 1)
            this.image = image
        })

        val output = captureStdout {
            TuiRenderer(3, 1).render(root, FocusManager())
        }

        assertContains(output, Kitty.DELETE_ALL)
        assertContains(output, Ansi.cursorTo(1, 2))
        assertContains(output, image.kitty)
        assertFalse(output.endsWith(Ansi.CURSOR_SHOW))
        assertTrue(output.endsWith(Ansi.CURSOR_HIDE))
    }

    @Test
    fun zeroSizedViewportWithImageDoesNotThrow() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("image").apply {
            bounds = Rect(0, 0, 1, 1)
            image = TerminalImage(byteArrayOf(0, 0, 0, 0), 1, 1)
        })

        val output = captureStdout { TuiRenderer(0, 0).render(root, FocusManager()) }

        assertFalse(output.contains(root.children.single().image!!.kitty))
        assertTrue(output.endsWith(Ansi.CURSOR_HIDE))
    }

    @Test
    fun fullyOffscreenImageIsNotMovedOntoViewportEdge() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        val image = TerminalImage(byteArrayOf(0, 0, 0, 0), 1, 1)
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("image").apply {
            bounds = Rect(-2, 0, 1, 1)
            this.image = image
        })

        val output = captureStdout { TuiRenderer(2, 1).render(root, FocusManager()) }

        assertFalse(output.contains(image.kitty))
    }

    @Test
    fun cursorCoordinatesAreClampedToViewport() {
        SixelSupport.overrideForTesting(TerminalCaps.UNSUPPORTED)
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("input").apply {
            bounds = Rect(Int.MAX_VALUE, Int.MIN_VALUE, 1, 1)
            focusable = true
            focusId = 9
            cursorCol = Int.MAX_VALUE
            cursorRow = Int.MIN_VALUE
        })
        val focus = FocusManager().apply { requestFocus(9) }

        val output = captureStdout { TuiRenderer(5, 3).render(root, focus) }

        assertContains(output, Ansi.cursorTo(1, 5))
    }

    @Test
    fun textDrawnOverImagePreventsProtocolImageFromCoveringIt() {
        SixelSupport.overrideForTesting(TerminalCaps(sixelSupported = false, kittySupported = true))
        val image = TerminalImage(byteArrayOf(0, 0, 0, 0), 1, 1)
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("image").apply {
            bounds = Rect(0, 0, 1, 1)
            this.image = image
        })
        root.insertAt(1, TuiNode("text").apply {
            bounds = Rect(0, 0, 1, 1)
            text = "X"
        })

        val output = captureStdout { TuiRenderer(1, 1).render(root, FocusManager()) }

        assertContains(output, "X")
        assertFalse(output.contains(image.kitty))
    }

    private fun captureStdout(block: () -> Unit): String {
        val original = System.out
        val bytes = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(bytes, true, Charsets.UTF_8.name()))
            block()
        } finally {
            System.setOut(original)
        }
        return bytes.toString(Charsets.UTF_8.name())
    }
}

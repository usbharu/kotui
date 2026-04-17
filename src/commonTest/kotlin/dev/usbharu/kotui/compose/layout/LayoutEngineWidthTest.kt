package dev.usbharu.kotui.compose.layout

import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutEngineWidthTest {
    @Test
    fun rowLaysOutCjkChildrenByDisplayWidth() {
        val root = TuiNode("Row").apply {
            layoutPolicy = LayoutPolicy.ROW
            layoutGap = 0
            preferredHeight = 1
        }
        val a = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            text = "あ"       // width 2
            preferredHeight = 1
        }
        val b = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            text = "hi"       // width 2
            preferredHeight = 1
        }
        root.insertAt(0, a)
        root.insertAt(1, b)

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 1)

        assertEquals(0, a.bounds.x)
        assertEquals(2, a.bounds.width)
        assertEquals(2, b.bounds.x)
        assertEquals(2, b.bounds.width)
    }

    @Test
    fun rowLaysOutEmojiWithWidthTwo() {
        val root = TuiNode("Row").apply {
            layoutPolicy = LayoutPolicy.ROW
            preferredHeight = 1
        }
        val emoji = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            text = "\uD83D\uDC4D"  // 👍 width 2
            preferredHeight = 1
        }
        val after = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            text = "x"  // width 1
            preferredHeight = 1
        }
        root.insertAt(0, emoji)
        root.insertAt(1, after)

        LayoutEngine.layout(root, 20, 1)

        assertEquals(2, emoji.bounds.width)
        assertEquals(2, after.bounds.x)
        assertEquals(1, after.bounds.width)
    }
}

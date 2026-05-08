package dev.usbharu.kotui.compose.node

import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.flexBasis
import dev.usbharu.kotui.compose.modifier.focusScope
import dev.usbharu.kotui.compose.modifier.focusable
import dev.usbharu.kotui.compose.modifier.gap
import dev.usbharu.kotui.compose.modifier.height
import dev.usbharu.kotui.compose.modifier.offset
import dev.usbharu.kotui.compose.modifier.onKeyEvent
import dev.usbharu.kotui.compose.modifier.size
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.modifier.weight
import dev.usbharu.kotui.compose.modifier.width
import dev.usbharu.kotui.compose.modifier.zIndex
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TuiNodeTest {
    @Test
    fun insertRemoveMoveAndClearMaintainParentLinks() {
        val root = TuiNode("root")
        val a = TuiNode("a")
        val b = TuiNode("b")
        val c = TuiNode("c")
        root.insertAt(0, a)
        root.insertAt(1, b)
        root.insertAt(2, c)

        assertSame(root, a.parent)
        root.move(0, 3, 2)
        assertEquals(listOf("c", "a", "b"), root.children.map { it.tag })
        root.move(1, 1, 1)
        assertEquals(listOf("c", "a", "b"), root.children.map { it.tag })

        root.removeAt(1, 1)
        assertNull(a.parent)
        assertEquals(listOf("c", "b"), root.children.map { it.tag })

        root.clear()
        assertTrue(root.children.isEmpty())
        assertNull(b.parent)
        assertNull(c.parent)
    }

    @Test
    fun modifierChainAppliesAllNodePropertiesInOrder() {
        var handled = false
        val style = Style(fg = Ansi.FG_GREEN)
        val node = TuiNode("node").apply {
            bounds = Rect(0, 0, 9, 8)
            applyModifier(
                Modifier
                    .size(4, 3)
                    .width(5)
                    .height(6)
                    .style(style)
                    .focusable()
                    .focusScope()
                    .zIndex(7)
                    .gap(2)
                    .weight(1.5f)
                    .flexBasis(10)
                    .offset(11, 12)
                    .onKeyEvent {
                        handled = it.key == Key.ENTER
                        handled
                    }
            )
        }

        assertEquals(5, node.preferredWidth)
        assertEquals(6, node.preferredHeight)
        assertEquals(style, node.style)
        assertTrue(node.focusable)
        assertTrue(node.focusScope)
        assertEquals(7, node.zIndex)
        assertEquals(2, node.layoutGap)
        assertEquals(1.5f, node.flexGrow)
        assertEquals(10, node.flexBasis)
        assertEquals(Rect(11, 12, 9, 8), node.bounds)
        assertTrue(node.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertTrue(handled)
    }

    @Test
    fun emptyModifierLeavesNodeUnchangedAndToStringIsStable() {
        val node = TuiNode("node")
        node.applyModifier(Modifier)

        assertNull(node.preferredWidth)
        assertFalse(node.focusable)
        assertEquals("Modifier", Modifier.toString())
    }

    @Test
    fun moveBackwardKeepsMovedChildrenInOrder() {
        val root = TuiNode("root")
        listOf("a", "b", "c", "d").forEachIndexed { index, tag ->
            root.insertAt(index, TuiNode(tag))
        }

        root.move(from = 2, to = 0, count = 2)

        assertEquals(listOf("c", "d", "a", "b"), root.children.map { it.tag })
        assertTrue(root.children.all { it.parent === root })
    }
}

package dev.usbharu.kotui.compose.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertFalse
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
        assertSame(root, b.parent)
        assertSame(root, c.parent)

        root.move(0, 3, 1)
        assertEquals(listOf(b, c, a), root.children)

        root.removeAt(1, 1)
        assertNull(c.parent)
        assertEquals(listOf(b, a), root.children)

        root.clear()
        assertNull(a.parent)
        assertNull(b.parent)
        assertEquals(emptyList(), root.children)
    }

    @Test
    fun insertingChildIntoNewParentDetachesItFromOldParent() {
        val oldParent = TuiNode("old")
        val newParent = TuiNode("new")
        val child = TuiNode("child")
        oldParent.insertAt(0, child)

        newParent.insertAt(0, child)

        assertEquals(emptyList(), oldParent.children)
        assertEquals(listOf(child), newParent.children)
        assertSame(newParent, child.parent)
    }

    @Test
    fun reinsertingWithinSameParentMovesWithoutDuplicatingChild() {
        val root = TuiNode("root")
        val a = TuiNode("a")
        val b = TuiNode("b")
        root.insertAt(0, a)
        root.insertAt(1, b)

        root.insertAt(1, a)

        assertEquals(listOf(b, a), root.children)
        assertSame(root, a.parent)
    }

    @Test
    fun selfAndAncestorInsertionAreRejectedWithoutMutation() {
        val root = TuiNode("root")
        val child = TuiNode("child")
        root.insertAt(0, child)

        assertFailsWith<IllegalArgumentException> { root.insertAt(0, root) }
        assertFailsWith<IllegalArgumentException> { child.insertAt(0, root) }
        assertEquals(listOf(child), root.children)
        assertSame(root, child.parent)
    }

    @Test
    fun invalidRemovalIsAtomic() {
        val root = TuiNode("root")
        val a = TuiNode("a")
        val b = TuiNode("b")
        root.insertAt(0, a)
        root.insertAt(1, b)

        assertFailsWith<IllegalArgumentException> { root.removeAt(0, 3) }
        assertFailsWith<IllegalArgumentException> { root.removeAt(0, -1) }

        assertEquals(listOf(a, b), root.children)
        assertSame(root, a.parent)
        assertSame(root, b.parent)
    }

    @Test
    fun invalidMoveIsAtomic() {
        val root = TuiNode("root")
        val nodes = List(3) { TuiNode("$it") }
        nodes.forEachIndexed(root::insertAt)

        assertFailsWith<IllegalArgumentException> { root.move(1, 3, 3) }
        assertFailsWith<IllegalArgumentException> { root.move(0, 99, 1) }
        assertFailsWith<IllegalArgumentException> { root.move(0, 1, -1) }

        assertEquals(nodes, root.children)
        nodes.forEach { assertSame(root, it.parent) }
    }

    @Test
    fun childrenViewCannotBypassParentLinkMaintenance() {
        val root = TuiNode("root")
        root.insertAt(0, TuiNode("child"))

        assertFalse(root.children is MutableList<*>)
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

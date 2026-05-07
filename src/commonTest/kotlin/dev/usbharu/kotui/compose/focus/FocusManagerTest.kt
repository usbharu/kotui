package dev.usbharu.kotui.compose.focus

import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class FocusManagerTest {
    private fun focusable(manager: FocusManager, tag: String = "Focusable"): TuiNode =
        TuiNode(tag).apply {
            focusable = true
            focusId = manager.allocateFocusId()
        }

    @Test
    fun focusNextAndPreviousCycleThroughFocusableNodes() {
        val manager = FocusManager()
        val a = focusable(manager, "a")
        val b = focusable(manager, "b")
        val root = TuiNode("root").apply {
            insertAt(0, a)
            insertAt(1, b)
        }

        manager.focusNext(root)
        assertEquals(a.focusId, manager.focusedId)
        manager.focusNext(root)
        assertEquals(b.focusId, manager.focusedId)
        manager.focusNext(root)
        assertEquals(a.focusId, manager.focusedId)
        manager.focusPrevious(root)
        assertEquals(b.focusId, manager.focusedId)
    }

    @Test
    fun focusNextUsesNearestFocusScope() {
        val manager = FocusManager()
        val outside = focusable(manager, "outside")
        val first = focusable(manager, "first")
        val second = focusable(manager, "second")
        val scope = TuiNode("scope").apply {
            focusScope = true
            insertAt(0, first)
            insertAt(1, second)
        }
        val root = TuiNode("root").apply {
            insertAt(0, outside)
            insertAt(1, scope)
        }

        manager.requestFocus(first.focusId)
        manager.focusNext(root)

        assertEquals(second.focusId, manager.focusedId)
        manager.focusNext(root)
        assertEquals(first.focusId, manager.focusedId)
    }

    @Test
    fun autoFocusKeepsExistingValidFocusAndRepairsMissingFocus() {
        val manager = FocusManager()
        val a = focusable(manager)
        val b = focusable(manager)
        val root = TuiNode("root").apply {
            insertAt(0, a)
            insertAt(1, b)
        }

        manager.requestFocus(b.focusId)
        manager.autoFocus(root)
        assertEquals(b.focusId, manager.focusedId)

        root.removeAt(1, 1)
        manager.autoFocus(root)
        assertEquals(a.focusId, manager.focusedId)
    }

    @Test
    fun findFocusedNodeReturnsNullWhenNothingIsFocused() {
        val manager = FocusManager()
        val root = TuiNode("root")

        assertNull(manager.findFocusedNode(root))
    }

    @Test
    fun findFocusedNodeReturnsNestedFocusedNode() {
        val manager = FocusManager()
        val child = focusable(manager)
        val root = TuiNode("root").apply { insertAt(0, TuiNode("box").apply { insertAt(0, child) }) }

        manager.requestFocus(child.focusId)

        assertSame(child, manager.findFocusedNode(root))
    }

    @Test
    fun focusNextAndPreviousIgnoreTreesWithoutFocusableNodes() {
        val manager = FocusManager()
        val root = TuiNode("root")

        manager.focusNext(root)
        assertEquals(-1, manager.focusedId)
        manager.focusPrevious(root)
        assertEquals(-1, manager.focusedId)
    }
}

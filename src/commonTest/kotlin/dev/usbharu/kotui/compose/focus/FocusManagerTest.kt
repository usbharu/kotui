package dev.usbharu.kotui.compose.focus

import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FocusManagerTest {
    private fun focusable(manager: FocusManager, tag: String = "Focusable"): TuiNode =
        TuiNode(tag).apply {
            focusable = true
            focusId = manager.allocateFocusId()
        }

    private fun node(id: Int, focusable: Boolean = true): TuiNode =
        TuiNode("node-$id").apply {
            this.focusable = focusable
            focusId = id
        }

    @Test
    fun allocateFocusIdReturnsIncreasingIds() {
        val manager = FocusManager()

        assertEquals(0, manager.allocateFocusId())
        assertEquals(1, manager.allocateFocusId())
        assertEquals(2, manager.allocateFocusId())
    }

    @Test
    fun requestFocusUpdatesFocusedState() {
        val manager = FocusManager()

        manager.requestFocus(7)

        assertEquals(7, manager.focusedId)
        assertTrue(manager.isFocused(7))
    }

    @Test
    fun autoFocusSelectsFirstFocusableAndKeepsValidFocus() {
        val root = TuiNode("root")
        val first = node(10)
        val second = node(11)
        root.insertAt(0, first)
        root.insertAt(1, second)
        val manager = FocusManager()

        manager.autoFocus(root)
        manager.autoFocus(root)

        assertEquals(10, manager.focusedId)
        assertSame(first, manager.findFocusedNode(root))
    }

    @Test
    fun autoFocusClearsFocusWhenFocusedNodeDisappearsAndNoCandidatesRemain() {
        val root = TuiNode("root")
        val child = node(1)
        root.insertAt(0, child)
        val manager = FocusManager()
        manager.requestFocus(1)
        root.removeAt(0, 1)

        manager.autoFocus(root)

        assertEquals(-1, manager.focusedId)
        assertNull(manager.findFocusedNode(root))
    }

    @Test
    fun focusNextAndPreviousWrapWithinCurrentScope() {
        val root = TuiNode("root")
        val beforeScope = node(1)
        val scope = TuiNode("scope").apply { focusScope = true }
        val scopedA = node(2)
        val scopedB = node(3)
        val afterScope = node(4)
        root.insertAt(0, beforeScope)
        root.insertAt(1, scope)
        scope.insertAt(0, scopedA)
        scope.insertAt(1, scopedB)
        root.insertAt(2, afterScope)
        val manager = FocusManager()
        manager.requestFocus(2)

        manager.focusNext(root)
        assertEquals(3, manager.focusedId)

        manager.focusNext(root)
        assertEquals(2, manager.focusedId)

        manager.focusPrevious(root)
        assertEquals(3, manager.focusedId)
    }

    @Test
    fun focusNextFallsBackToRootWhenCurrentFocusIsUnknown() {
        val root = TuiNode("root")
        root.insertAt(0, node(5))
        root.insertAt(1, node(6))
        val manager = FocusManager()
        manager.requestFocus(99)

        manager.focusNext(root)

        assertEquals(5, manager.focusedId)
    }

    @Test
    fun focusPreviousUsesLastFocusableWhenNothingIsFocused() {
        val root = TuiNode("root")
        root.insertAt(0, node(5))
        root.insertAt(1, node(6))
        val manager = FocusManager()

        manager.focusPrevious(root)

        assertEquals(6, manager.focusedId)
    }

    @Test
    fun focusNavigationIgnoresNonFocusableNodesAndEmptyTrees() {
        val root = TuiNode("root")
        root.insertAt(0, node(1, focusable = false))
        val manager = FocusManager()

        manager.focusNext(root)
        manager.focusPrevious(root)

        assertEquals(-1, manager.focusedId)
    }

    @Test
    fun autoFocusMovesAwayFromNodeThatBecameNonFocusable() {
        val root = TuiNode("root")
        val first = node(1)
        val second = node(2)
        root.insertAt(0, first)
        root.insertAt(1, second)
        val manager = FocusManager()
        manager.requestFocus(1)
        first.focusable = false

        manager.autoFocus(root)

        assertEquals(2, manager.focusedId)
        assertSame(second, manager.findFocusedNode(root))
    }

    @Test
    fun findFocusedNodeDoesNotReturnNonFocusableNode() {
        val root = TuiNode("root")
        root.insertAt(0, node(4, focusable = false))
        val manager = FocusManager()
        manager.requestFocus(4)

        assertNull(manager.findFocusedNode(root))
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

    @Test
    fun focusPreviousUsesNearestScopeAndWrapsInsideScope() {
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
        manager.focusPrevious(root)

        assertEquals(second.focusId, manager.focusedId)
        manager.focusPrevious(root)
        assertEquals(first.focusId, manager.focusedId)
    }

    @Test
    fun autoFocusClearsWhenTreeHasNoFocusableReplacement() {
        val manager = FocusManager()
        val child = focusable(manager)
        val root = TuiNode("root").apply { insertAt(0, child) }

        manager.requestFocus(child.focusId)
        root.clear()
        manager.autoFocus(root)

        assertEquals(-1, manager.focusedId)
    }

}

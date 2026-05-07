package dev.usbharu.kotui.compose.runtime

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TuiEventDispatcherTest {
    private fun focusable(manager: FocusManager, tag: String): TuiNode =
        TuiNode(tag).apply {
            focusable = true
            focusId = manager.allocateFocusId()
        }

    @Test
    fun tabMovesFocusAndConsumesKey() {
        val manager = FocusManager()
        val first = focusable(manager, "first")
        val second = focusable(manager, "second")
        val root = TuiNode("root").apply {
            insertAt(0, first)
            insertAt(1, second)
        }
        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertTrue(dispatcher.dispatchKey(KeyEvent('\t', Key.TAB)))
        assertEquals(first.focusId, manager.focusedId)
        assertTrue(dispatcher.dispatchKey(KeyEvent('\t', Key.TAB)))
        assertEquals(second.focusId, manager.focusedId)
    }

    @Test
    fun focusedNodeKeyHandlerConsumesBeforeParent() {
        val manager = FocusManager()
        val calls = mutableListOf<String>()
        val child = focusable(manager, "child").apply {
            onKeyEvent = { calls += "child"; true }
        }
        val parent = TuiNode("parent").apply {
            onKeyEvent = { calls += "parent"; true }
            insertAt(0, child)
        }
        val root = TuiNode("root").apply { insertAt(0, parent) }
        manager.requestFocus(child.focusId)

        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertTrue(dispatcher.dispatchKey(KeyEvent('x', Key.CHAR)))
        assertEquals(listOf("child"), calls)
    }

    @Test
    fun keyBubblesToParentWhenFocusedNodeDoesNotConsume() {
        val manager = FocusManager()
        val calls = mutableListOf<String>()
        val child = focusable(manager, "child").apply {
            onKeyEvent = { calls += "child"; false }
        }
        val parent = TuiNode("parent").apply {
            onKeyEvent = { calls += "parent"; true }
            insertAt(0, child)
        }
        val root = TuiNode("root").apply { insertAt(0, parent) }
        manager.requestFocus(child.focusId)

        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertTrue(dispatcher.dispatchKey(KeyEvent('x', Key.CHAR)))
        assertEquals(listOf("child", "parent"), calls)
    }

    @Test
    fun unhandledKeyIsPublishedAndNotConsumed() {
        val manager = FocusManager()
        val child = focusable(manager, "child")
        val root = TuiNode("root").apply { insertAt(0, child) }
        manager.requestFocus(child.focusId)
        var published: KeyEvent? = null
        val event = KeyEvent('x', Key.CHAR)

        val dispatcher = TuiEventDispatcher(root, manager) { published = it }

        assertFalse(dispatcher.dispatchKey(event))
        assertEquals(event, published)
    }

    @Test
    fun keyWithoutFocusedNodeIsPublished() {
        var published: KeyEvent? = null
        val event = KeyEvent('x', Key.CHAR)
        val dispatcher = TuiEventDispatcher(TuiNode("root"), FocusManager()) { published = it }

        assertFalse(dispatcher.dispatchKey(event))
        assertEquals(event, published)
    }

    @Test
    fun pasteBubblesFromFocusedNodeToParent() {
        val manager = FocusManager()
        val calls = mutableListOf<String>()
        val child = focusable(manager, "child").apply {
            onPaste = { calls += "child:$it"; false }
        }
        val parent = TuiNode("parent").apply {
            onPaste = { calls += "parent:$it"; true }
            insertAt(0, child)
        }
        val root = TuiNode("root").apply { insertAt(0, parent) }
        manager.requestFocus(child.focusId)

        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertTrue(dispatcher.dispatchPaste("hello"))
        assertEquals(listOf("child:hello", "parent:hello"), calls)
    }

    @Test
    fun focusedNodePasteHandlerConsumesBeforeParent() {
        val manager = FocusManager()
        val calls = mutableListOf<String>()
        val child = focusable(manager, "child").apply {
            onPaste = { calls += "child:$it"; true }
        }
        val parent = TuiNode("parent").apply {
            onPaste = { calls += "parent:$it"; true }
            insertAt(0, child)
        }
        val root = TuiNode("root").apply { insertAt(0, parent) }
        manager.requestFocus(child.focusId)

        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertTrue(dispatcher.dispatchPaste("hello"))
        assertEquals(listOf("child:hello"), calls)
    }

    @Test
    fun pasteFallsThroughWhenNoHandlerConsumes() {
        val manager = FocusManager()
        val child = focusable(manager, "child").apply {
            onPaste = { false }
        }
        val root = TuiNode("root").apply { insertAt(0, child) }
        manager.requestFocus(child.focusId)

        val dispatcher = TuiEventDispatcher(root, manager) {}

        assertFalse(dispatcher.dispatchPaste("hello"))
    }

    @Test
    fun pasteWithoutFocusedNodeIsIgnored() {
        val dispatcher = TuiEventDispatcher(TuiNode("root"), FocusManager()) {}

        assertFalse(dispatcher.dispatchPaste("hello"))
    }

    @Test
    fun dispatchEventRoutesByEventType() {
        val manager = FocusManager()
        val child = focusable(manager, "child").apply {
            onKeyEvent = { true }
            onPaste = { true }
        }
        val root = TuiNode("root").apply { insertAt(0, child) }
        manager.requestFocus(child.focusId)
        val dispatcher = TuiEventDispatcher(root, manager) {}

        dispatcher.dispatchEvent(KeyEvent('x', Key.CHAR))
        dispatcher.dispatchEvent(PasteEvent("pasted"))
    }
}

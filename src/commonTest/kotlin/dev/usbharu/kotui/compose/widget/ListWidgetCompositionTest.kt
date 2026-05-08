package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListWidgetCompositionTest {

    @Test
    fun radioGroupEmitsTextChildrenForEachOption() = runTest {
        val session = composeWithDefaults {
            RadioGroup(options = listOf("A", "B"), selected = "A", onSelectedChange = {})
        }

        session.awaitIdle()

        val radio = session.root.children.single()
        assertEquals("RadioGroup", radio.tag)
        assertEquals(LayoutPolicy.COLUMN, radio.layoutPolicy)
        assertTrue(radio.focusable)
        assertNotNull(radio.onKeyEvent)
        assertEquals(2, radio.preferredHeight)
        assertEquals(2, radio.children.size)
        assertEquals("Text", radio.children[0].tag)
        assertEquals("Text", radio.children[1].tag)

        session.dispose()
    }

    @Test
    fun radioGroupArrowNavigationRequestsNewSelection() = runTest {
        val changes = mutableListOf<String>()
        val session = composeWithDefaults {
            RadioGroup(options = listOf("A", "B", "C"), selected = "A", onSelectedChange = changes::add)
        }

        session.awaitIdle()

        val radio = session.root.children.single()
        assertTrue(radio.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))
        assertTrue(radio.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.END)))
        assertEquals(listOf("B", "C"), changes)

        session.dispose()
    }

    @Test
    fun radioGroupEmptyStateDoesNotConsumeNavigation() = runTest {
        val session = composeWithDefaults {
            RadioGroup(options = emptyList<String>(), selected = "missing", onSelectedChange = {})
        }

        session.awaitIdle()

        val radio = session.root.children.single()
        assertEquals(1, radio.preferredHeight)
        assertEquals(1, radio.children.size)
        assertEquals("  (no options)", radio.children.single().text)
        assertFalse(radio.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))

        session.dispose()
    }

    @Test
    fun selectableListEmitsTextChildrenForItems() = runTest {
        val session = composeWithDefaults {
            SelectableList(items = listOf("A", "B"), selectedIndex = 0, onSelectedIndexChange = {})
        }

        session.awaitIdle()

        val list = session.root.children.single()
        assertEquals("SelectableList", list.tag)
        assertEquals(LayoutPolicy.COLUMN, list.layoutPolicy)
        assertTrue(list.focusable)
        assertNotNull(list.onKeyEvent)
        assertEquals(2, list.children.size)
        assertEquals("Text", list.children[0].tag)
        assertEquals("Text", list.children[1].tag)

        session.dispose()
    }

    @Test
    fun selectableListHandlesNavigationActivationAndVisibleRows() = runTest {
        var moved: Int? = null
        var activated: Pair<Int, String>? = null
        val session = composeWithDefaults {
            SelectableList(
                items = listOf("A", "B", "C"),
                selectedIndex = 1,
                onSelectedIndexChange = { moved = it },
                visibleRows = 2,
                onActivate = { index, value -> activated = index to value },
            )
        }

        session.awaitIdle()

        val list = session.root.children.single()
        assertEquals(2, list.preferredHeight)
        assertEquals(2, list.children.size)
        assertEquals("  A", list.children[0].text)
        assertEquals("  B", list.children[1].text)
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))
        assertEquals(2, moved)
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertEquals(1 to "B", activated)

        session.dispose()
    }

    @Test
    fun selectableListEmptyStateDoesNotConsumeKeys() = runTest {
        val session = composeWithDefaults {
            SelectableList(items = emptyList<String>(), selectedIndex = 0, onSelectedIndexChange = {})
        }

        session.awaitIdle()

        val list = session.root.children.single()
        assertEquals(1, list.preferredHeight)
        assertEquals(1, list.children.size)
        assertEquals("  (empty)", list.children.single().text)
        assertFalse(list.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))
        assertFalse(list.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))

        session.dispose()
    }

    @Test
    fun multiSelectListEmitsTextChildrenForItems() = runTest {
        val session = composeWithDefaults {
            MultiSelectList(
                items = listOf("A", "B"),
                cursorIndex = 0,
                onCursorIndexChange = {},
                checkedIndices = setOf(1),
                onCheckedIndicesChange = {},
            )
        }

        session.awaitIdle()

        val list = session.root.children.single()
        assertEquals("MultiSelectList", list.tag)
        assertEquals(LayoutPolicy.COLUMN, list.layoutPolicy)
        assertTrue(list.focusable)
        assertNotNull(list.onKeyEvent)
        assertEquals(2, list.children.size)
        assertEquals("Text", list.children[0].tag)
        assertEquals("Text", list.children[1].tag)

        session.dispose()
    }

    @Test
    fun multiSelectListHandlesToggleBulkSelectionAndActivation() = runTest {
        var cursor: Int? = null
        val checkedChanges = mutableListOf<Set<Int>>()
        var activated: Pair<Int, String>? = null
        val session = composeWithDefaults {
            MultiSelectList(
                items = listOf("A", "B", "C"),
                cursorIndex = 1,
                onCursorIndexChange = { cursor = it },
                checkedIndices = setOf(0),
                onCheckedIndicesChange = checkedChanges::add,
                onActivate = { index, value -> activated = index to value },
            )
        }

        session.awaitIdle()

        val list = session.root.children.single()
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_UP)))
        assertEquals(0, cursor)
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent(' ', Key.CHAR)))
        assertEquals(setOf(0, 1), checkedChanges.last())
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('a', Key.CHAR, ctrl = true)))
        assertEquals(setOf(0, 1, 2), checkedChanges.last())
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('d', Key.CHAR, ctrl = true)))
        assertEquals(emptySet(), checkedChanges.last())
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertEquals(1 to "B", activated)

        session.dispose()
    }
}

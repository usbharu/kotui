package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListWidgetCompositionTest {

    @Test
    fun radioGroupEmitsTextChildrenForEachOption() = runBlocking {
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
    fun selectableListEmitsTextChildrenForItems() = runBlocking {
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
    fun multiSelectListEmitsTextChildrenForItems() = runBlocking {
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
}

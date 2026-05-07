package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InteractiveWidgetCompositionTest {

    @Test
    fun buttonIsFocusableWithKeyEvent() = runBlocking {
        val session = composeWithDefaults {
            Button("Go")
        }

        session.awaitIdle()

        val button = session.root.children.single()
        assertEquals("Button", button.tag)
        assertEquals(LayoutPolicy.LEAF, button.layoutPolicy)
        assertEquals(1, button.preferredHeight)
        assertTrue(button.focusable)
        assertNotNull(button.onKeyEvent)
        assertNotNull(button.onActivate)
        assertNotNull(button.text)
        assertTrue(button.text!!.contains("Go"))

        session.dispose()
    }

    @Test
    fun checkboxIsFocusableAndShowsCheckedState() = runBlocking {
        val session = composeWithDefaults {
            Checkbox(checked = true, label = "Done", onCheckedChange = {})
        }

        session.awaitIdle()

        val checkbox = session.root.children.single()
        assertEquals("Checkbox", checkbox.tag)
        assertEquals(LayoutPolicy.LEAF, checkbox.layoutPolicy)
        assertEquals(1, checkbox.preferredHeight)
        assertTrue(checkbox.focusable)
        assertNotNull(checkbox.onKeyEvent)
        assertNotNull(checkbox.onActivate)
        assertNotNull(checkbox.text)

        session.dispose()
    }

    @Test
    fun selectIsFocusableWithKeyEvent() = runBlocking {
        val session = composeWithDefaults {
            Select(items = listOf("A", "B"), selected = "A", onSelectedChange = {})
        }

        session.awaitIdle()

        val selectNode = session.root.children.first { it.tag == "Select" }
        assertEquals("Select", selectNode.tag)
        assertEquals(LayoutPolicy.LEAF, selectNode.layoutPolicy)
        assertEquals(1, selectNode.preferredHeight)
        assertTrue(selectNode.focusable)
        assertNotNull(selectNode.onKeyEvent)
        assertNotNull(selectNode.onActivate)
        assertNotNull(selectNode.text)

        session.dispose()
    }
}

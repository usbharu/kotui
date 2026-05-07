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

class InteractiveWidgetCompositionTest {

    @Test
    fun buttonIsFocusableWithKeyEvent() = runTest {
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
    fun buttonEnterActivatesButPlainSpaceDoesNot() = runTest {
        var clicks = 0
        val session = composeWithDefaults {
            Button("Go") { clicks++ }
        }

        session.awaitIdle()

        val button = session.root.children.single()
        assertTrue(button.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertEquals(1, clicks)
        assertFalse(button.onKeyEvent!!.invoke(KeyEvent(' ', Key.CHAR)))
        assertEquals(1, clicks)
        button.onActivate!!.invoke()
        assertEquals(2, clicks)

        session.dispose()
    }

    @Test
    fun checkboxIsFocusableAndShowsCheckedState() = runTest {
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
    fun checkboxActivationRequestsToggledValue() = runTest {
        val changes = mutableListOf<Boolean>()
        val session = composeWithDefaults {
            Checkbox(checked = false, label = "Done", onCheckedChange = changes::add)
        }

        session.awaitIdle()

        val checkbox = session.root.children.single()
        assertTrue(checkbox.onKeyEvent!!.invoke(KeyEvent(' ', Key.CHAR)))
        assertTrue(checkbox.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertEquals(listOf(true, true), changes)
        checkbox.onActivate!!.invoke()
        assertEquals(listOf(true, true, true), changes)

        session.dispose()
    }

    @Test
    fun selectIsFocusableWithKeyEvent() = runTest {
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

    @Test
    fun selectActivationExpandsAndSelectingItemClosesDropdown() = runTest {
        var selected = "A"
        val session = composeWithDefaults {
            Select(
                items = listOf("A", "B", "C"),
                selected = selected,
                onSelectedChange = { selected = it },
                dropdownWidth = 12,
                dropdownHeight = 4,
            )
        }

        session.awaitIdle()

        val selectNode = session.root.children.single()
        assertTrue(selectNode.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        session.applySnapshotAndAwaitIdle()

        assertEquals(2, session.root.children.size)
        val modal = session.root.children[1]
        assertEquals("Panel", modal.tag)
        assertTrue(modal.focusScope)
        assertEquals(12, modal.preferredWidth)
        assertEquals(4, modal.preferredHeight)

        val list = modal.children.single { it.tag == "SelectableList" }
        assertEquals(2, list.children.size)
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))
        session.applySnapshotAndAwaitIdle()
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        session.applySnapshotAndAwaitIdle()

        assertEquals("B", selected)
        assertEquals(listOf("Select"), session.root.children.map { it.tag })

        session.dispose()
    }
}

package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.mutableStateOf
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.height
import dev.usbharu.kotui.compose.modifier.width
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WidgetRecompositionCompositionTest {

    @Test
    fun buttonRecomposesLabelModifierFocusAndCallback() = runTest {
        val label = mutableStateOf("Save")
        val width = mutableStateOf(8)
        var callbackVersion = "initial"
        val clicks = mutableListOf<String>()
        val session = composeWithDefaults {
            Button(label.value, Modifier.width(width.value)) {
                clicks += callbackVersion
            }
        }

        session.awaitIdle()
        val button = session.root.children.single()
        assertEquals("  Save  ", button.text)
        assertEquals(8, button.preferredWidth)

        callbackVersion = "updated"
        label.value = "Publish"
        width.value = 12
        session.focusManager.requestFocus(button.focusId)
        session.applySnapshotAndAwaitIdle()

        assertSame(button, session.root.children.single())
        assertEquals("[ Publish ]", button.text)
        assertEquals(12, button.preferredWidth)
        assertTrue(button.onKeyEvent!!.invoke(KeyEvent('\n', Key.ENTER)))
        assertEquals(listOf("updated"), clicks)

        session.dispose()
    }

    @Test
    fun checkboxRecomposesCheckedStateFocusAndToggleTarget() = runTest {
        val checked = mutableStateOf(false)
        val label = mutableStateOf("")
        val requested = mutableListOf<Boolean>()
        val session = composeWithDefaults {
            Checkbox(
                checked = checked.value,
                label = label.value,
                onCheckedChange = requested::add,
            )
        }

        session.awaitIdle()
        val checkbox = session.root.children.single()
        assertEquals("  [ ]", checkbox.text)
        assertTrue(checkbox.onKeyEvent!!.invoke(KeyEvent(' ', Key.CHAR)))
        assertEquals(listOf(true), requested)

        checked.value = true
        label.value = "Done"
        session.focusManager.requestFocus(checkbox.focusId)
        session.applySnapshotAndAwaitIdle()

        assertSame(checkbox, session.root.children.single())
        assertEquals("▶ [x] Done", checkbox.text)
        checkbox.onActivate!!.invoke()
        assertEquals(listOf(true, false), requested)

        session.dispose()
    }

    @Test
    fun radioGroupRecomposesOptionsSelectionAndCustomLabels() = runTest {
        val options = mutableStateOf(listOf(1, 2, 3))
        val selected = mutableStateOf(2)
        val changes = mutableListOf<Int>()
        val session = composeWithDefaults {
            RadioGroup(
                options = options.value,
                selected = selected.value,
                onSelectedChange = changes::add,
                optionLabel = { "item-$it" },
            )
        }

        session.awaitIdle()
        val radio = session.root.children.single()
        assertEquals(listOf("  ( ) item-1", "  (●) item-2", "  ( ) item-3"), radio.children.map { it.text })
        assertTrue(radio.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.ARROW_DOWN)))
        assertEquals(listOf(3), changes)

        options.value = listOf(4, 5)
        selected.value = 99
        session.focusManager.requestFocus(radio.focusId)
        session.applySnapshotAndAwaitIdle()

        assertSame(radio, session.root.children.single())
        assertEquals(2, radio.preferredHeight)
        assertEquals(listOf("▶ (●) item-4", "  ( ) item-5"), radio.children.map { it.text })
        assertTrue(radio.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.END)))
        assertEquals(listOf(3, 5), changes)

        session.dispose()
    }

    @Test
    fun selectableListRecomposesViewportFocusAndModifierHeight() = runTest {
        val items = mutableStateOf(listOf("A", "B", "C", "D"))
        val selected = mutableStateOf(0)
        val height = mutableStateOf(4)
        val moves = mutableListOf<Int>()
        val session = composeWithDefaults {
            SelectableList(
                items = items.value,
                selectedIndex = selected.value,
                onSelectedIndexChange = moves::add,
                visibleRows = 2,
                modifier = Modifier.height(height.value),
            )
        }

        session.awaitIdle()
        val list = session.root.children.single()
        assertEquals(4, list.preferredHeight)
        assertEquals(listOf("  A", "  B"), list.children.map { it.text })

        selected.value = 1
        height.value = 5
        session.focusManager.requestFocus(list.focusId)
        session.applySnapshotAndAwaitIdle()

        assertSame(list, session.root.children.single())
        assertEquals(5, list.preferredHeight)
        assertEquals(listOf("  A", "▶ B"), list.children.map { it.text })
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent('\u0000', Key.PAGE_DOWN)))
        assertEquals(listOf(3), moves)

        session.dispose()
    }

    @Test
    fun multiSelectListRecomposesCheckedRowsAndClampedCursor() = runTest {
        val items = mutableStateOf(listOf("A", "B", "C"))
        val cursor = mutableStateOf(0)
        val checked = mutableStateOf(setOf(1))
        val requested = mutableListOf<Set<Int>>()
        val session = composeWithDefaults {
            MultiSelectList(
                items = items.value,
                cursorIndex = cursor.value,
                onCursorIndexChange = { cursor.value = it },
                checkedIndices = checked.value,
                onCheckedIndicesChange = requested::add,
                visibleRows = 2,
            )
        }

        session.awaitIdle()
        val list = session.root.children.single()
        assertEquals(listOf("  [ ] A", "  [x] B"), list.children.map { it.text })

        items.value = listOf("Only")
        checked.value = emptySet()
        session.focusManager.requestFocus(list.focusId)
        session.applySnapshotAndAwaitIdle()

        assertSame(list, session.root.children.single())
        assertEquals(1, list.children.size)
        assertEquals("▶ [ ] Only", list.children.single().text)
        assertTrue(list.onKeyEvent!!.invoke(KeyEvent(' ', Key.CHAR)))
        assertEquals(listOf(setOf(0)), requested)

        session.dispose()
    }
}

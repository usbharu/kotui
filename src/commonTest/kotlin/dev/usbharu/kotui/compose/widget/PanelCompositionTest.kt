package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.mutableStateOf
import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelCompositionTest {

    @Test
    fun panelSetsBorderAndTitle() = runBlocking {
        val session = composeWithDefaults {
            Panel("my panel") {
                Text("content")
            }
        }

        session.awaitIdle()

        val panel = session.root.children.single()
        assertEquals("Panel", panel.tag)
        assertEquals(LayoutPolicy.COLUMN, panel.layoutPolicy)
        assertTrue(panel.drawBorder)
        assertEquals("my panel", panel.borderTitle)
        assertEquals(1, panel.children.size)
        assertEquals("Text", panel.children[0].tag)

        session.dispose()
    }

    @Test
    fun panelRecomposesTitleAndContent() = runBlocking {
        val title = mutableStateOf("one")
        val showSecond = mutableStateOf(false)
        val session = composeWithDefaults {
            Panel(title.value) {
                Text("first")
                if (showSecond.value) Text("second")
            }
        }

        session.awaitIdle()
        val panel = session.root.children.single()
        assertEquals("one", panel.borderTitle)
        assertEquals(1, panel.children.size)

        title.value = "two"
        showSecond.value = true
        session.applySnapshotAndAwaitIdle()

        assertEquals("two", panel.borderTitle)
        assertEquals(listOf("first", "second"), panel.children.map { it.text })

        session.dispose()
    }

    @Test
    fun modalSetsFocusScopeAndHighZIndex() = runBlocking {
        val session = composeWithDefaults {
            Modal("dialog") {
                Text("modal content")
            }
        }

        session.awaitIdle()

        val modal = session.root.children.single()
        assertEquals("Panel", modal.tag)
        assertEquals(LayoutPolicy.COLUMN, modal.layoutPolicy)
        assertTrue(modal.drawBorder)
        assertEquals("dialog", modal.borderTitle)
        assertTrue(modal.focusScope)
        assertTrue(modal.zIndex >= 10)
        assertEquals(1, modal.children.size)
        assertEquals("modal content", modal.children.single().text)

        session.dispose()
    }
}

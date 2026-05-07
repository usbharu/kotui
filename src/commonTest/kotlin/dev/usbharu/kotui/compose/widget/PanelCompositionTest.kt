package dev.usbharu.kotui.compose.widget

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

        session.dispose()
    }
}

package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextAreaCompositionTest {

    @Test
    fun textAreaEmitsColumnWithTextChildrenPerLine() = runBlocking {
        val session = composeWithDefaults {
            TextArea(value = "a\nb", onValueChange = {})
        }

        session.awaitIdle()

        val area = session.root.children.single()
        assertEquals("TextArea", area.tag)
        assertEquals(LayoutPolicy.COLUMN, area.layoutPolicy)
        assertTrue(area.focusable)
        assertNotNull(area.onKeyEvent)
        assertNotNull(area.onPaste)

        assertEquals(3, area.children.size)
        assertEquals("Text", area.children[0].tag)
        assertEquals("Text", area.children[1].tag)
        assertEquals("Spacer", area.children[2].tag)

        session.dispose()
    }
}

package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextInputCompositionTest {

    @Test
    fun textInputIsFocusableWithCursorAndKeyHandler() = runTest {
        val session = composeWithDefaults {
            TextInput(value = "hello", onValueChange = {}, placeholder = "name")
        }

        session.awaitIdle()

        val input = session.root.children.single()
        assertEquals("TextInput", input.tag)
        assertEquals(LayoutPolicy.COLUMN, input.layoutPolicy)
        assertEquals(1, input.preferredHeight)
        val body = input.children.single()
        assertEquals("TextInputBody", body.tag)
        assertEquals(LayoutPolicy.LEAF, body.layoutPolicy)
        assertTrue(body.focusable)
        assertNotNull(body.onKeyEvent)
        assertNotNull(body.onPaste)
        assertNotNull(body.text)

        session.dispose()
    }
}

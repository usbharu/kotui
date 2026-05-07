package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextInputCompositionTest {

    @Test
    fun textInputIsFocusableWithCursorAndKeyHandler() = runBlocking {
        val session = composeWithDefaults {
            TextInput(value = "hello", onValueChange = {}, placeholder = "name")
        }

        session.awaitIdle()

        val input = session.root.children.single()
        assertEquals("TextInput", input.tag)
        assertEquals(LayoutPolicy.LEAF, input.layoutPolicy)
        assertEquals(1, input.preferredHeight)
        assertTrue(input.focusable)
        assertNotNull(input.onKeyEvent)
        assertNotNull(input.onPaste)
        assertNotNull(input.text)

        session.dispose()
    }
}

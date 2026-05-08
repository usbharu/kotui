package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.width
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

    @Test
    fun invalidBracketedPasteIsConsumedWithoutChangingValue() = runTest {
        var value = "12"
        val session = composeWithDefaults {
            TextInput(
                value = value,
                onValueChange = { value = it },
                inputValidator = TextInputValidator.AsciiDigitsOnly,
            )
        }

        session.awaitIdle()

        val body = session.root.children.single().children.single()
        assertEquals(true, body.onPaste?.invoke("a3"))
        assertEquals("12", value)

        session.dispose()
    }

    @Test
    fun intrinsicWidthFromLongValueIsClamped() = runTest {
        val session = composeWithDefaults {
            TextInput(value = "x".repeat(400), onValueChange = {})
        }

        session.awaitIdle()

        assertEquals(256, session.root.children.single().preferredWidth)

        session.dispose()
    }

    @Test
    fun explicitModifierWidthOverridesIntrinsicWidthClamp() = runTest {
        val session = composeWithDefaults {
            TextInput(
                value = "x".repeat(400),
                onValueChange = {},
                modifier = Modifier.width(40),
            )
        }

        session.awaitIdle()

        assertEquals(40, session.root.children.single().preferredWidth)

        session.dispose()
    }
}

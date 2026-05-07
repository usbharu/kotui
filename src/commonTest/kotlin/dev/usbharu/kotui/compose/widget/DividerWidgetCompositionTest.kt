package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.height
import dev.usbharu.kotui.compose.modifier.size
import dev.usbharu.kotui.compose.modifier.width
import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DividerWidgetCompositionTest {

    @Test
    fun dividerSetsFillCharAndPreferredSize() = runBlocking {
        val session = composeWithDefaults {
            Divider('=')
        }

        session.awaitIdle()

        val divider = session.root.children.single()
        assertEquals("Divider", divider.tag)
        assertEquals(LayoutPolicy.LEAF, divider.layoutPolicy)
        assertEquals(1, divider.preferredHeight)
        assertEquals('=', divider.fillChar)

        session.dispose()
    }

    @Test
    fun dividerModifierCanOverrideDefaultHeightAndWidth() = runBlocking {
        val session = composeWithDefaults {
            Divider('-', Modifier.size(20, 2))
        }

        session.awaitIdle()

        val divider = session.root.children.single()
        assertEquals(20, divider.preferredWidth)
        assertEquals(2, divider.preferredHeight)
        assertEquals('-', divider.fillChar)

        session.dispose()
    }

    @Test
    fun verticalDividerSetsFillCharAndPreferredWidth() = runBlocking {
        val session = composeWithDefaults {
            VerticalDivider('|')
        }

        session.awaitIdle()

        val vd = session.root.children.single()
        assertEquals("VerticalDivider", vd.tag)
        assertEquals(LayoutPolicy.LEAF, vd.layoutPolicy)
        assertEquals(1, vd.preferredWidth)
        assertEquals('|', vd.fillChar)

        session.dispose()
    }

    @Test
    fun verticalDividerModifierCanOverrideWidthAndSetHeight() = runBlocking {
        val session = composeWithDefaults {
            VerticalDivider('|', Modifier.width(3).height(4))
        }

        session.awaitIdle()

        val divider = session.root.children.single()
        assertEquals(3, divider.preferredWidth)
        assertEquals(4, divider.preferredHeight)

        session.dispose()
    }

    @Test
    fun spacerHasNoTextContent() = runBlocking {
        val session = composeWithDefaults {
            Spacer(Modifier.size(2, 1))
        }

        session.awaitIdle()

        val spacer = session.root.children.single()
        assertEquals("Spacer", spacer.tag)
        assertEquals(LayoutPolicy.LEAF, spacer.layoutPolicy)
        assertNull(spacer.text)
        assertEquals(2, spacer.preferredWidth)
        assertEquals(1, spacer.preferredHeight)

        session.dispose()
    }

    @Test
    fun spacerDefaultsHaveNoIntrinsicSize() = runBlocking {
        val session = composeWithDefaults {
            Spacer()
        }

        session.awaitIdle()

        val spacer = session.root.children.single()
        assertEquals("Spacer", spacer.tag)
        assertNull(spacer.preferredWidth)
        assertNull(spacer.preferredHeight)
        assertNull(spacer.text)
        assertNull(spacer.fillChar)

        session.dispose()
    }
}

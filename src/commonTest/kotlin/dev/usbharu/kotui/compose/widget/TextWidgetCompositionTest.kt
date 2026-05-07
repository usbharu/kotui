package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.mutableStateOf
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.width
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TextWidgetCompositionTest {

    @Test
    fun textSetsTagContentAndPreferredSize() = runBlocking {
        val session = composeWithDefaults {
            Text("hello")
        }

        session.awaitIdle()

        val text = session.root.children.single()
        assertEquals("Text", text.tag)
        assertEquals(LayoutPolicy.LEAF, text.layoutPolicy)
        assertEquals("hello", text.text)
        assertEquals(1, text.preferredHeight)

        session.dispose()
    }

    @Test
    fun textRecomposesContentAndAppliesModifierWidth() = runBlocking {
        val value = mutableStateOf("first")
        val session = composeWithDefaults {
            Text(value.value, Modifier.width(12))
        }

        session.awaitIdle()
        val text = session.root.children.single()
        assertEquals("first", text.text)
        assertEquals(12, text.preferredWidth)

        value.value = "second"
        session.applySnapshotAndAwaitIdle()

        assertEquals("second", text.text)
        assertEquals(12, text.preferredWidth)

        session.dispose()
    }

    @Test
    fun badgeSetsStyledTextWithPadding() = runBlocking {
        val session = composeWithDefaults {
            Badge("ok")
        }

        session.awaitIdle()

        val badge = session.root.children.single()
        assertEquals("Badge", badge.tag)
        assertEquals(LayoutPolicy.LEAF, badge.layoutPolicy)
        assertEquals(1, badge.preferredHeight)
        assertEquals(" ok ", badge.text)
        assertNotNull(badge.preferredWidth)
        assertEquals(4, badge.preferredWidth)

        val expectedStyle = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_BRIGHT_WHITE, bold = true)
        assertEquals(expectedStyle, badge.style)

        session.dispose()
    }

    @Test
    fun badgePreferredWidthUsesDisplayWidthForWideCharacters() = runBlocking {
        val session = composeWithDefaults {
            Badge("界")
        }

        session.awaitIdle()

        val badge = session.root.children.single()
        assertEquals(" 界 ", badge.text)
        assertEquals(4, badge.preferredWidth)

        session.dispose()
    }
}

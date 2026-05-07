package dev.usbharu.kotui.compose.widget

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
}

package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProgressBarCompositionTest {

    @Test
    fun progressBarShowsFilledAndEmptyChars() = runBlocking {
        val session = composeWithDefaults {
            ProgressBar(0.25f, width = 4, showPercent = true, filledChar = '#', emptyChar = '.')
        }

        session.awaitIdle()

        val bar = session.root.children.single()
        assertEquals("ProgressBar", bar.tag)
        assertEquals(LayoutPolicy.LEAF, bar.layoutPolicy)
        assertEquals(1, bar.preferredHeight)
        assertNotNull(bar.text)
        assertEquals("[#...] 25%", bar.text)
        assertNotNull(bar.preferredWidth)

        session.dispose()
    }

    @Test
    fun progressBarAtFullShowsAllFilled() = runBlocking {
        val session = composeWithDefaults {
            ProgressBar(1.0f, width = 3, showPercent = false, filledChar = 'X', emptyChar = 'O')
        }

        session.awaitIdle()

        val bar = session.root.children.single()
        assertEquals("[XXX]", bar.text)

        session.dispose()
    }

    @Test
    fun progressBarAtZeroShowsAllEmpty() = runBlocking {
        val session = composeWithDefaults {
            ProgressBar(0.0f, width = 3, showPercent = false, filledChar = 'X', emptyChar = 'O')
        }

        session.awaitIdle()

        val bar = session.root.children.single()
        assertEquals("[OOO]", bar.text)

        session.dispose()
    }
}

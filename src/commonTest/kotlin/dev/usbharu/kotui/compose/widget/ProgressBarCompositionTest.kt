package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.mutableStateOf
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
    fun progressBarHandlesZeroWidthWithPercent() = runBlocking {
        val session = composeWithDefaults {
            ProgressBar(0.75f, width = 0, showPercent = true, filledChar = '#', emptyChar = '.')
        }

        session.awaitIdle()

        val bar = session.root.children.single()
        assertEquals("[] 75%", bar.text)
        assertEquals(6, bar.preferredWidth)

        session.dispose()
    }

    @Test
    fun progressBarRecomposesProgressText() = runBlocking {
        val progress = mutableStateOf(0.0f)
        val session = composeWithDefaults {
            ProgressBar(progress.value, width = 4, showPercent = false, filledChar = '#', emptyChar = '.')
        }

        session.awaitIdle()
        val bar = session.root.children.single()
        assertEquals("[....]", bar.text)

        progress.value = 0.5f
        session.applySnapshotAndAwaitIdle()

        assertEquals("[##..]", bar.text)
        assertEquals(6, bar.preferredWidth)

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

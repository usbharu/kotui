package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SpinnerCompositionTest {

    @Test
    fun spinnerWithManualFrameSetsDisplayText() = runBlocking {
        val session = composeWithDefaults {
            Spinner(frame = 0, chars = listOf('|', '/', '-', '\\'))
        }

        session.awaitIdle()

        val spinner = session.root.children.single()
        assertEquals("Spinner", spinner.tag)
        assertEquals(LayoutPolicy.LEAF, spinner.layoutPolicy)
        assertEquals(1, spinner.preferredHeight)
        assertEquals(1, spinner.preferredWidth)
        assertNotNull(spinner.text)
        assertEquals("|", spinner.text)

        session.dispose()
    }

    @Test
    fun spinnerWithNegativeFrameWraps() = runBlocking {
        val session = composeWithDefaults {
            Spinner(frame = -1, chars = listOf('|', '/'))
        }

        session.awaitIdle()

        val spinner = session.root.children.single()
        assertNotNull(spinner.text)
        assertEquals("/", spinner.text)

        session.dispose()
    }
}

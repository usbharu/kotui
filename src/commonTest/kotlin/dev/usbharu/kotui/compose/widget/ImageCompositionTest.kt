package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImageCompositionTest {

    @Test
    fun imageSetsDimensionsAndTerminalImage() = runTest {
        val img = TerminalImage(
            rgba = ByteArray(4),
            pixelWidth = 1,
            pixelHeight = 1,
            fallbackText = "img",
        )
        val session = composeWithDefaults {
            Image(img)
        }

        session.awaitIdle()

        val node = session.root.children.single()
        assertEquals("Image", node.tag)
        assertEquals(LayoutPolicy.LEAF, node.layoutPolicy)
        assertNotNull(node.image)
        assertEquals(img.cellWidth, node.preferredWidth)
        assertEquals(img.cellHeight, node.preferredHeight)

        session.dispose()
    }
}

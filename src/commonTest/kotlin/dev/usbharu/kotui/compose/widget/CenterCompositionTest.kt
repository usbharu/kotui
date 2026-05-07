package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CenterCompositionTest {

    @Test
    fun centerSetsLayoutPolicyAndPreservesChild() = runTest {
        val session = composeWithDefaults {
            Center {
                Text("centered")
            }
        }

        session.awaitIdle()

        val center = session.root.children.single()
        assertEquals("Center", center.tag)
        assertEquals(LayoutPolicy.CENTER, center.layoutPolicy)
        assertEquals(1, center.children.size)
        assertEquals("Text", center.children[0].tag)
        assertEquals("centered", center.children[0].text)

        session.dispose()
    }
}

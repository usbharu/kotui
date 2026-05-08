package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BoxCompositionTest {

    @Test
    fun boxSetsLayoutPolicyAndPreservesChildren() = runTest {
        val session = composeWithDefaults {
            Box {
                Text("a")
                Text("b")
            }
        }

        session.awaitIdle()

        val box = session.root.children.single()
        assertEquals("Box", box.tag)
        assertEquals(LayoutPolicy.BOX, box.layoutPolicy)
        assertEquals(2, box.children.size)
        assertEquals("Text", box.children[0].tag)
        assertEquals("Text", box.children[1].tag)

        session.dispose()
    }
}

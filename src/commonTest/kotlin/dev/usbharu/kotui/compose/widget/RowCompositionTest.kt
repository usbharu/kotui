package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RowCompositionTest {

    @Test
    fun rowSetsLayoutPolicyAndProperties() = runTest {
        val session = composeWithDefaults {
            Row(
                gap = 5,
                justifyContent = JustifyContent.End,
                alignItems = AlignItems.Center,
            ) {
                Text("a")
                Text("b")
                Text("c")
            }
        }

        session.awaitIdle()

        val row = session.root.children.single()
        assertEquals("Row", row.tag)
        assertEquals(LayoutPolicy.ROW, row.layoutPolicy)
        assertEquals(5, row.layoutGap)
        assertEquals(JustifyContent.End, row.justifyContent)
        assertEquals(AlignItems.Center, row.alignItems)
        assertEquals(3, row.children.size)

        session.dispose()
    }

    @Test
    fun rowDefaultGapIsTwo() = runTest {
        val session = composeWithDefaults {
            Row {
                Text("x")
            }
        }

        session.awaitIdle()

        val row = session.root.children.single()
        assertEquals(2, row.layoutGap)
        assertEquals(JustifyContent.Start, row.justifyContent)
        assertEquals(AlignItems.Stretch, row.alignItems)

        session.dispose()
    }
}

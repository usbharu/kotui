package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.node.LayoutPolicy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnCompositionTest {

    @Test
    fun columnSetsLayoutPolicyAndProperties() = runBlocking {
        val session = composeWithDefaults {
            Column(
                gap = 2,
                justifyContent = JustifyContent.Center,
                alignItems = AlignItems.End,
            ) {
                Text("a")
                Text("b")
            }
        }

        session.awaitIdle()

        val column = session.root.children.single()
        assertEquals("Column", column.tag)
        assertEquals(LayoutPolicy.COLUMN, column.layoutPolicy)
        assertEquals(2, column.layoutGap)
        assertEquals(JustifyContent.Center, column.justifyContent)
        assertEquals(AlignItems.End, column.alignItems)
        assertEquals(2, column.children.size)
        assertEquals("Text", column.children[0].tag)
        assertEquals("Text", column.children[1].tag)

        session.dispose()
    }

    @Test
    fun columnDefaultsAreCorrect() = runBlocking {
        val session = composeWithDefaults {
            Column {
                Text("x")
            }
        }

        session.awaitIdle()

        val column = session.root.children.single()
        assertEquals(0, column.layoutGap)
        assertEquals(JustifyContent.Start, column.justifyContent)
        assertEquals(AlignItems.Stretch, column.alignItems)

        session.dispose()
    }
}

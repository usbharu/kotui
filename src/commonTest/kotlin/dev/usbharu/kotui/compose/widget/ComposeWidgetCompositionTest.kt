package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.InMemoryClipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.size
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.Recomposer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposeWidgetCompositionTest {
    @Test
    fun composeWidgetsBuildExpectedNodeTree() = runBlocking {
        val root = TuiNode("Root")
        val recomposer = Recomposer(coroutineContext)
        val job = launch { recomposer.runRecomposeAndApplyChanges() }
        val composition = Composition(TuiApplier(root), recomposer)

        composition.setContent {
            CompositionLocalProvider(
                LocalFocusManager provides FocusManager(),
                LocalClipboard provides InMemoryClipboard(),
            ) {
                Column(gap = 1, justifyContent = JustifyContent.Center, alignItems = AlignItems.Center) {
                    Row(gap = 3, justifyContent = JustifyContent.End, alignItems = AlignItems.End) {
                        Text("hello")
                        Badge("ok")
                        Divider('=')
                        VerticalDivider('|')
                        Spacer(Modifier.size(2, 1))
                    }
                    Box {
                        Center(Modifier.size(8, 3)) {
                            Panel("panel") {
                                ProgressBar(0.25f, width = 4, showPercent = true, filledChar = '#', emptyChar = '.')
                            }
                        }
                    }
                    Modal("modal") {
                        Button("Go")
                        Checkbox(checked = true, label = "Done", onCheckedChange = {})
                        RadioGroup(options = listOf("A", "B"), selected = "A", onSelectedChange = {})
                        Select(items = listOf("A", "B"), selected = "A", onSelectedChange = {})
                        SelectableList(items = listOf("A", "B"), selectedIndex = 0, onSelectedIndexChange = {})
                        MultiSelectList(
                            items = listOf("A", "B"),
                            cursorIndex = 0,
                            onCursorIndexChange = {},
                            checkedIndices = setOf(1),
                            onCheckedIndicesChange = {},
                        )
                        Spinner(frame = -1, chars = listOf('|', '/'))
                        TextInput(value = "", onValueChange = {}, placeholder = "name")
                        TextArea(value = "a\n", onValueChange = {})
                    }
                    Image(TerminalImage(ByteArray(4), pixelWidth = 1, pixelHeight = 1, fallbackText = "img"))
                }
            }
        }

        recomposer.awaitIdle()

        val column = root.children.single()
        assertEquals(LayoutPolicy.COLUMN, column.layoutPolicy)
        assertEquals(1, column.layoutGap)
        assertEquals(JustifyContent.Center, column.justifyContent)
        assertEquals(AlignItems.Center, column.alignItems)

        val row = column.children[0]
        assertEquals(LayoutPolicy.ROW, row.layoutPolicy)
        assertEquals(3, row.layoutGap)
        assertEquals("Text", row.children[0].tag)
        assertEquals("hello", row.children[0].text)
        assertEquals(" ok ", row.children[1].text)
        assertEquals('=', row.children[2].fillChar)
        assertEquals('|', row.children[3].fillChar)

        val modal = column.children[2]
        assertEquals("Panel", modal.tag)
        assertEquals("modal", modal.borderTitle)
        assertTrue(modal.focusScope)
        assertTrue(modal.zIndex >= 10)
        assertNotNull(modal.children.firstOrNull { it.tag == "Button" }?.onKeyEvent)

        val image = column.children.last()
        assertEquals("Image", image.tag)
        assertEquals(1, image.preferredWidth)
        assertEquals(1, image.preferredHeight)

        composition.dispose()
        recomposer.close()
        job.cancel()
    }
}

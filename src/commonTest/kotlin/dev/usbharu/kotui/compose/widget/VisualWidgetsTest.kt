package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.layout.LayoutEngine
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals

class VisualWidgetsTest {

    @Test
    fun progressBarRendersHalfFilledWithPercent() {
        val text = buildProgressText(
            progress = 0.5f,
            width = 10,
            showPercent = true,
            filledChar = '█',
            emptyChar = '░',
        )
        assertEquals("[█████░░░░░] 50%", text)
    }

    @Test
    fun progressBarClampsOverflowToFull() {
        val text = buildProgressText(
            progress = 2f,
            width = 4,
            showPercent = false,
            filledChar = '#',
            emptyChar = '.',
        )
        assertEquals("[####]", text)
    }

    @Test
    fun progressBarClampsNegativeToEmpty() {
        val text = buildProgressText(
            progress = -1f,
            width = 4,
            showPercent = false,
            filledChar = '#',
            emptyChar = '.',
        )
        assertEquals("[....]", text)
    }

    @Test
    fun spacerSizedNodeKeepsItsBoundsInsideColumn() {
        val column = TuiNode("Column").apply {
            layoutPolicy = LayoutPolicy.COLUMN
        }
        val spacer = TuiNode("Spacer").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredWidth = 5
            preferredHeight = 2
        }
        column.insertAt(0, spacer)

        LayoutEngine.layout(column, screenWidth = 40, screenHeight = 10)

        assertEquals(5, spacer.bounds.width)
        assertEquals(2, spacer.bounds.height)
    }

    @Test
    fun centerPlacesChildAtMiddleOfParentBounds() {
        val center = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            preferredWidth = 20
            preferredHeight = 6
        }
        val child = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredWidth = 4
            preferredHeight = 2
        }
        center.insertAt(0, child)

        // Place center inside a Box-like parent so layoutEngine sets bounds.
        val root = TuiNode("Root").apply {
            layoutPolicy = LayoutPolicy.BOX
        }
        root.insertAt(0, center)

        LayoutEngine.layout(root, screenWidth = 30, screenHeight = 10)

        // center.bounds should be (0,0,20,6) inside the root box.
        assertEquals(20, center.bounds.width)
        assertEquals(6, center.bounds.height)
        // Child centered: x = (20-4)/2 = 8, y = (6-2)/2 = 2
        assertEquals(8, child.bounds.x)
        assertEquals(2, child.bounds.y)
        assertEquals(4, child.bounds.width)
        assertEquals(2, child.bounds.height)
    }

    @Test
    fun centerClampsChildToParentWhenChildLarger() {
        val center = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            preferredWidth = 8
            preferredHeight = 4
        }
        val child = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredWidth = 100
            preferredHeight = 100
        }
        center.insertAt(0, child)

        val root = TuiNode("Root").apply { layoutPolicy = LayoutPolicy.BOX }
        root.insertAt(0, center)

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 10)

        assertEquals(8, child.bounds.width)
        assertEquals(4, child.bounds.height)
        assertEquals(0, child.bounds.x)
        assertEquals(0, child.bounds.y)
    }

    @Test
    fun verticalDividerFillsRowHeightInferredFromSibling() {
        // Row inside Column, no explicit Row height. Row should infer its height
        // from the tallest child (the Center, height 5), and the VerticalDivider
        // should fill that height without needing Modifier.height().
        val column = TuiNode("Column").apply { layoutPolicy = LayoutPolicy.COLUMN }
        val row = TuiNode("Row").apply { layoutPolicy = LayoutPolicy.ROW; layoutGap = 0 }
        val left = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            preferredWidth = 10
            preferredHeight = 5
        }
        val divider = TuiNode("VerticalDivider").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredWidth = 1
            fillChar = '│'
        }
        val right = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            preferredWidth = 10
            preferredHeight = 5
        }
        row.insertAt(0, left)
        row.insertAt(1, divider)
        row.insertAt(2, right)
        column.insertAt(0, row)

        LayoutEngine.layout(column, screenWidth = 30, screenHeight = 10)

        assertEquals(5, divider.bounds.height)
        assertEquals(1, divider.bounds.width)
    }

    @Test
    fun fillCharNodeKeepsParentDrivenWidthInsideColumn() {
        // Mimics what Divider does: a LEAF node with preferredHeight = 1 and no
        // preferredWidth gets the column's full available width.
        val column = TuiNode("Column").apply { layoutPolicy = LayoutPolicy.COLUMN }
        val divider = TuiNode("Divider").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredHeight = 1
            fillChar = '─'
        }
        column.insertAt(0, divider)

        LayoutEngine.layout(column, screenWidth = 30, screenHeight = 5)

        assertEquals(30, divider.bounds.width)
        assertEquals(1, divider.bounds.height)
    }
}

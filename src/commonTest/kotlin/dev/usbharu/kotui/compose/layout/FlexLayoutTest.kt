package dev.usbharu.kotui.compose.layout

import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals

class FlexLayoutTest {

    private fun leaf(width: Int? = null, height: Int? = null, grow: Float = 0f, basis: Int? = null): TuiNode =
        TuiNode("Leaf").apply {
            layoutPolicy = LayoutPolicy.LEAF
            preferredWidth = width
            preferredHeight = height
            flexGrow = grow
            flexBasis = basis
        }

    private fun row(
        justify: JustifyContent = JustifyContent.Start,
        align: AlignItems = AlignItems.Start,
        gap: Int = 0,
        children: List<TuiNode>,
    ): TuiNode = TuiNode("Row").apply {
        layoutPolicy = LayoutPolicy.ROW
        justifyContent = justify
        alignItems = align
        layoutGap = gap
        preferredHeight = 3
        children.forEachIndexed { i, c -> insertAt(i, c) }
    }

    private fun column(
        justify: JustifyContent = JustifyContent.Start,
        align: AlignItems = AlignItems.Start,
        gap: Int = 0,
        children: List<TuiNode>,
    ): TuiNode = TuiNode("Column").apply {
        layoutPolicy = LayoutPolicy.COLUMN
        justifyContent = justify
        alignItems = align
        layoutGap = gap
        preferredWidth = 10
        children.forEachIndexed { i, c -> insertAt(i, c) }
    }

    @Test
    fun rowWeightDistributesFreeSpaceProportionally() {
        val a = leaf(height = 1, grow = 1f)
        val b = leaf(height = 1, grow = 2f)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 30, screenHeight = 3)

        assertEquals(0, a.bounds.x)
        assertEquals(10, a.bounds.width)
        assertEquals(10, b.bounds.x)
        assertEquals(20, b.bounds.width)
    }

    @Test
    fun rowFlexBasisSetsStartingSize() {
        val a = leaf(height = 1, basis = 4, grow = 1f)
        val b = leaf(height = 1, basis = 6, grow = 1f)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        // basis 4 + 6 = 10. Remaining 10 split 1:1 → each +5.
        assertEquals(9, a.bounds.width)
        assertEquals(11, b.bounds.width)
    }

    @Test
    fun rowJustifyContentEndPacksRight() {
        val a = leaf(width = 4, height = 1)
        val b = leaf(width = 6, height = 1)
        val root = row(justify = JustifyContent.End, children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        assertEquals(10, a.bounds.x)
        assertEquals(14, b.bounds.x)
    }

    @Test
    fun rowJustifyContentCenterCentersItems() {
        val a = leaf(width = 4, height = 1)
        val b = leaf(width = 4, height = 1)
        val root = row(justify = JustifyContent.Center, children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        // Free = 12 → center starts at 6.
        assertEquals(6, a.bounds.x)
        assertEquals(10, b.bounds.x)
    }

    @Test
    fun rowSpaceBetweenSeparatesItems() {
        val a = leaf(width = 4, height = 1)
        val b = leaf(width = 4, height = 1)
        val c = leaf(width = 4, height = 1)
        val root = row(justify = JustifyContent.SpaceBetween, children = listOf(a, b, c))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        // 20 - 12 = 8 spread between 2 gaps → 4 each.
        assertEquals(0, a.bounds.x)
        assertEquals(8, b.bounds.x)
        assertEquals(16, c.bounds.x)
    }

    @Test
    fun rowSpaceEvenlyDistributesAllGaps() {
        val a = leaf(width = 4, height = 1)
        val b = leaf(width = 4, height = 1)
        val root = row(justify = JustifyContent.SpaceEvenly, children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        // Free 12 / (2+1) = 4 slot each side.
        assertEquals(4, a.bounds.x)
        assertEquals(12, b.bounds.x)
    }

    @Test
    fun rowAlignItemsStretchMakesChildrenFillCross() {
        val a = leaf(width = 4)
        val root = row(align = AlignItems.Stretch, children = listOf(a))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(3, a.bounds.height)
        assertEquals(0, a.bounds.y)
    }

    @Test
    fun rowAlignItemsCenterCentersChildrenOnCross() {
        val a = leaf(width = 4, height = 1)
        val root = row(align = AlignItems.Center, children = listOf(a))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        // avail=3, child h=1 → cy = 1.
        assertEquals(1, a.bounds.y)
    }

    @Test
    fun rowAlignItemsEndPlacesChildrenAtBottom() {
        val a = leaf(width = 4, height = 1)
        val root = row(align = AlignItems.End, children = listOf(a))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(2, a.bounds.y)
    }

    @Test
    fun columnWeightDistributesHeight() {
        val a = leaf(width = 6, grow = 1f)
        val b = leaf(width = 6, grow = 3f)
        val root = column(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 20)

        // No intrinsic height (no text/fill) → basis=0 for both. Remaining=20, 1:3 → 5/15.
        assertEquals(5, a.bounds.height)
        assertEquals(5, b.bounds.y)
        assertEquals(15, b.bounds.height)
    }

    @Test
    fun columnAlignItemsStretchExpandsWidth() {
        val root = TuiNode("Column").apply {
            layoutPolicy = LayoutPolicy.COLUMN
            alignItems = AlignItems.Stretch
            preferredWidth = 12
            preferredHeight = 10
        }
        val a = leaf(height = 2)
        root.insertAt(0, a)

        LayoutEngine.layout(root, 12, 10)

        assertEquals(12, a.bounds.width)
        assertEquals(0, a.bounds.x)
    }

    @Test
    fun rowOverflowClampsToZeroExtraWithoutNegative() {
        val a = leaf(width = 15, height = 1)
        val b = leaf(width = 15, height = 1, grow = 1f)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        // 30 > 10 → no grow distribution, widths stay at basis.
        assertEquals(15, a.bounds.width)
        assertEquals(15, b.bounds.width)
    }

    @Test
    fun emptyRowKeepsRootBounds() {
        val root = row(children = emptyList())

        LayoutEngine.layout(root, screenWidth = 12, screenHeight = 3)

        assertEquals(12, root.bounds.width)
        assertEquals(3, root.bounds.height)
    }

    @Test
    fun columnWithBorderInsetsChildrenAndShrinksToContentWhenHeightIsInferred() {
        val child = leaf(height = 2)
        val root = TuiNode("Panel").apply {
            layoutPolicy = LayoutPolicy.COLUMN
            drawBorder = true
            preferredWidth = 10
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 8)

        assertEquals(1, child.bounds.x)
        assertEquals(1, child.bounds.y)
        assertEquals(8, child.bounds.width)
        assertEquals(4, root.bounds.height)
    }

    @Test
    fun rowSpaceAroundAddsHalfSlotBeforeFirstChild() {
        val a = leaf(width = 2, height = 1)
        val b = leaf(width = 2, height = 1)
        val root = row(justify = JustifyContent.SpaceAround, children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 12, screenHeight = 3)

        assertEquals(2, a.bounds.x)
        assertEquals(8, b.bounds.x)
    }

    @Test
    fun singleChildSpaceBetweenDoesNotMoveChild() {
        val a = leaf(width = 3, height = 1)
        val root = row(justify = JustifyContent.SpaceBetween, children = listOf(a))

        LayoutEngine.layout(root, screenWidth = 12, screenHeight = 3)

        assertEquals(0, a.bounds.x)
    }

    @Test
    fun boxKeepsExistingChildOffsetAndAppliesPreferredSize() {
        val child = leaf(width = 3, height = 2).apply {
            bounds = dev.usbharu.kotui.core.Rect(4, 5, 99, 99)
        }
        val root = TuiNode("Box").apply {
            layoutPolicy = LayoutPolicy.BOX
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 10)

        assertEquals(4, child.bounds.x)
        assertEquals(5, child.bounds.y)
        assertEquals(3, child.bounds.width)
        assertEquals(2, child.bounds.height)
    }

    @Test
    fun centerDefaultsChildHeightToOneWhenPreferredHeightIsMissing() {
        val child = leaf(width = 4)
        val root = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 5)

        assertEquals(3, child.bounds.x)
        assertEquals(2, child.bounds.y)
        assertEquals(4, child.bounds.width)
        assertEquals(1, child.bounds.height)
    }

    @Test
    fun zeroSizedConstraintsDoNotProduceNegativeChildBounds() {
        val child = leaf(height = 2)
        val root = TuiNode("Column").apply {
            layoutPolicy = LayoutPolicy.COLUMN
            drawBorder = true
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 1, screenHeight = 1)

        assertEquals(0, child.bounds.width)
        assertEquals(2, child.bounds.height)
    }
}

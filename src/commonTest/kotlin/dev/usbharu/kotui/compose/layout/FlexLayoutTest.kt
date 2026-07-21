package dev.usbharu.kotui.compose.layout

import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.offset

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
    fun spaceEvenlyDistributesIntegerRemainderInsteadOfLeavingItAtEnd() {
        val a = leaf(width = 1, height = 1)
        val b = leaf(width = 1, height = 1)
        val root = row(justify = JustifyContent.SpaceEvenly, children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        val left = a.bounds.x
        val middle = b.bounds.x - (a.bounds.x + a.bounds.width)
        val right = 10 - (b.bounds.x + b.bounds.width)
        assertTrue(maxOf(left, middle, right) - minOf(left, middle, right) <= 1)
    }

    @Test
    fun aggregateBasisAndGapArithmeticDoesNotWrapNegative() {
        val children = listOf(
            leaf(width = Int.MAX_VALUE, height = 1),
            leaf(width = Int.MAX_VALUE, height = 1),
            leaf(width = 1, height = 1),
        )
        val root = row(gap = Int.MAX_VALUE, children = children)

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(0, children[0].bounds.x)
        assertEquals(Int.MAX_VALUE, children[1].bounds.x)
        assertEquals(Int.MAX_VALUE, children[2].bounds.x)
        assertTrue(children.all { it.bounds.width >= 0 })
    }

    @Test
    fun overflowingBasisSumDoesNotCreatePhantomGrowSpace() {
        val growing = leaf(width = 0, height = 1, grow = 1f)
        val root = row(children = listOf(
            leaf(width = Int.MAX_VALUE, height = 1),
            leaf(width = Int.MAX_VALUE, height = 1),
            growing,
        ))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(0, growing.bounds.width)
    }

    @Test
    fun hugeFiniteGrowWeightsStillShareSpaceProportionally() {
        val a = leaf(height = 1, grow = Float.MAX_VALUE)
        val b = leaf(height = 1, grow = Float.MAX_VALUE)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(5, a.bounds.width)
        assertEquals(5, b.bounds.width)
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
    fun centerUsesIntrinsicChildHeightWhenPreferredHeightIsUnset() {
        val root = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
        }
        val child = TuiNode("Column").apply {
            layoutPolicy = LayoutPolicy.COLUMN
            insertAt(0, leaf(width = 2, height = 1))
            insertAt(1, leaf(width = 2, height = 1))
        }
        root.insertAt(0, child)

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 6)

        assertEquals(2, child.bounds.height)
        assertEquals(2, child.bounds.y)
        assertEquals(2, child.children[0].bounds.y)
        assertEquals(3, child.children[1].bounds.y)
    }

    @Test
    fun boxRepositionsChildWhenParentMovesBetweenLayouts() {
        val root = TuiNode("root").apply {
            layoutPolicy = LayoutPolicy.ROW
            justifyContent = JustifyContent.End
        }
        val box = TuiNode("box").apply {
            layoutPolicy = LayoutPolicy.BOX
            preferredWidth = 3
            preferredHeight = 2
        }
        val child = leaf(width = 1, height = 1)
        box.insertAt(0, child)
        root.insertAt(0, box)
        LayoutEngine.layout(root, 10, 5)
        assertEquals(7, child.bounds.x)

        LayoutEngine.layout(root, 8, 5)

        assertEquals(5, child.bounds.x)
        assertEquals(0, child.bounds.y)
    }

    @Test
    fun removingOffsetModifierReturnsBoxChildToParentOrigin() {
        val root = TuiNode("root").apply {
            layoutPolicy = LayoutPolicy.BOX
            bounds = dev.usbharu.kotui.core.Rect(2, 3, 5, 5)
        }
        val child = leaf(width = 1, height = 1)
        child.applyModifier(Modifier.offset(8, 9))
        root.insertAt(0, child)
        LayoutEngine.layout(root, 5, 5)
        assertEquals(8, child.bounds.x)

        child.applyModifier(Modifier)
        LayoutEngine.layout(root, 5, 5)

        assertEquals(0, child.bounds.x)
        assertEquals(0, child.bounds.y)
    }

    @Test
    fun layoutRejectsNegativeScreenDimensions() {
        val root = TuiNode("root")

        assertFailsWith<IllegalArgumentException> { LayoutEngine.layout(root, -1, 1) }
        assertFailsWith<IllegalArgumentException> { LayoutEngine.layout(root, 1, -1) }
    }

    @Test
    fun layoutRejectsNegativeDirectDimensionsAndSpacing() {
        val invalidNodes = listOf(
            TuiNode("width").apply { preferredWidth = -1 },
            TuiNode("height").apply { preferredHeight = -1 },
            TuiNode("gap").apply { layoutGap = -1 },
            TuiNode("basis").apply { flexBasis = -1 },
        )

        invalidNodes.forEach { node ->
            assertFailsWith<IllegalArgumentException>(node.tag) { LayoutEngine.layout(node, 1, 1) }
        }
    }

    @Test
    fun layoutRejectsNonFiniteOrNegativeDirectGrowValues() {
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { grow ->
            val root = TuiNode("root").apply { flexGrow = grow }
            assertFailsWith<IllegalArgumentException>(grow.toString()) { LayoutEngine.layout(root, 1, 1) }
        }
    }

    @Test
    fun layoutValidatesInvalidDescendantsBeforeMutatingRootBounds() {
        val root = TuiNode("root").apply { bounds = dev.usbharu.kotui.core.Rect(7, 8, 9, 10) }
        root.insertAt(0, TuiNode("bad").apply { preferredWidth = -1 })

        assertFailsWith<IllegalArgumentException> { LayoutEngine.layout(root, 2, 2) }

        assertEquals(dev.usbharu.kotui.core.Rect(7, 8, 9, 10), root.bounds)
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
    fun boxResetsStaleChildPositionAndAppliesPreferredSize() {
        val child = leaf(width = 3, height = 2).apply {
            bounds = dev.usbharu.kotui.core.Rect(4, 5, 99, 99)
        }
        val root = TuiNode("Box").apply {
            layoutPolicy = LayoutPolicy.BOX
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 10)

        assertEquals(0, child.bounds.x)
        assertEquals(0, child.bounds.y)
        assertEquals(3, child.bounds.width)
        assertEquals(2, child.bounds.height)
    }

    @Test
    fun centerKeepsLeafHeightZeroWhenPreferredHeightIsMissing() {
        val child = leaf(width = 4)
        val root = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 5)

        assertEquals(3, child.bounds.x)
        assertEquals(2, child.bounds.y)
        assertEquals(4, child.bounds.width)
        assertEquals(0, child.bounds.height)
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

    @Test
    fun emptyColumnKeepsRootBounds() {
        val root = column(children = emptyList())

        LayoutEngine.layout(root, screenWidth = 12, screenHeight = 7)

        assertEquals(12, root.bounds.width)
        assertEquals(7, root.bounds.height)
    }

    @Test
    fun rowWithNoGrowKeepsBasisWidthsAndSkipsRemainderDistribution() {
        val a = leaf(width = 2, height = 1)
        val b = leaf(width = 3, height = 1)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 12, screenHeight = 3)

        assertEquals(2, a.bounds.width)
        assertEquals(3, b.bounds.width)
        assertEquals(2, b.bounds.x)
    }

    @Test
    fun rowGrowWithoutRoundingRemainderKeepsExactShares() {
        val a = leaf(height = 1, grow = 1f)
        val b = leaf(height = 1, grow = 1f)
        val root = row(children = listOf(a, b))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(5, a.bounds.width)
        assertEquals(5, b.bounds.width)
    }

    @Test
    fun rowPreferredWidthPreventsShrinkToContent() {
        val child = leaf(width = 2, height = 1)
        val root = row(children = listOf(child)).apply { preferredWidth = 20 }

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        assertEquals(20, root.bounds.width)
    }

    @Test
    fun columnPreferredHeightPreventsShrinkToContent() {
        val child = leaf(width = 2, height = 1)
        val root = column(children = listOf(child)).apply { preferredHeight = 9 }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 9)

        assertEquals(9, root.bounds.height)
    }

    @Test
    fun columnCenterAndEndAlignmentUseIntrinsicChildWidth() {
        val centered = leaf(height = 1).apply { text = "abcd" }
        val ended = leaf(height = 1).apply { text = "xy" }
        val centerRoot = column(align = AlignItems.Center, children = listOf(centered))
        val endRoot = column(align = AlignItems.End, children = listOf(ended))

        LayoutEngine.layout(centerRoot, screenWidth = 10, screenHeight = 2)
        LayoutEngine.layout(endRoot, screenWidth = 10, screenHeight = 2)

        assertEquals(3, centered.bounds.x)
        assertEquals(4, centered.bounds.width)
        assertEquals(8, ended.bounds.x)
        assertEquals(2, ended.bounds.width)
    }

    @Test
    fun borderedRowInsetsChildrenAndShrinksWidthToContent() {
        val child = leaf(width = 2, height = 1)
        val root = TuiNode("PanelRow").apply {
            layoutPolicy = LayoutPolicy.ROW
            drawBorder = true
            preferredHeight = 3
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 3)

        assertEquals(1, child.bounds.x)
        assertEquals(1, child.bounds.y)
        assertEquals(4, root.bounds.width)
    }

    @Test
    fun centerUsesIntrinsicWidthForChildWithoutPreferredWidth() {
        val child = TuiNode("Text").apply {
            layoutPolicy = LayoutPolicy.LEAF
            text = "abc"
        }
        val root = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 9, screenHeight = 5)

        assertEquals(3, child.bounds.x)
        assertEquals(2, child.bounds.y)
        assertEquals(3, child.bounds.width)
        assertEquals(1, child.bounds.height)
    }

    @Test
    fun boxWithoutPreferredSizeFillsConstraintsForZeroBoundsChild() {
        val child = leaf()
        val root = TuiNode("Box").apply {
            layoutPolicy = LayoutPolicy.BOX
            insertAt(0, child)
        }

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 8)

        assertEquals(10, child.bounds.width)
        assertEquals(8, child.bounds.height)
    }

    @Test
    fun rowUsesNestedRowIntrinsicWidthIncludingGap() {
        val nestedA = leaf(width = 2, height = 1)
        val nestedB = leaf(width = 3, height = 1)
        val nested = TuiNode("NestedRow").apply {
            layoutPolicy = LayoutPolicy.ROW
            layoutGap = 1
            insertAt(0, nestedA)
            insertAt(1, nestedB)
        }
        val after = leaf(width = 1, height = 1)
        val root = row(children = listOf(nested, after))

        LayoutEngine.layout(root, screenWidth = 20, screenHeight = 3)

        assertEquals(6, nested.bounds.width)
        assertEquals(6, after.bounds.x)
    }

    @Test
    fun rowUsesBoxAndCenterIntrinsicHeightsForCrossSize() {
        val boxChild = leaf(width = 1, height = 4)
        val box = TuiNode("Box").apply {
            layoutPolicy = LayoutPolicy.BOX
            preferredWidth = 2
            insertAt(0, boxChild)
        }
        val centerChild = leaf(width = 1, height = 3)
        val center = TuiNode("Center").apply {
            layoutPolicy = LayoutPolicy.CENTER
            preferredWidth = 2
            insertAt(0, centerChild)
        }
        val root = row(align = AlignItems.Start, children = listOf(box, center))

        LayoutEngine.layout(root, screenWidth = 10, screenHeight = 8)

        assertEquals(4, box.bounds.height)
        assertEquals(3, center.bounds.height)
    }

}

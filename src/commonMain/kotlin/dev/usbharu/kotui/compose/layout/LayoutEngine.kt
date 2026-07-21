package dev.usbharu.kotui.compose.layout

import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.layout.Constraints
import dev.usbharu.kotui.utils.displayWidth

object LayoutEngine {
    private fun dimension(value: Long): Int = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    private fun coordinate(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    fun layout(root: TuiNode, screenWidth: Int, screenHeight: Int) {
        require(screenWidth >= 0 && screenHeight >= 0) { "screen dimensions must be non-negative" }
        validateNode(root)
        root.bounds = Rect(0, 0, screenWidth, screenHeight)
        layoutNode(root, Constraints(maxWidth = screenWidth, maxHeight = screenHeight))
    }

    private fun validateNode(node: TuiNode) {
        require(node.preferredWidth == null || node.preferredWidth!! >= 0) { "preferred width must be non-negative" }
        require(node.preferredHeight == null || node.preferredHeight!! >= 0) { "preferred height must be non-negative" }
        require(node.layoutGap >= 0) { "layout gap must be non-negative" }
        require(node.flexBasis == null || node.flexBasis!! >= 0) { "flex basis must be non-negative" }
        require(node.flexGrow.isFinite() && node.flexGrow >= 0f) { "flex grow must be finite and non-negative" }
        node.children.forEach(::validateNode)
    }

    private fun intrinsicWidth(node: TuiNode): Int {
        node.preferredWidth?.let { return it }
        node.text?.let { return it.displayWidth() }
        val inset = if (node.drawBorder) 2 else 0
        return when (node.layoutPolicy) {
            LayoutPolicy.LEAF -> 0
            LayoutPolicy.ROW -> {
                val gap = node.layoutGap
                val sum = node.children.sumOf { rowChildBasisWidth(it).toLong() } +
                    (node.children.size - 1).coerceAtLeast(0).toLong() * gap.toLong()
                dimension(sum + inset.toLong())
            }
            LayoutPolicy.COLUMN,
            LayoutPolicy.BOX,
            LayoutPolicy.CENTER -> dimension(
                (node.children.maxOfOrNull { intrinsicWidth(it) } ?: 0).toLong() + inset.toLong(),
            )
        }
    }

    private fun intrinsicHeight(node: TuiNode): Int {
        node.preferredHeight?.let { return it }
        val inset = if (node.drawBorder) 2 else 0
        return when (node.layoutPolicy) {
            LayoutPolicy.LEAF -> if (node.text != null || node.fillChar != null) 1 else 0
            LayoutPolicy.ROW -> dimension(
                (node.children.maxOfOrNull { intrinsicHeight(it) } ?: 0).toLong() + inset.toLong(),
            )
            LayoutPolicy.COLUMN -> {
                val gap = node.layoutGap
                val sum = node.children.sumOf { columnChildBasisHeight(it).toLong() } +
                    (node.children.size - 1).coerceAtLeast(0).toLong() * gap.toLong()
                dimension(sum + inset.toLong())
            }
            LayoutPolicy.BOX,
            LayoutPolicy.CENTER -> dimension(
                (node.children.maxOfOrNull { intrinsicHeight(it) } ?: 0).toLong() + inset.toLong(),
            )
        }
    }

    private fun rowChildBasisWidth(node: TuiNode): Int =
        node.flexBasis ?: intrinsicWidth(node)

    private fun columnChildBasisHeight(node: TuiNode): Int =
        node.flexBasis ?: intrinsicHeight(node)

    private fun layoutNode(node: TuiNode, constraints: Constraints) {
        when (node.layoutPolicy) {
            LayoutPolicy.COLUMN -> layoutColumn(node, constraints)
            LayoutPolicy.ROW -> layoutRow(node, constraints)
            LayoutPolicy.BOX -> layoutBox(node, constraints)
            LayoutPolicy.CENTER -> layoutCenter(node, constraints)
            LayoutPolicy.LEAF -> {}
        }
    }

    /** Distributes [remaining] cells across children proportionally to [grows]. Negative remaining → zeros. */
    private fun distributeGrow(grows: FloatArray, remaining: Int): IntArray {
        val extras = IntArray(grows.size)
        if (remaining <= 0) return extras
        var total = 0.0
        for (g in grows) if (g > 0f) total += g.toDouble()
        if (total <= 0.0) return extras
        var allocated = 0
        var lastIdx = -1
        for (i in grows.indices) {
            if (grows[i] > 0f) {
                val share = (remaining.toDouble() * (grows[i].toDouble() / total)).toInt()
                extras[i] = share
                allocated += share
                lastIdx = i
            }
        }
        // Absorb rounding remainder into the last flex child.
        if (lastIdx >= 0 && allocated < remaining) {
            extras[lastIdx] += remaining - allocated
        }
        return extras
    }

    private fun computeMainExtraOffsets(
        justify: JustifyContent,
        contentSize: Int,
        usedMain: Long,
        count: Int,
    ): IntArray {
        if (count <= 0) return IntArray(0)
        val free = (contentSize.toLong() - usedMain).coerceAtLeast(0L)
        return IntArray(count) { index ->
            val offset = when (justify) {
                JustifyContent.Start -> 0L
                JustifyContent.End -> free
                JustifyContent.Center -> free / 2L
                JustifyContent.SpaceBetween -> if (count <= 1) 0L else free * index / (count - 1)
                JustifyContent.SpaceAround -> free * (2L * index + 1L) / (2L * count)
                JustifyContent.SpaceEvenly -> free * (index + 1L) / (count + 1L)
            }
            offset.toInt()
        }
    }

    private fun crossOffset(align: AlignItems, available: Int, childSize: Int): Int {
        if (available <= 0) return 0
        return when (align) {
            AlignItems.Start, AlignItems.Stretch -> 0
            AlignItems.Center -> ((available - childSize) / 2).coerceAtLeast(0)
            AlignItems.End -> (available - childSize).coerceAtLeast(0)
        }
    }

    private fun layoutColumn(node: TuiNode, constraints: Constraints) {
        val inset = if (node.drawBorder) 1 else 0
        val baseX = coordinate(node.bounds.x.toLong() + inset)
        val baseY = coordinate(node.bounds.y.toLong() + inset)
        val availW = (constraints.maxWidth - 2 * inset).coerceAtLeast(0)
        val availH = (constraints.maxHeight - 2 * inset).coerceAtLeast(0)
        val gap = node.layoutGap
        val children = node.children
        val n = children.size

        if (n == 0) return

        val bases = IntArray(n) { columnChildBasisHeight(children[it]) }
        val grows = FloatArray(n) { children[it].flexGrow }
        val gapsTotal = (n - 1).coerceAtLeast(0).toLong() * gap.toLong()
        val basesSum = bases.sumOf { it.toLong() }
        val remaining = dimension(availH.toLong() - basesSum - gapsTotal)
        val extras = distributeGrow(grows, remaining)

        val finalHeights = IntArray(n) { bases[it] + extras[it] }
        val usedMain = finalHeights.sumOf { it.toLong() } + gapsTotal

        val extraOffsets = computeMainExtraOffsets(node.justifyContent, availH, usedMain, n)

        var packedY = baseY.toLong()
        for (i in 0 until n) {
            val child = children[i]
            val stretch = node.alignItems == AlignItems.Stretch
            val cw = when {
                child.preferredWidth != null -> child.preferredWidth!!
                stretch -> availW
                else -> intrinsicWidth(child).coerceAtMost(availW)
            }
            val ch = finalHeights[i]
            val cx = coordinate(baseX.toLong() + crossOffset(node.alignItems, availW, cw).toLong())
            val cy = coordinate(packedY + extraOffsets[i].toLong())
            child.bounds = Rect(cx, cy, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            packedY += ch.toLong() + gap.toLong()
        }

        if (node.preferredHeight == null) {
            node.bounds = node.bounds.copy(height = dimension(usedMain + 2L * inset))
        }
    }

    private fun layoutRow(node: TuiNode, constraints: Constraints) {
        val inset = if (node.drawBorder) 1 else 0
        val baseX = coordinate(node.bounds.x.toLong() + inset)
        val baseY = coordinate(node.bounds.y.toLong() + inset)
        val availW = (constraints.maxWidth - 2 * inset).coerceAtLeast(0)
        val availH = (constraints.maxHeight - 2 * inset).coerceAtLeast(0)
        val gap = node.layoutGap
        val children = node.children
        val n = children.size

        if (n == 0) return

        val bases = IntArray(n) { rowChildBasisWidth(children[it]) }
        val grows = FloatArray(n) { children[it].flexGrow }
        val gapsTotal = (n - 1).coerceAtLeast(0).toLong() * gap.toLong()
        val basesSum = bases.sumOf { it.toLong() }
        val remaining = dimension(availW.toLong() - basesSum - gapsTotal)
        val extras = distributeGrow(grows, remaining)

        val finalWidths = IntArray(n) { bases[it] + extras[it] }
        val usedMain = finalWidths.sumOf { it.toLong() } + gapsTotal

        val extraOffsets = computeMainExtraOffsets(node.justifyContent, availW, usedMain, n)

        var packedX = baseX.toLong()
        for (i in 0 until n) {
            val child = children[i]
            val stretch = node.alignItems == AlignItems.Stretch
            val ch = when {
                child.preferredHeight != null -> child.preferredHeight!!
                stretch -> availH
                else -> intrinsicHeight(child).coerceAtMost(availH)
            }
            val cw = finalWidths[i]
            val cy = coordinate(baseY.toLong() + crossOffset(node.alignItems, availH, ch).toLong())
            val cx = coordinate(packedX + extraOffsets[i].toLong())
            child.bounds = Rect(cx, cy, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            packedX += cw.toLong() + gap.toLong()
        }

        if (node.preferredWidth == null) {
            node.bounds = node.bounds.copy(width = dimension(usedMain + 2L * inset))
        }
    }

    private fun layoutCenter(node: TuiNode, constraints: Constraints) {
        val parentW = node.bounds.width
        val parentH = node.bounds.height
        for (child in node.children) {
            val cw = (child.preferredWidth ?: intrinsicWidth(child)).coerceAtMost(parentW)
            val ch = (child.preferredHeight ?: intrinsicHeight(child)).coerceAtMost(parentH)
            val cx = coordinate(node.bounds.x.toLong() + ((parentW - cw) / 2).coerceAtLeast(0).toLong())
            val cy = coordinate(node.bounds.y.toLong() + ((parentH - ch) / 2).coerceAtLeast(0).toLong())
            child.bounds = Rect(cx, cy, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
        }
    }

    private fun layoutBox(node: TuiNode, constraints: Constraints) {
        for (child in node.children) {
            val cw = child.preferredWidth ?: constraints.maxWidth
            val ch = child.preferredHeight ?: constraints.maxHeight
            if (!child.hasExplicitOffset) {
                child.bounds = Rect(node.bounds.x, node.bounds.y, cw, ch)
            } else {
                child.bounds = child.bounds.copy(width = cw, height = ch)
            }
            layoutNode(child, Constraints(maxWidth = child.bounds.width, maxHeight = child.bounds.height))
        }
    }
}

package dev.usbharu.kotui.compose.layout

import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.layout.Constraints
import dev.usbharu.kotui.utils.displayWidth

object LayoutEngine {
    fun layout(root: TuiNode, screenWidth: Int, screenHeight: Int) {
        root.bounds = Rect(0, 0, screenWidth, screenHeight)
        layoutNode(root, Constraints(maxWidth = screenWidth, maxHeight = screenHeight))
    }

    private fun intrinsicWidth(node: TuiNode): Int {
        node.preferredWidth?.let { return it }
        node.text?.let { return it.displayWidth() }
        val inset = if (node.drawBorder) 2 else 0
        return when (node.layoutPolicy) {
            LayoutPolicy.LEAF -> 0
            LayoutPolicy.ROW -> {
                val gap = node.layoutGap
                val sum = node.children.sumOf { rowChildBasisWidth(it) } +
                    (node.children.size - 1).coerceAtLeast(0) * gap
                sum + inset
            }
            LayoutPolicy.COLUMN,
            LayoutPolicy.BOX,
            LayoutPolicy.CENTER -> (node.children.maxOfOrNull { intrinsicWidth(it) } ?: 0) + inset
        }
    }

    private fun intrinsicHeight(node: TuiNode): Int {
        node.preferredHeight?.let { return it }
        val inset = if (node.drawBorder) 2 else 0
        return when (node.layoutPolicy) {
            LayoutPolicy.LEAF -> if (node.text != null || node.fillChar != null) 1 else 0
            LayoutPolicy.ROW -> (node.children.maxOfOrNull { intrinsicHeight(it) } ?: 0) + inset
            LayoutPolicy.COLUMN -> {
                val gap = node.layoutGap
                val sum = node.children.sumOf { columnChildBasisHeight(it) } +
                    (node.children.size - 1).coerceAtLeast(0) * gap
                sum + inset
            }
            LayoutPolicy.BOX,
            LayoutPolicy.CENTER -> (node.children.maxOfOrNull { intrinsicHeight(it) } ?: 0) + inset
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
        var total = 0f
        for (g in grows) if (g > 0f) total += g
        if (total <= 0f) return extras
        var allocated = 0
        var lastIdx = -1
        for (i in grows.indices) {
            if (grows[i] > 0f) {
                val share = (remaining * (grows[i] / total)).toInt()
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

    private fun computeMainOffsets(
        justify: JustifyContent,
        contentSize: Int,
        usedMain: Int,
        gap: Int,
        count: Int,
    ): Pair<Int, Int> {
        if (count <= 0) return 0 to gap
        val free = (contentSize - usedMain).coerceAtLeast(0)
        return when (justify) {
            JustifyContent.Start -> 0 to gap
            JustifyContent.End -> free to gap
            JustifyContent.Center -> free / 2 to gap
            JustifyContent.SpaceBetween ->
                if (count <= 1) 0 to gap
                else 0 to gap + free / (count - 1)
            JustifyContent.SpaceAround -> {
                val slot = free / count
                slot / 2 to gap + slot
            }
            JustifyContent.SpaceEvenly -> {
                val slot = free / (count + 1)
                slot to gap + slot
            }
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
        val baseX = node.bounds.x + inset
        val baseY = node.bounds.y + inset
        val availW = (constraints.maxWidth - 2 * inset).coerceAtLeast(0)
        val availH = (constraints.maxHeight - 2 * inset).coerceAtLeast(0)
        val gap = node.layoutGap
        val children = node.children
        val n = children.size

        if (n == 0) return

        val bases = IntArray(n) { columnChildBasisHeight(children[it]) }
        val grows = FloatArray(n) { children[it].flexGrow }
        val gapsTotal = (n - 1).coerceAtLeast(0) * gap
        val basesSum = bases.sum()
        val remaining = availH - basesSum - gapsTotal
        val extras = distributeGrow(grows, remaining)

        val finalHeights = IntArray(n) { bases[it] + extras[it] }
        val usedMain = finalHeights.sum() + gapsTotal

        val (startOffset, effectiveGap) = computeMainOffsets(
            node.justifyContent, availH, usedMain, gap, n,
        )

        var currentY = baseY + startOffset
        for (i in 0 until n) {
            val child = children[i]
            val stretch = node.alignItems == AlignItems.Stretch
            val cw = when {
                child.preferredWidth != null -> child.preferredWidth!!
                stretch -> availW
                else -> intrinsicWidth(child).coerceAtMost(availW)
            }
            val ch = finalHeights[i]
            val cx = baseX + crossOffset(node.alignItems, availW, cw)
            child.bounds = Rect(cx, currentY, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            currentY += ch + effectiveGap
        }

        if (node.preferredHeight == null) {
            val content = (usedMain).coerceAtLeast(0)
            node.bounds = node.bounds.copy(height = content + 2 * inset)
        }
    }

    private fun layoutRow(node: TuiNode, constraints: Constraints) {
        val inset = if (node.drawBorder) 1 else 0
        val baseX = node.bounds.x + inset
        val baseY = node.bounds.y + inset
        val availW = (constraints.maxWidth - 2 * inset).coerceAtLeast(0)
        val availH = (constraints.maxHeight - 2 * inset).coerceAtLeast(0)
        val gap = node.layoutGap
        val children = node.children
        val n = children.size

        if (n == 0) return

        val bases = IntArray(n) { rowChildBasisWidth(children[it]) }
        val grows = FloatArray(n) { children[it].flexGrow }
        val gapsTotal = (n - 1).coerceAtLeast(0) * gap
        val basesSum = bases.sum()
        val remaining = availW - basesSum - gapsTotal
        val extras = distributeGrow(grows, remaining)

        val finalWidths = IntArray(n) { bases[it] + extras[it] }
        val usedMain = finalWidths.sum() + gapsTotal

        val (startOffset, effectiveGap) = computeMainOffsets(
            node.justifyContent, availW, usedMain, gap, n,
        )

        var currentX = baseX + startOffset
        for (i in 0 until n) {
            val child = children[i]
            val stretch = node.alignItems == AlignItems.Stretch
            val ch = when {
                child.preferredHeight != null -> child.preferredHeight!!
                stretch -> availH
                else -> intrinsicHeight(child).coerceAtMost(availH)
            }
            val cw = finalWidths[i]
            val cy = baseY + crossOffset(node.alignItems, availH, ch)
            child.bounds = Rect(currentX, cy, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            currentX += cw + effectiveGap
        }

        if (node.preferredWidth == null) {
            val content = (usedMain).coerceAtLeast(0)
            node.bounds = node.bounds.copy(width = content + 2 * inset)
        }
    }

    private fun layoutCenter(node: TuiNode, constraints: Constraints) {
        val parentW = node.bounds.width
        val parentH = node.bounds.height
        for (child in node.children) {
            val cw = (child.preferredWidth ?: intrinsicWidth(child)).coerceAtMost(parentW)
            val ch = (child.preferredHeight ?: 1).coerceAtMost(parentH)
            val cx = node.bounds.x + ((parentW - cw) / 2).coerceAtLeast(0)
            val cy = node.bounds.y + ((parentH - ch) / 2).coerceAtLeast(0)
            child.bounds = Rect(cx, cy, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
        }
    }

    private fun layoutBox(node: TuiNode, constraints: Constraints) {
        for (child in node.children) {
            val cw = child.preferredWidth ?: constraints.maxWidth
            val ch = child.preferredHeight ?: constraints.maxHeight
            if (child.bounds == Rect.ZERO) {
                child.bounds = Rect(node.bounds.x, node.bounds.y, cw, ch)
            } else {
                child.bounds = child.bounds.copy(width = cw, height = ch)
            }
            layoutNode(child, Constraints(maxWidth = child.bounds.width, maxHeight = child.bounds.height))
        }
    }
}

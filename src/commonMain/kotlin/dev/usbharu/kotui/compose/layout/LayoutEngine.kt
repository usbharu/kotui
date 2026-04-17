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
                val sum = node.children.sumOf { intrinsicWidth(it) } +
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
                val sum = node.children.sumOf { intrinsicHeight(it) } +
                    (node.children.size - 1).coerceAtLeast(0) * gap
                sum + inset
            }
            LayoutPolicy.BOX,
            LayoutPolicy.CENTER -> (node.children.maxOfOrNull { intrinsicHeight(it) } ?: 0) + inset
        }
    }

    private fun layoutNode(node: TuiNode, constraints: Constraints) {
        when (node.layoutPolicy) {
            LayoutPolicy.COLUMN -> layoutColumn(node, constraints)
            LayoutPolicy.ROW -> layoutRow(node, constraints)
            LayoutPolicy.BOX -> layoutBox(node, constraints)
            LayoutPolicy.CENTER -> layoutCenter(node, constraints)
            LayoutPolicy.LEAF -> {}
        }
    }

    private fun layoutColumn(node: TuiNode, constraints: Constraints) {
        val inset = if (node.drawBorder) 1 else 0
        var currentY = node.bounds.y + inset
        val x = node.bounds.x + inset
        val gap = node.layoutGap
        val availW = (constraints.maxWidth - 2 * inset).coerceAtLeast(0)

        for (child in node.children) {
            val cw = child.preferredWidth ?: availW
            val ch = child.preferredHeight ?: intrinsicHeight(child)
            child.bounds = Rect(x, currentY, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            currentY += child.bounds.height.coerceAtLeast(ch) + gap
        }

        if (node.preferredHeight == null && node.children.isNotEmpty()) {
            val content = (currentY - node.bounds.y - inset - gap).coerceAtLeast(0)
            node.bounds = node.bounds.copy(height = content + 2 * inset)
        }
    }

    private fun layoutRow(node: TuiNode, constraints: Constraints) {
        val inset = if (node.drawBorder) 1 else 0
        var currentX = node.bounds.x + inset
        val y = node.bounds.y + inset
        val gap = node.layoutGap
        val availH = (constraints.maxHeight - 2 * inset).coerceAtLeast(0)

        for (child in node.children) {
            val cw = intrinsicWidth(child)
            val ch = child.preferredHeight ?: availH
            child.bounds = Rect(currentX, y, cw, ch)
            layoutNode(child, Constraints(maxWidth = cw, maxHeight = ch))
            currentX += child.bounds.width.coerceAtLeast(cw) + gap
        }

        if (node.preferredWidth == null && node.children.isNotEmpty()) {
            val content = (currentX - node.bounds.x - inset - gap).coerceAtLeast(0)
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

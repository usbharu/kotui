package dev.usbharu.kotui.compose.render

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.render.RenderBuffer
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.SixelSupport
import dev.usbharu.kotui.utils.TerminalCaps
import dev.usbharu.kotui.utils.takeDisplayWidth
import dev.usbharu.kotui.utils.displayWidth

class TuiRenderer(
    screenWidth: Int,
    screenHeight: Int,
    private val output: (String) -> Unit = ::print,
) {
    constructor(screenWidth: Int, screenHeight: Int) : this(screenWidth, screenHeight, ::print)

    var screenWidth: Int = screenWidth
        private set
    var screenHeight: Int = screenHeight
        private set
    internal val buffer = RenderBuffer(screenWidth, screenHeight)

    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == screenWidth && newHeight == screenHeight) return
        buffer.resize(newWidth, newHeight)
        screenWidth = newWidth
        screenHeight = newHeight
    }

    fun render(root: TuiNode, focusManager: FocusManager) {
        buffer.clear()
        renderNode(root, 0L, focusManager)
        val cursorNode = findCursorNode(root, focusManager)
        flush(cursorNode)
    }

    /**
     * Populates the internal [buffer] by running the full render pass but
     * without emitting ANSI escapes to stdout. Primarily for tests that want
     * to inspect cell contents / placements after a layout + render cycle.
     */
    internal fun renderToBuffer(root: TuiNode, focusManager: FocusManager) {
        buffer.clear()
        renderNode(root, 0L, focusManager)
    }

    private fun renderNode(node: TuiNode, parentZ: Long, focusManager: FocusManager) {
        val nodeZ = node.zIndex.toLong()
        val effectiveZLong = when {
            nodeZ > 0 && parentZ > Long.MAX_VALUE - nodeZ -> Long.MAX_VALUE
            nodeZ < 0 && parentZ < Long.MIN_VALUE - nodeZ -> Long.MIN_VALUE
            else -> parentZ + nodeZ
        }
        val effectiveZ = effectiveZLong.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        val isFocused = node.focusable && focusManager.isFocused(node.focusId)
        val activeStyle = if (isFocused) (node.focusedStyle ?: node.style) else node.style

        node.fillChar?.let { ch ->
            val fillWidth = ch.toString().displayWidth()
            for (yy in node.bounds.y until node.bounds.y + node.bounds.height) {
                var dx = 0
                while (fillWidth > 0 && dx + fillWidth <= node.bounds.width) {
                    buffer.setGrapheme(node.bounds.x + dx, yy, ch.toString(), fillWidth, activeStyle, effectiveZ)
                    dx += fillWidth
                }
            }
        }

        if (node.drawBorder) {
            renderBorder(node.bounds, activeStyle, node.borderTitle, effectiveZ)
        }

        node.text?.let { text ->
            val clipped = text.takeDisplayWidth(node.bounds.width)
            buffer.writeString(node.bounds.x, node.bounds.y, clipped, activeStyle, effectiveZ)
            node.textHighlights?.forEach { hl ->
                applyHighlight(node.bounds.x, node.bounds.y, hl, activeStyle, effectiveZ, node.bounds.width)
            }
        }

        node.image?.let { img ->
            val placed = buffer.placeImage(node.bounds.x, node.bounds.y, img, effectiveZ)
            val caps = SixelSupport.cached ?: TerminalCaps.UNSUPPORTED
            if (!placed || (!caps.kittySupported && !caps.sixelSupported)) {
                img.fallbackText?.let { text ->
                    val clipped = text.takeDisplayWidth(minOf(img.cellWidth, node.bounds.width.coerceAtLeast(0)))
                    buffer.writeString(node.bounds.x, node.bounds.y, clipped, activeStyle, effectiveZ)
                }
            }
        }

        for (child in node.children) {
            renderNode(child, effectiveZLong, focusManager)
        }
    }

    private fun renderBorder(b: Rect, style: Style, title: String?, z: Int) {
        val (x, y, w, h) = b
        if (w <= 0 || h <= 0) return
        buffer.set(x, y, '+', style, z)
        for (col in 1 until w - 1) buffer.set(x + col, y, '-', style, z)
        if (w > 1) buffer.set(x + w - 1, y, '+', style, z)

        if (!title.isNullOrEmpty()) {
            buffer.writeString(x + 2, y, " $title ".takeDisplayWidth((w - 4).coerceAtLeast(0)), style, z)
        }

        for (row in 1 until h - 1) {
            buffer.set(x, y + row, '|', style, z)
            for (col in 1 until w - 1) buffer.set(x + col, y + row, ' ', style, z)
            if (w > 1) buffer.set(x + w - 1, y + row, '|', style, z)
        }

        if (h > 1) {
            buffer.set(x, y + h - 1, '+', style, z)
            for (col in 1 until w - 1) buffer.set(x + col, y + h - 1, '-', style, z)
            if (w > 1) buffer.set(x + w - 1, y + h - 1, '+', style, z)
        }
    }

    private fun applyHighlight(nodeX: Int, nodeY: Int, hl: dev.usbharu.kotui.compose.node.TextHighlight, base: Style, zIndex: Int, nodeWidth: Int) {
        val start = hl.startCol.coerceAtLeast(0)
        val end = hl.endCol.coerceAtMost(nodeWidth)
        for (dx in start until end) {
            val x = nodeX + dx
            var targetX = x
            var existing = buffer.get(targetX, nodeY)
            if (existing.isContinuation && targetX > 0) {
                targetX--
                existing = buffer.get(targetX, nodeY)
            }
            val merged = base.copy(
                fg = hl.style.fg ?: existing.style.fg ?: base.fg,
                bg = hl.style.bg ?: existing.style.bg ?: base.bg,
                bold = hl.style.bold || existing.style.bold,
                underline = hl.style.underline || existing.style.underline,
                reverse = hl.style.reverse || existing.style.reverse,
            )
            val content = existing.content
            val width = if (existing.width == 0) 1 else existing.width
            buffer.setGrapheme(targetX, nodeY, if (content.isEmpty()) " " else content, width, merged, zIndex)
        }
    }

    private fun findCursorNode(node: TuiNode, focusManager: FocusManager): TuiNode? {
        if (node.focusable && focusManager.isFocused(node.focusId) && node.cursorCol != null) {
            return node
        }
        for (child in node.children) {
            findCursorNode(child, focusManager)?.let { return it }
        }
        return null
    }

    private fun flush(cursorNode: TuiNode?) {
        val sb = StringBuilder()
        sb.append(Ansi.CURSOR_HIDE)

        // kgp images are persistent overlays, unlike sixel which paints into
        // the cell buffer. Wipe any lingering images from the previous frame
        // before we redraw so navigation and animation work the same way.
        val caps = SixelSupport.cached ?: TerminalCaps.UNSUPPORTED
        if (caps.kittySupported) {
            sb.append(dev.usbharu.kotui.utils.Kitty.DELETE_ALL)
        }
        var lastStyle: Style? = null
        for (y in 0 until screenHeight) {
            sb.append(Ansi.cursorTo(y + 1, 1))
            for (x in 0 until screenWidth) {
                val cell = buffer.get(x, y)
                if (cell.isContinuation) continue
                if (cell.style != lastStyle) {
                    sb.append(Ansi.RESET)
                    sb.append(styleToAnsi(cell.style))
                    lastStyle = cell.style
                }
                sb.append(cell.content)
            }
        }
        sb.append(Ansi.RESET)

        if ((caps.kittySupported || caps.sixelSupported) && screenWidth > 0 && screenHeight > 0) {
            for (p in buffer.imagePlacements()) {
                if (p.x !in 0 until screenWidth || p.y !in 0 until screenHeight) continue
                var unobscured = true
                for (dy in 0 until p.cellHeight) {
                    for (dx in 0 until p.cellWidth) {
                        val cell = buffer.get(p.x + dx, p.y + dy)
                        if (cell.zIndex > p.zIndex || (cell.zIndex == p.zIndex && cell.content != " ")) {
                            unobscured = false
                        }
                    }
                }
                if (!unobscured) continue
                val row = p.y + 1
                val col = p.x + 1
                sb.append(Ansi.cursorTo(row, col))
                sb.append(Ansi.RESET)
                if (caps.kittySupported) {
                    sb.append(p.image.kitty)
                } else {
                    sb.append(p.image.sixel)
                }
            }
        }

        if (cursorNode != null && screenWidth > 0 && screenHeight > 0) {
            val col = (cursorNode.bounds.x.toLong() + (cursorNode.cursorCol ?: 0).toLong() + 1L)
                .coerceIn(1L, screenWidth.toLong()).toInt()
            val row = (cursorNode.bounds.y.toLong() + (cursorNode.cursorRow ?: 0).toLong() + 1L)
                .coerceIn(1L, screenHeight.toLong()).toInt()
            sb.append(Ansi.cursorTo(row, col))
            sb.append(Ansi.CURSOR_SHOW)
        } else {
            sb.append(Ansi.CURSOR_HIDE)
        }

        output(sb.toString())
    }

    private fun styleToAnsi(style: Style): String = buildString {
        style.fg?.takeIf(::isSafeSgrSequence)?.let { append(it) }
        style.bg?.takeIf(::isSafeSgrSequence)?.let { append(it) }
        if (style.bold) append(Ansi.BOLD)
        if (style.underline) append(Ansi.UNDERLINE)
        if (style.reverse) append(Ansi.REVERSE)
    }
}

internal fun isSafeSgrSequence(value: String): Boolean {
    if (!value.startsWith("\u001B[") || !value.endsWith('m')) return false
    val parameters = value.substring(2, value.length - 1)
    if (parameters.isEmpty()) return true
    return parameters.split(';').all { part ->
        part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) && (part.toIntOrNull() ?: -1) in 0..255
    }
}

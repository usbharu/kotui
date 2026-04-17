package dev.usbharu.kotui.render

import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.forEachCodePoint

data class Cell(
    val content: String = " ",
    val style: Style = Style(),
    val zIndex: Int = 0,
    val width: Int = 1,
    val isContinuation: Boolean = false
)

/** Region reserved for a pixel image, anchored at cell coordinate ([x], [y]). */
data class ImagePlacement(
    val x: Int,
    val y: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val image: TerminalImage,
    val zIndex: Int,
)

class RenderBuffer(val width: Int, val height: Int) {
    private val cells: Array<Array<Cell>> = Array(height) { Array(width) { Cell() } }
    private val placements: MutableList<ImagePlacement> = mutableListOf()

    fun set(x: Int, y: Int, char: Char, style: Style, zIndex: Int) {
        setGrapheme(x, y, char.toString(), 1, style, zIndex)
    }

    fun setGrapheme(x: Int, y: Int, grapheme: String, graphemeWidth: Int, style: Style, zIndex: Int) {
        if (y < 0 || y >= height) return
        if (graphemeWidth <= 0) return

        val placeWidth = if (graphemeWidth == 2 && x + 1 >= width) 1 else graphemeWidth
        val placeContent = if (graphemeWidth == 2 && x + 1 >= width) " " else grapheme

        if (x < 0 || x >= width) return

        val current = cells[y][x]
        if (zIndex < current.zIndex) return

        clearOverlap(x, y, placeWidth, zIndex)
        if (!canWrite(x, y, placeWidth, zIndex)) return

        cells[y][x] = Cell(
            content = placeContent,
            style = style,
            zIndex = zIndex,
            width = placeWidth,
            isContinuation = false
        )
        if (placeWidth == 2) {
            cells[y][x + 1] = Cell(
                content = "",
                style = style,
                zIndex = zIndex,
                width = 0,
                isContinuation = true
            )
        }
    }

    private fun canWrite(x: Int, y: Int, w: Int, zIndex: Int): Boolean {
        for (dx in 0 until w) {
            val cx = x + dx
            if (cx < 0 || cx >= width) return false
            if (zIndex < cells[y][cx].zIndex) return false
        }
        return true
    }

    private fun clearOverlap(x: Int, y: Int, w: Int, zIndex: Int) {
        if (x - 1 >= 0) {
            val left = cells[y][x - 1]
            if (left.width == 2 && zIndex >= left.zIndex) {
                cells[y][x - 1] = Cell(style = left.style, zIndex = left.zIndex)
            }
        }
        if (w == 2) {
            val right = x + 1
            if (right < width) {
                val r = cells[y][right]
                if (r.isContinuation && zIndex >= r.zIndex) {
                    // belongs to a wide cell at x; it will be overwritten below
                }
                if (r.width == 2 && x + 2 < width) {
                    val far = cells[y][x + 2]
                    if (far.isContinuation && zIndex >= far.zIndex) {
                        cells[y][x + 2] = Cell(style = far.style, zIndex = far.zIndex)
                    }
                }
            }
        }
    }

    fun writeString(x: Int, y: Int, text: String, style: Style, zIndex: Int) {
        if (y < 0 || y >= height) return
        var cursorX = x
        val sb = StringBuilder()
        var pendingCp = -1
        var pendingWidth = 0

        fun flushPending() {
            if (pendingCp >= 0) {
                setGrapheme(cursorX, y, sb.toString(), pendingWidth, style, zIndex)
                cursorX += if (pendingWidth == 0) 0 else pendingWidth
                sb.clear()
                pendingCp = -1
                pendingWidth = 0
            }
        }

        text.forEachCodePoint { cp, w, _ ->
            if (w == 0 && pendingCp >= 0) {
                appendCodePoint(sb, cp)
            } else {
                flushPending()
                appendCodePoint(sb, cp)
                pendingCp = cp
                pendingWidth = w
            }
        }
        flushPending()
    }

    private fun appendCodePoint(sb: StringBuilder, cp: Int) {
        if (cp <= 0xFFFF) {
            sb.append(cp.toChar())
        } else {
            val offset = cp - 0x10000
            sb.append((0xD800 + (offset shr 10)).toChar())
            sb.append((0xDC00 + (offset and 0x3FF)).toChar())
        }
    }

    fun get(x: Int, y: Int): Cell {
        if (x < 0 || x >= width || y < 0 || y >= height) return Cell()
        return cells[y][x]
    }

    fun clear() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                cells[y][x] = Cell()
            }
        }
        placements.clear()
    }

    /**
     * Reserves the rectangle [x, x + image.cellWidth) × [y, y + image.cellHeight)
     * for a sixel image drawn at [zIndex]. The cells are blanked with spaces at
     * [zIndex] so lower-priority content is cleared and higher-priority overlays
     * can still paint on top via the normal z-index rules. The caller decides
     * whether to additionally paint a fallback string depending on whether the
     * terminal supports sixel.
     */
    fun placeImage(x: Int, y: Int, image: TerminalImage, zIndex: Int) {
        val w = image.cellWidth
        val h = image.cellHeight
        for (dy in 0 until h) {
            val cy = y + dy
            if (cy < 0 || cy >= height) continue
            for (dx in 0 until w) {
                val cx = x + dx
                if (cx < 0 || cx >= width) continue
                if (zIndex < cells[cy][cx].zIndex) continue
                cells[cy][cx] = Cell(content = " ", zIndex = zIndex)
            }
        }
        placements.add(ImagePlacement(x, y, w, h, image, zIndex))
    }

    fun imagePlacements(): List<ImagePlacement> = placements
}

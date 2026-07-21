package dev.usbharu.kotui.render

import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.forEachTerminalGrapheme
import dev.usbharu.kotui.utils.displayWidth

data class Cell(
    val content: String = " ",
    val style: Style = Style(),
    val zIndex: Int = Int.MIN_VALUE,
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

class RenderBuffer(width: Int, height: Int) {
    init {
        require(width >= 0) { "width must be non-negative" }
        require(height >= 0) { "height must be non-negative" }
    }
    var width: Int = width
        private set
    var height: Int = height
        private set
    private var cells: Array<Array<Cell>> = Array(height) { Array(width) { Cell() } }
    private val placements: MutableList<ImagePlacement> = mutableListOf()

    /** Reallocates the cell grid to [newWidth] × [newHeight]. Existing contents are discarded. */
    fun resize(newWidth: Int, newHeight: Int) {
        require(newWidth >= 0) { "width must be non-negative" }
        require(newHeight >= 0) { "height must be non-negative" }
        if (newWidth == width && newHeight == height) return
        width = newWidth
        height = newHeight
        cells = Array(newHeight) { Array(newWidth) { Cell() } }
        placements.clear()
    }

    fun set(x: Int, y: Int, char: Char, style: Style, zIndex: Int) {
        val content = char.toString()
        val width = content.displayWidth()
        if (width > 0) setGrapheme(x, y, content, width, style, zIndex)
    }

    fun setGrapheme(x: Int, y: Int, grapheme: String, graphemeWidth: Int, style: Style, zIndex: Int) {
        require(grapheme.isNotEmpty()) { "grapheme must not be empty" }
        require(graphemeWidth in 1..2) { "terminal grapheme width must be 1 or 2" }
        if (y < 0 || y >= height) return

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
        val existing = cells[y][x]
        if (existing.width == 2 && x + 1 < width && zIndex >= existing.zIndex) {
            val continuation = cells[y][x + 1]
            if (continuation.isContinuation && continuation.zIndex == existing.zIndex) {
                cells[y][x + 1] = Cell()
            }
        }
        if (x - 1 >= 0) {
            val left = cells[y][x - 1]
            if (left.width == 2 && zIndex >= left.zIndex) {
                cells[y][x - 1] = Cell()
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
                        cells[y][x + 2] = Cell()
                    }
                }
            }
        }
    }

    fun writeString(x: Int, y: Int, text: String, style: Style, zIndex: Int) {
        if (y < 0 || y >= height) return
        var cursorX = x
        text.forEachTerminalGrapheme { start, end, graphemeWidth ->
            if (graphemeWidth > 0) {
                setGrapheme(cursorX, y, text.substring(start, end), graphemeWidth, style, zIndex)
            }
            cursorX += graphemeWidth
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
    fun placeImage(x: Int, y: Int, image: TerminalImage, zIndex: Int): Boolean {
        val w = image.cellWidth
        val h = image.cellHeight
        if (x < 0 || y < 0 ||
            x.toLong() + w.toLong() > width.toLong() ||
            y.toLong() + h.toLong() > height.toLong()
        ) return false
        var blocked = false
        for (dy in 0 until h) {
            val cy = y + dy
            for (dx in 0 until w) {
                val cx = x + dx
                if (zIndex < cells[cy][cx].zIndex) blocked = true
            }
        }
        if (blocked) return false
        for (dy in 0 until h) {
            val cy = y + dy
            for (dx in 0 until w) {
                val cx = x + dx
                clearOverlap(cx, cy, 1, zIndex)
                cells[cy][cx] = Cell(content = " ", zIndex = zIndex)
            }
        }
        placements.add(ImagePlacement(x, y, w, h, image, zIndex))
        return true
    }

    fun imagePlacements(): List<ImagePlacement> = placements.toList()
}

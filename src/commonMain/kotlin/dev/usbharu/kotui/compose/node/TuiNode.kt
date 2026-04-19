package dev.usbharu.kotui.compose.node

import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style

enum class LayoutPolicy { COLUMN, ROW, BOX, LEAF, CENTER }

/** Cell-column range (inclusive start, exclusive end) that should be rendered with [style]. */
data class TextHighlight(val startCol: Int, val endCol: Int, val style: Style)

class TuiNode(val tag: String = "Node") {
    var parent: TuiNode? = null
        internal set
    val children: MutableList<TuiNode> = mutableListOf()

    var layoutPolicy: LayoutPolicy = LayoutPolicy.LEAF
    var bounds: Rect = Rect.ZERO
    var preferredWidth: Int? = null
    var preferredHeight: Int? = null
    var layoutGap: Int = 0

    var flexGrow: Float = 0f
    var flexBasis: Int? = null
    var justifyContent: JustifyContent = JustifyContent.Start
    var alignItems: AlignItems = AlignItems.Stretch

    var style: Style = Style()
    var focusedStyle: Style? = null

    var text: String? = null
    var fillChar: Char? = null
    var zIndex: Int = 0
    var drawBorder: Boolean = false
    var borderTitle: String? = null

    var focusable: Boolean = false
    var focusScope: Boolean = false
    var focusId: Int = -1

    var onKeyEvent: ((KeyEvent) -> Boolean)? = null
    var onPaste: ((String) -> Boolean)? = null
    var onActivate: (() -> Unit)? = null

    /** Column offset within this node where the terminal cursor should be placed. null = no cursor. */
    var cursorCol: Int? = null

    /**
     * Optional styled ranges applied on top of [text] when rendering. Ranges are
     * expressed in terminal cell columns relative to the node's left edge. Used for
     * things like selection highlights in TextInput.
     */
    var textHighlights: List<TextHighlight>? = null

    /**
     * Pixel-based image to emit (via sixel or Kitty graphics protocol) after
     * the regular cell buffer has been flushed. The node's [bounds] dictates
     * where the image is anchored in cell coordinates.
     */
    var image: dev.usbharu.kotui.compose.widget.TerminalImage? = null

    fun applyModifier(modifier: Modifier) {
        modifier.foldIn(Unit) { _, element -> element.apply(this) }
    }

    fun insertAt(index: Int, child: TuiNode) {
        children.add(index, child)
        child.parent = this
    }

    fun removeAt(index: Int, count: Int) {
        repeat(count) { children.removeAt(index).parent = null }
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from == to) return
        val moved = (0 until count).map { children.removeAt(from) }
        children.addAll(if (to > from) to - count else to, moved)
    }

    fun clear() {
        children.forEach { it.parent = null }
        children.clear()
    }
}

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
    private data class ModifierSnapshot(
        val preferredWidth: Int?, val preferredHeight: Int?,
        val style: Style, val focusedStyle: Style?,
        val focusable: Boolean, val focusScope: Boolean,
        val zIndex: Int, val layoutGap: Int, val flexGrow: Float, val flexBasis: Int?,
        val x: Int, val y: Int, val hasExplicitOffset: Boolean,
        val onKeyEvent: ((KeyEvent) -> Boolean)?,
    )

    private var modifierBase: ModifierSnapshot? = null
    private var modifierApplied: ModifierSnapshot? = null
    var parent: TuiNode? = null
        internal set
    private val mutableChildren: MutableList<TuiNode> = mutableListOf()
    val children: List<TuiNode> = object : AbstractList<TuiNode>() {
        override val size: Int get() = mutableChildren.size
        override fun get(index: Int): TuiNode = mutableChildren[index]
    }

    var layoutPolicy: LayoutPolicy = LayoutPolicy.LEAF
    var bounds: Rect = Rect.ZERO
    internal var hasExplicitOffset: Boolean = false
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

    /** Row offset within this node where the terminal cursor should be placed. 0 if unset. */
    var cursorRow: Int? = null

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
        restorePreviousModifier()
        val base = modifierSnapshot()
        modifier.foldIn(Unit) { _, element -> element.apply(this) }
        val applied = modifierSnapshot()
        modifierBase = base
        modifierApplied = applied
    }

    private fun modifierSnapshot() = ModifierSnapshot(
        preferredWidth, preferredHeight, style, focusedStyle, focusable, focusScope,
        zIndex, layoutGap, flexGrow, flexBasis, bounds.x, bounds.y, hasExplicitOffset, onKeyEvent,
    )

    private fun restorePreviousModifier() {
        val base = modifierBase ?: return
        val applied = modifierApplied ?: return
        if (preferredWidth == applied.preferredWidth) preferredWidth = base.preferredWidth
        if (preferredHeight == applied.preferredHeight) preferredHeight = base.preferredHeight
        if (style == applied.style) style = base.style
        if (focusedStyle == applied.focusedStyle) focusedStyle = base.focusedStyle
        if (focusable == applied.focusable) focusable = base.focusable
        if (focusScope == applied.focusScope) focusScope = base.focusScope
        if (zIndex == applied.zIndex) zIndex = base.zIndex
        if (layoutGap == applied.layoutGap) layoutGap = base.layoutGap
        if (flexGrow == applied.flexGrow) flexGrow = base.flexGrow
        if (flexBasis == applied.flexBasis) flexBasis = base.flexBasis
        var restoredX = bounds.x
        var restoredY = bounds.y
        if (bounds.x == applied.x) restoredX = base.x
        if (bounds.y == applied.y) restoredY = base.y
        bounds = bounds.copy(x = restoredX, y = restoredY)
        if (hasExplicitOffset == applied.hasExplicitOffset) hasExplicitOffset = base.hasExplicitOffset
        if (onKeyEvent == applied.onKeyEvent) onKeyEvent = base.onKeyEvent
        modifierBase = null
        modifierApplied = null
    }

    internal fun applyOffset(x: Int, y: Int) {
        hasExplicitOffset = true
        bounds = Rect(x, y, bounds.width, bounds.height)
    }

    fun insertAt(index: Int, child: TuiNode) {
        require(child !== this) { "a node cannot be its own child" }
        var ancestor: TuiNode? = this
        while (ancestor != null) {
            require(ancestor !== child) { "inserting an ancestor would create a cycle" }
            ancestor = ancestor.parent
        }
        val oldParent = child.parent
        val resultingSize = mutableChildren.size - if (oldParent === this) 1 else 0
        require(index in 0..resultingSize) { "index out of bounds: $index" }
        oldParent?.let {
            oldParent.mutableChildren.remove(child)
            child.parent = null
        }
        mutableChildren.add(index, child)
        child.parent = this
    }

    fun removeAt(index: Int, count: Int) {
        require(count >= 0) { "count must be non-negative" }
        require(index >= 0 && index <= mutableChildren.size - count) { "range out of bounds: index=$index count=$count" }
        repeat(count) { mutableChildren.removeAt(index).parent = null }
    }

    fun move(from: Int, to: Int, count: Int) {
        require(count >= 0) { "count must be non-negative" }
        require(from >= 0 && from <= mutableChildren.size - count) { "source range out of bounds" }
        require(to in 0..mutableChildren.size) { "destination out of bounds" }
        if (from == to || count == 0) return
        require(to !in (from + 1) until (from + count)) { "destination cannot be inside moved range" }
        val moved = (0 until count).map { mutableChildren.removeAt(from) }
        mutableChildren.addAll(if (to > from) to - count else to, moved)
    }

    fun clear() {
        mutableChildren.forEach { it.parent = null }
        mutableChildren.clear()
    }
}

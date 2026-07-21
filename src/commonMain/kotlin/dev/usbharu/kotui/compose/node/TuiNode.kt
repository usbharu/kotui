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
        val layoutPolicy: LayoutPolicy, val bounds: Rect, val hasExplicitOffset: Boolean,
        val preferredWidth: Int?, val preferredHeight: Int?, val layoutGap: Int,
        val flexGrow: Float, val flexBasis: Int?,
        val justifyContent: JustifyContent, val alignItems: AlignItems,
        val style: Style, val focusedStyle: Style?,
        val text: String?, val fillChar: Char?, val zIndex: Int,
        val drawBorder: Boolean, val borderTitle: String?,
        val focusable: Boolean, val focusScope: Boolean, val focusId: Int,
        val onKeyEvent: ((KeyEvent) -> Boolean)?, val onPaste: ((String) -> Boolean)?,
        val onActivate: (() -> Unit)?, val cursorCol: Int?, val cursorRow: Int?,
        val textHighlights: List<TextHighlight>?,
        val image: dev.usbharu.kotui.compose.widget.TerminalImage?,
    )

    private var modifierBase: ModifierSnapshot? = null
    private var modifierApplied: ModifierSnapshot? = null
    private var modifierUpdatePrepared = false
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
        if (!modifierUpdatePrepared) restorePreviousModifierPreservingChanges()
        val base = modifierSnapshot()
        modifierBase = base
        modifierUpdatePrepared = false
        modifier.foldIn(Unit) { _, element -> element.apply(this) }
        modifierApplied = modifierSnapshot()
    }

    private fun modifierSnapshot() = ModifierSnapshot(
        layoutPolicy, bounds, hasExplicitOffset,
        preferredWidth, preferredHeight, layoutGap, flexGrow, flexBasis, justifyContent, alignItems,
        style, focusedStyle, text, fillChar, zIndex, drawBorder, borderTitle,
        focusable, focusScope, focusId, onKeyEvent, onPaste, onActivate,
        cursorCol, cursorRow, textHighlights, image,
    )

    /** Restores the unmodified node state before widget setters run for a new update pass. */
    fun beginModifierUpdate() {
        modifierBase?.let(::restoreModifierSnapshot)
        modifierBase = null
        modifierApplied = null
        modifierUpdatePrepared = true
    }

    private fun restorePreviousModifierPreservingChanges() {
        val base = modifierBase ?: return
        val applied = modifierApplied ?: return
        val current = modifierSnapshot()
        restoreModifierSnapshot(base)
        if (current.layoutPolicy != applied.layoutPolicy) layoutPolicy = current.layoutPolicy
        if (current.bounds != applied.bounds) bounds = current.bounds
        if (current.hasExplicitOffset != applied.hasExplicitOffset) hasExplicitOffset = current.hasExplicitOffset
        if (current.preferredWidth != applied.preferredWidth) preferredWidth = current.preferredWidth
        if (current.preferredHeight != applied.preferredHeight) preferredHeight = current.preferredHeight
        if (current.layoutGap != applied.layoutGap) layoutGap = current.layoutGap
        if (current.flexGrow != applied.flexGrow) flexGrow = current.flexGrow
        if (current.flexBasis != applied.flexBasis) flexBasis = current.flexBasis
        if (current.justifyContent != applied.justifyContent) justifyContent = current.justifyContent
        if (current.alignItems != applied.alignItems) alignItems = current.alignItems
        if (current.style != applied.style) style = current.style
        if (current.focusedStyle != applied.focusedStyle) focusedStyle = current.focusedStyle
        if (current.text != applied.text) text = current.text
        if (current.fillChar != applied.fillChar) fillChar = current.fillChar
        if (current.zIndex != applied.zIndex) zIndex = current.zIndex
        if (current.drawBorder != applied.drawBorder) drawBorder = current.drawBorder
        if (current.borderTitle != applied.borderTitle) borderTitle = current.borderTitle
        if (current.focusable != applied.focusable) focusable = current.focusable
        if (current.focusScope != applied.focusScope) focusScope = current.focusScope
        if (current.focusId != applied.focusId) focusId = current.focusId
        if (current.onKeyEvent != applied.onKeyEvent) onKeyEvent = current.onKeyEvent
        if (current.onPaste != applied.onPaste) onPaste = current.onPaste
        if (current.onActivate != applied.onActivate) onActivate = current.onActivate
        if (current.cursorCol != applied.cursorCol) cursorCol = current.cursorCol
        if (current.cursorRow != applied.cursorRow) cursorRow = current.cursorRow
        if (current.textHighlights != applied.textHighlights) textHighlights = current.textHighlights
        if (current.image != applied.image) image = current.image
        modifierBase = null
        modifierApplied = null
    }

    private fun restoreModifierSnapshot(base: ModifierSnapshot) {
        layoutPolicy = base.layoutPolicy
        bounds = base.bounds
        hasExplicitOffset = base.hasExplicitOffset
        preferredWidth = base.preferredWidth
        preferredHeight = base.preferredHeight
        layoutGap = base.layoutGap
        flexGrow = base.flexGrow
        flexBasis = base.flexBasis
        justifyContent = base.justifyContent
        alignItems = base.alignItems
        style = base.style
        focusedStyle = base.focusedStyle
        text = base.text
        fillChar = base.fillChar
        zIndex = base.zIndex
        drawBorder = base.drawBorder
        borderTitle = base.borderTitle
        focusable = base.focusable
        focusScope = base.focusScope
        focusId = base.focusId
        onKeyEvent = base.onKeyEvent
        onPaste = base.onPaste
        onActivate = base.onActivate
        cursorCol = base.cursorCol
        cursorRow = base.cursorRow
        textHighlights = base.textHighlights
        image = base.image
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

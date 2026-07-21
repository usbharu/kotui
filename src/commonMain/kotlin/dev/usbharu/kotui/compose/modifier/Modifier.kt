package dev.usbharu.kotui.compose.modifier

import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Rect
import dev.usbharu.kotui.core.Style

interface Modifier {
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R
    fun then(other: Modifier): Modifier =
        if (other === Companion) this else CombinedModifier(this, other)

    interface Element : Modifier {
        fun apply(node: TuiNode)
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = operation(initial, this)
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = operation(this, initial)
    }

    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override fun then(other: Modifier): Modifier = other
        override fun toString() = "Modifier"
    }
}

private data class CombinedModifier(private val outer: Modifier, private val inner: Modifier) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)
    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)
}

private data class WidthMod(val w: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.preferredWidth = w } }
private data class HeightMod(val h: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.preferredHeight = h } }
private data class StyleMod(val s: Style) : Modifier.Element { override fun apply(node: TuiNode) { node.style = s } }
private data class FocusedStyleMod(val s: Style) : Modifier.Element { override fun apply(node: TuiNode) { node.focusedStyle = s } }
private object FocusableMod : Modifier.Element { override fun apply(node: TuiNode) { node.focusable = true } }
private object FocusScopeMod : Modifier.Element { override fun apply(node: TuiNode) { node.focusScope = true } }
private data class ZIndexMod(val z: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.zIndex = z } }
private data class GapMod(val g: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.layoutGap = g } }
private data class FlexGrowMod(val v: Float) : Modifier.Element { override fun apply(node: TuiNode) { node.flexGrow = v } }
private data class FlexBasisMod(val v: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.flexBasis = v } }
private data class OffsetMod(val x: Int, val y: Int) : Modifier.Element {
    override fun apply(node: TuiNode) { node.applyOffset(x, y) }
}
private data class KeyEventMod(val handler: (KeyEvent) -> Boolean) : Modifier.Element {
    override fun apply(node: TuiNode) { node.onKeyEvent = handler }
}

fun Modifier.width(n: Int): Modifier {
    require(n >= 0) { "width must be non-negative" }
    return then(WidthMod(n))
}
fun Modifier.height(n: Int): Modifier {
    require(n >= 0) { "height must be non-negative" }
    return then(HeightMod(n))
}
fun Modifier.size(w: Int, h: Int): Modifier = width(w).height(h)
fun Modifier.style(style: Style): Modifier = then(StyleMod(style))
fun Modifier.focusedStyle(style: Style): Modifier = then(FocusedStyleMod(style))
fun Modifier.focusable(): Modifier = then(FocusableMod)
fun Modifier.focusScope(): Modifier = then(FocusScopeMod)
fun Modifier.zIndex(n: Int): Modifier = then(ZIndexMod(n))
fun Modifier.gap(n: Int): Modifier {
    require(n >= 0) { "gap must be non-negative" }
    return then(GapMod(n))
}
fun Modifier.weight(value: Float): Modifier = flexGrow(value)
fun Modifier.flexGrow(value: Float): Modifier {
    require(value.isFinite() && value >= 0f) { "flex grow must be finite and non-negative" }
    return then(FlexGrowMod(value))
}
fun Modifier.flexBasis(size: Int): Modifier {
    require(size >= 0) { "flex basis must be non-negative" }
    return then(FlexBasisMod(size))
}
fun Modifier.offset(x: Int, y: Int): Modifier = then(OffsetMod(x, y))
fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier = then(KeyEventMod(handler))

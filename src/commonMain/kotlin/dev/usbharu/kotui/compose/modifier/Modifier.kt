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

private class CombinedModifier(private val outer: Modifier, private val inner: Modifier) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)
    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)
}

private class WidthMod(val w: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.preferredWidth = w } }
private class HeightMod(val h: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.preferredHeight = h } }
private class StyleMod(val s: Style) : Modifier.Element { override fun apply(node: TuiNode) { node.style = s } }
private class FocusedStyleMod(val s: Style) : Modifier.Element { override fun apply(node: TuiNode) { node.focusedStyle = s } }
private class FocusableMod : Modifier.Element { override fun apply(node: TuiNode) { node.focusable = true } }
private class FocusScopeMod : Modifier.Element { override fun apply(node: TuiNode) { node.focusScope = true } }
private class ZIndexMod(val z: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.zIndex = z } }
private class GapMod(val g: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.layoutGap = g } }
private class FlexGrowMod(val v: Float) : Modifier.Element { override fun apply(node: TuiNode) { node.flexGrow = v } }
private class FlexBasisMod(val v: Int) : Modifier.Element { override fun apply(node: TuiNode) { node.flexBasis = v } }
private class OffsetMod(val x: Int, val y: Int) : Modifier.Element {
    override fun apply(node: TuiNode) { node.bounds = Rect(x, y, node.bounds.width, node.bounds.height) }
}
private class KeyEventMod(val handler: (KeyEvent) -> Boolean) : Modifier.Element {
    override fun apply(node: TuiNode) { node.onKeyEvent = handler }
}

fun Modifier.width(n: Int): Modifier = then(WidthMod(n))
fun Modifier.height(n: Int): Modifier = then(HeightMod(n))
fun Modifier.size(w: Int, h: Int): Modifier = width(w).height(h)
fun Modifier.style(style: Style): Modifier = then(StyleMod(style))
fun Modifier.focusedStyle(style: Style): Modifier = then(FocusedStyleMod(style))
fun Modifier.focusable(): Modifier = then(FocusableMod())
fun Modifier.focusScope(): Modifier = then(FocusScopeMod())
fun Modifier.zIndex(n: Int): Modifier = then(ZIndexMod(n))
fun Modifier.gap(n: Int): Modifier = then(GapMod(n))
fun Modifier.weight(value: Float): Modifier = then(FlexGrowMod(value))
fun Modifier.flexGrow(value: Float): Modifier = then(FlexGrowMod(value))
fun Modifier.flexBasis(size: Int): Modifier = then(FlexBasisMod(size))
fun Modifier.offset(x: Int, y: Int): Modifier = then(OffsetMod(x, y))
fun Modifier.onKeyEvent(handler: (KeyEvent) -> Boolean): Modifier = then(KeyEventMod(handler))

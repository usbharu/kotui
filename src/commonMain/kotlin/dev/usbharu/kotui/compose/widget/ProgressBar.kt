package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.utils.displayWidth

internal fun buildProgressText(
    progress: Float,
    width: Int,
    showPercent: Boolean,
    filledChar: Char,
    emptyChar: Char,
): String {
    require(width >= 0) { "progress bar width must be non-negative" }
    require(filledChar.toString().displayWidth() == 1) { "filledChar must occupy one terminal cell" }
    require(emptyChar.toString().displayWidth() == 1) { "emptyChar must occupy one terminal cell" }
    val clamped = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val barWidth = width
    val filledCount = (clamped * barWidth).toInt().coerceIn(0, barWidth)
    val emptyCount = barWidth - filledCount
    val percent = (clamped * 100).toInt()
    return buildString {
        append('[')
        repeat(filledCount) { append(filledChar) }
        repeat(emptyCount) { append(emptyChar) }
        append(']')
        if (showPercent) {
            append(' ')
            append(percent)
            append('%')
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    width: Int = 20,
    showPercent: Boolean = true,
    filledChar: Char = '█',
    emptyChar: Char = '░',
    modifier: Modifier = Modifier,
) {
    val rendered = buildProgressText(progress, width, showPercent, filledChar, emptyChar)
    val intrinsicWidth = rendered.displayWidth()

    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("ProgressBar").apply { layoutPolicy = LayoutPolicy.LEAF; preferredHeight = 1 } },
        update = {
            reconcile { beginModifierUpdate() }
            set(rendered) { text = it }
            set(intrinsicWidth) { preferredWidth = it }
            reconcile { applyModifier(modifier) }
        }
    )
}

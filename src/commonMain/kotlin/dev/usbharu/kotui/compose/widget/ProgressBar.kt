package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

internal fun buildProgressText(
    progress: Float,
    width: Int,
    showPercent: Boolean,
    filledChar: Char,
    emptyChar: Char,
): String {
    val clamped = progress.coerceIn(0f, 1f)
    val barWidth = width.coerceAtLeast(0)
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
    val intrinsicWidth = rendered.length

    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("ProgressBar").apply { layoutPolicy = LayoutPolicy.LEAF; preferredHeight = 1 } },
        update = {
            set(rendered) { text = it }
            set(intrinsicWidth) { preferredWidth = it }
            set(modifier) { applyModifier(it) }
        }
    )
}

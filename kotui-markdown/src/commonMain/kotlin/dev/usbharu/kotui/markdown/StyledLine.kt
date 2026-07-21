package dev.usbharu.kotui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.displayWidth

@Composable
internal fun StyledLine(
    segments: List<StyledSegment>,
    base: Style = Style(),
    prefix: String = "",
    prefixStyle: Style = base,
    modifier: Modifier = Modifier,
) {
    val sb = StringBuilder()
    val highlights = mutableListOf<TextHighlight>()

    if (prefix.isNotEmpty()) {
        val pw = prefix.displayWidth()
        sb.append(prefix)
        if (prefixStyle != base) {
            highlights.add(TextHighlight(0, pw, prefixStyle))
        }
    }

    var col = sb.toString().displayWidth()
    for (seg in segments) {
        if (seg.text.isEmpty()) continue
        val w = seg.text.displayWidth()
        sb.append(seg.text)
        if (seg.style != base) {
            highlights.add(TextHighlight(col, col + w, seg.style))
        }
        col += w
    }

    val text = sb.toString()
    val hlSnapshot = highlights.toList()
    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("StyledLine").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
            }
        },
        update = {
            reconcile { beginModifierUpdate() }
            set(text) { this.text = it }
            set(base) { this.style = it }
            set(hlSnapshot) { this.textHighlights = it.ifEmpty { null } }
            reconcile { applyModifier(modifier) }
        },
    )
}

package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.displayWidth

private val DEFAULT_BADGE_STYLE = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_BRIGHT_WHITE, bold = true)

@Composable
fun Badge(label: String, modifier: Modifier = Modifier) {
    val rendered = " $label "
    val width = rendered.displayWidth()

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("Badge").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
                style = DEFAULT_BADGE_STYLE
            }
        },
        update = {
            reconcile { beginModifierUpdate() }
            set(rendered) { text = it }
            set(width) { preferredWidth = it }
            reconcile { applyModifier(modifier) }
        }
    )
}

package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.utils.displayWidth

@Composable
fun Divider(char: Char = '─', modifier: Modifier = Modifier) {
    require(char.toString().displayWidth() > 0) { "divider character must be visible" }
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Divider").apply { layoutPolicy = LayoutPolicy.LEAF; preferredHeight = 1 } },
        update = {
            reconcile { beginModifierUpdate() }
            set(char) { fillChar = it }
            reconcile { applyModifier(modifier) }
        }
    )
}

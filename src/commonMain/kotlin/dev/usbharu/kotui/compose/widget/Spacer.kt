package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

@Composable
fun Spacer(modifier: Modifier = Modifier) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Spacer").apply { layoutPolicy = LayoutPolicy.LEAF } },
        update = {
            reconcile { beginModifierUpdate() }
            reconcile { applyModifier(modifier) }
        }
    )
}

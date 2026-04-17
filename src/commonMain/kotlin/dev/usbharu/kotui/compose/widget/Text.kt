package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

@Composable
fun Text(text: String, modifier: Modifier = Modifier) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Text").apply { layoutPolicy = LayoutPolicy.LEAF; preferredHeight = 1 } },
        update = {
            set(text) { this.text = it }
            set(modifier) { applyModifier(it) }
        }
    )
}

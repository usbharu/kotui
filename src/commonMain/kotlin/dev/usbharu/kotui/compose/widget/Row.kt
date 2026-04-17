package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

@Composable
fun Row(modifier: Modifier = Modifier, gap: Int = 2, content: @Composable () -> Unit) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Row").apply { layoutPolicy = LayoutPolicy.ROW } },
        update = {
            set(gap) { layoutGap = it }
            set(modifier) { applyModifier(it) }
        },
        content = content
    )
}

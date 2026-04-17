package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

@Composable
fun Panel(title: String = "", modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Panel").apply { layoutPolicy = LayoutPolicy.COLUMN; drawBorder = true } },
        update = {
            set(title) { borderTitle = it }
            set(modifier) { applyModifier(it) }
        },
        content = content
    )
}

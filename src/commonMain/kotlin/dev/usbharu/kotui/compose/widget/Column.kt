package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

@Composable
fun Column(
    modifier: Modifier = Modifier,
    gap: Int = 0,
    justifyContent: JustifyContent = JustifyContent.Start,
    alignItems: AlignItems = AlignItems.Stretch,
    content: @Composable () -> Unit,
) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Column").apply { layoutPolicy = LayoutPolicy.COLUMN } },
        update = {
            reconcile { beginModifierUpdate() }
            set(gap) { layoutGap = it }
            set(justifyContent) { this.justifyContent = it }
            set(alignItems) { this.alignItems = it }
            reconcile { applyModifier(modifier) }
        },
        content = content,
    )
}

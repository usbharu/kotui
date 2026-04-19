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
fun Row(
    modifier: Modifier = Modifier,
    gap: Int = 2,
    justifyContent: JustifyContent = JustifyContent.Start,
    alignItems: AlignItems = AlignItems.Stretch,
    content: @Composable () -> Unit,
) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Row").apply { layoutPolicy = LayoutPolicy.ROW } },
        update = {
            set(gap) { layoutGap = it }
            set(justifyContent) { this.justifyContent = it }
            set(alignItems) { this.alignItems = it }
            set(modifier) { applyModifier(it) }
        },
        content = content,
    )
}

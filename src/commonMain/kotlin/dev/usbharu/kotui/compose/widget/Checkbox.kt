package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.LocalFocusManager

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val displayText = InteractiveWidgetOps.checkboxText(checked, label, isFocused)
    val toggle: () -> Unit = { onCheckedChange(!checked) }
    val focusStyled = InteractiveWidgetOps.focusStyled(modifier)

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("Checkbox").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
                focusable = true
            }
        },
        update = {
            set(displayText) { text = it }
            set(focusId) { this.focusId = it }
            set(toggle) { cb ->
                onActivate = cb
                onKeyEvent = { ev ->
                    if (InteractiveWidgetOps.isActivate(ev)) {
                        cb()
                        true
                    } else {
                        false
                    }
                }
            }
            set(focusStyled) { applyModifier(it) }
        },
    )
}

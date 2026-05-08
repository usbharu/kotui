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
fun Button(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)
    val displayText = InteractiveWidgetOps.buttonText(label, isFocused)
    val focusStyled = InteractiveWidgetOps.focusStyled(modifier)

    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("Button").apply { layoutPolicy = LayoutPolicy.LEAF; preferredHeight = 1; focusable = true } },
        update = {
            set(displayText) { text = it }
            set(focusId) { this.focusId = it }
            set(onClick) { callback ->
                onActivate = callback
                onKeyEvent = { event ->
                    if (InteractiveWidgetOps.buttonAccepts(event)) {
                        callback()
                        true
                    } else {
                        false
                    }
                }
            }
            set(focusStyled) { applyModifier(it) }
        }
    )
}

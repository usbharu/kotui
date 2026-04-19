package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.modifier.focusedStyle
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

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

    val box = if (checked) "[x]" else "[ ]"
    val tail = if (label.isEmpty()) "" else " $label"
    val prefix = if (isFocused) "▶ " else "  "
    val displayText = prefix + box + tail
    val toggle: () -> Unit = { onCheckedChange(!checked) }

    val focusStyled = modifier
        .style(Style())
        .focusedStyle(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true))

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
                    when {
                        ev.key == Key.ENTER -> { cb(); true }
                        ev.key == Key.CHAR && ev.char == ' ' && !ev.ctrl && !ev.alt -> { cb(); true }
                        else -> false
                    }
                }
            }
            set(focusStyled) { applyModifier(it) }
        },
    )
}

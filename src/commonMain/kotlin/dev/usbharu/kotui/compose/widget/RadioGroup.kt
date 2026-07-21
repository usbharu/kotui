package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.runtime.LocalFocusManager

@Composable
fun <T> RadioGroup(
    options: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
) {
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val selectedIdx = options.indexOf(selected).let { if (it < 0) 0 else it }

    val keyHandler: (KeyEvent) -> Boolean = handler@{ ev ->
        if (options.isEmpty()) return@handler false
        val moved = ListWidgetOps.radioMove(ev, selectedIdx, options.size) ?: return@handler false
        onSelectedChange(options[moved])
        true
    }

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("RadioGroup").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                focusable = true
            }
        },
        update = {
            reconcile { beginModifierUpdate() }
            set(focusId) { this.focusId = it }
            set(options.size) { preferredHeight = it.coerceAtLeast(1) }
            set(keyHandler) { onKeyEvent = it }
            reconcile { applyModifier(modifier) }
        },
        content = {
            if (options.isEmpty()) {
                Text("  (no options)")
            } else {
                options.forEachIndexed { i, opt ->
                    val isCur = i == selectedIdx
                    val prefix = ListWidgetOps.selectedPrefix(isCur, isFocused)
                    val mark = ListWidgetOps.radioMark(isCur)
                    val rowStyle = ListWidgetOps.selectedStyle(isCur, isFocused)
                    Text("$prefix$mark ${optionLabel(opt)}", Modifier.style(rowStyle))
                }
            }
        },
    )
}

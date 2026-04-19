package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

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
        val last = options.lastIndex
        when {
            ev.key == Key.ARROW_UP -> { onSelectedChange(options[(selectedIdx - 1).coerceAtLeast(0)]); true }
            ev.key == Key.ARROW_DOWN -> { onSelectedChange(options[(selectedIdx + 1).coerceAtMost(last)]); true }
            ev.key == Key.HOME -> { onSelectedChange(options[0]); true }
            ev.key == Key.END -> { onSelectedChange(options[last]); true }
            ev.key == Key.CHAR && !ev.ctrl && !ev.alt -> when (ev.char) {
                'k' -> { onSelectedChange(options[(selectedIdx - 1).coerceAtLeast(0)]); true }
                'j' -> { onSelectedChange(options[(selectedIdx + 1).coerceAtMost(last)]); true }
                else -> false
            }
            else -> false
        }
    }

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("RadioGroup").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                focusable = true
            }
        },
        update = {
            set(focusId) { this.focusId = it }
            set(options.size) { preferredHeight = it.coerceAtLeast(1) }
            set(keyHandler) { onKeyEvent = it }
            set(modifier) { applyModifier(it) }
        },
        content = {
            if (options.isEmpty()) {
                Text("  (no options)")
            } else {
                options.forEachIndexed { i, opt ->
                    val isCur = i == selectedIdx
                    val prefix = if (isCur && isFocused) "▶ " else "  "
                    val mark = if (isCur) "(●)" else "( )"
                    val rowStyle = if (isCur && isFocused) {
                        Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
                    } else Style()
                    Text("$prefix$mark ${optionLabel(opt)}", Modifier.style(rowStyle))
                }
            }
        },
    )
}

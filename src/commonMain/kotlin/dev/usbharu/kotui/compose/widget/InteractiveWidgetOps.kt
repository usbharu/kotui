package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.focusedStyle
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

internal object InteractiveWidgetOps {
    val focusedStyle = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
    val emptyStyle = Style()

    fun focusStyled(modifier: Modifier): Modifier =
        modifier.style(emptyStyle).focusedStyle(focusedStyle)

    fun buttonText(label: String, isFocused: Boolean): String =
        if (isFocused) "[ $label ]" else "  $label  "

    fun checkboxText(checked: Boolean, label: String, isFocused: Boolean): String {
        val box = if (checked) "[x]" else "[ ]"
        val tail = if (label.isEmpty()) "" else " $label"
        val prefix = if (isFocused) "▶ " else "  "
        return prefix + box + tail
    }

    fun selectText(label: String, isFocused: Boolean): String {
        val prefix = if (isFocused) "▶ " else "  "
        return "$prefix$label ▾ "
    }

    fun isPlainSpace(ev: KeyEvent): Boolean =
        ev.key == Key.CHAR && ev.char == ' ' && !ev.ctrl && !ev.alt

    fun isActivate(ev: KeyEvent): Boolean =
        ev.key == Key.ENTER || isPlainSpace(ev)

    fun buttonAccepts(ev: KeyEvent): Boolean =
        ev.key == Key.ENTER
}

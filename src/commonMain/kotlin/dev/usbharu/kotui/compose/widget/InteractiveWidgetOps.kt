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

    /** Widget defaults are applied first so caller modifiers remain authoritative. */
    fun focusStyled(modifier: Modifier): Modifier =
        Modifier.style(emptyStyle).focusedStyle(focusedStyle).then(modifier)

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

    fun isPlainSpace(event: KeyEvent): Boolean =
        event.key == Key.CHAR && event.char == ' ' && !event.ctrl && !event.alt

    fun isActivate(event: KeyEvent): Boolean =
        !event.ctrl && !event.alt && (event.key == Key.ENTER || isPlainSpace(event))

    fun buttonAccepts(event: KeyEvent): Boolean = isActivate(event)
}

internal fun interactiveWidgetModifier(modifier: Modifier): Modifier =
    InteractiveWidgetOps.focusStyled(modifier)

internal fun isUnmodifiedActivationKey(event: KeyEvent): Boolean =
    InteractiveWidgetOps.isActivate(event)

internal data class ListWindow(val rows: Int, val scroll: Int, val endExclusive: Int)

internal fun calculateListWindow(
    itemCount: Int,
    selectedIndex: Int,
    visibleRows: Int?,
    previousScroll: Int,
): ListWindow {
    require(itemCount >= 0) { "item count must be non-negative" }
    val rows = (visibleRows ?: itemCount).coerceAtLeast(1)
    if (itemCount == 0) return ListWindow(rows, 0, 0)

    val selected = selectedIndex.coerceIn(0, itemCount - 1)
    val maxScroll = (itemCount.toLong() - rows.toLong()).coerceAtLeast(0L).toInt()
    var scroll = previousScroll.coerceIn(0, maxScroll)
    if (selected < scroll) scroll = selected
    if (selected.toLong() >= scroll.toLong() + rows.toLong()) {
        scroll = (selected.toLong() - rows.toLong() + 1L).coerceAtLeast(0L).toInt()
    }
    scroll = scroll.coerceIn(0, maxScroll)
    val end = (scroll.toLong() + rows.toLong()).coerceAtMost(itemCount.toLong()).toInt()
    return ListWindow(rows, scroll, end)
}

internal fun validCheckedIndices(indices: Set<Int>, itemCount: Int): Set<Int> {
    require(itemCount >= 0) { "item count must be non-negative" }
    return indices.filterTo(linkedSetOf()) { it in 0 until itemCount }
}

/** A TextInput is one terminal row; pasted line breaks must not enter its model. */
internal fun normalizeSingleLinePaste(text: String): String = buildString(text.length) {
    var previousWasCarriageReturn = false
    for (ch in text) {
        when (ch) {
            '\r' -> { append(' '); previousWasCarriageReturn = true }
            '\n' -> { if (!previousWasCarriageReturn) append(' '); previousWasCarriageReturn = false }
            '\t' -> { append(' '); previousWasCarriageReturn = false }
            else -> { if (!ch.isISOControl()) append(ch); previousWasCarriageReturn = false }
        }
    }
}

internal fun requireSingleLineValue(value: String): String {
    require(value.none { it == '\r' || it == '\n' }) { "TextInput value must not contain line breaks" }
    return value
}

internal fun normalizeMultilinePaste(text: String): String = buildString(text.length) {
    var previousWasCarriageReturn = false
    for (ch in text) {
        when (ch) {
            '\r' -> { append('\n'); previousWasCarriageReturn = true }
            '\n' -> { if (!previousWasCarriageReturn) append('\n'); previousWasCarriageReturn = false }
            else -> { append(ch); previousWasCarriageReturn = false }
        }
    }
}

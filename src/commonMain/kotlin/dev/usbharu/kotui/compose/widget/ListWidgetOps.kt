package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

internal data class ListViewport(
    val rows: Int,
    val selectedIndex: Int,
    val scroll: Int,
    val end: Int,
)

internal object ListWidgetOps {
    val focusedRowStyle = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
    val normalRowStyle = Style()

    fun rows(visibleRows: Int?, itemCount: Int): Int =
        (visibleRows ?: itemCount).coerceAtLeast(1)

    fun selectedIndex(index: Int, itemCount: Int): Int =
        index.coerceIn(0, (itemCount - 1).coerceAtLeast(0))

    fun scrollForSelection(itemCount: Int, rows: Int, currentScroll: Int, selectedIndex: Int): Int {
        var scroll = currentScroll
        val maxScroll = (itemCount - rows).coerceAtLeast(0)
        if (itemCount > 0) {
            if (selectedIndex < scroll) scroll = selectedIndex
            if (selectedIndex >= scroll + rows) scroll = selectedIndex - rows + 1
        }
        return scroll.coerceIn(0, maxScroll)
    }

    fun viewport(itemCount: Int, selectedIndex: Int, visibleRows: Int?, currentScroll: Int): ListViewport {
        val rows = rows(visibleRows, itemCount)
        val selected = selectedIndex(selectedIndex, itemCount)
        val scroll = scrollForSelection(itemCount, rows, currentScroll, selected)
        return ListViewport(
            rows = rows,
            selectedIndex = selected,
            scroll = scroll,
            end = (scroll + rows).coerceAtMost(itemCount),
        )
    }

    fun singleSelectMove(ev: KeyEvent, selectedIndex: Int, itemCount: Int, rows: Int): Int? {
        if (itemCount <= 0) return null
        val last = itemCount - 1
        return when {
            ev.key == Key.ARROW_UP -> (selectedIndex - 1).coerceAtLeast(0)
            ev.key == Key.ARROW_DOWN -> (selectedIndex + 1).coerceAtMost(last)
            ev.key == Key.HOME -> 0
            ev.key == Key.END -> last
            ev.key == Key.PAGE_UP -> (selectedIndex - rows).coerceAtLeast(0)
            ev.key == Key.PAGE_DOWN -> (selectedIndex + rows).coerceAtMost(last)
            ev.key == Key.CHAR && !ev.ctrl && !ev.alt -> when (ev.char) {
                'k' -> (selectedIndex - 1).coerceAtLeast(0)
                'j' -> (selectedIndex + 1).coerceAtMost(last)
                'g' -> 0
                'G' -> last
                else -> null
            }
            ev.key == Key.CHAR && ev.ctrl -> when (ev.char) {
                'p' -> (selectedIndex - 1).coerceAtLeast(0)
                'n' -> (selectedIndex + 1).coerceAtMost(last)
                else -> null
            }
            else -> null
        }
    }

    fun radioMove(ev: KeyEvent, selectedIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 0) return null
        val last = itemCount - 1
        return when {
            ev.key == Key.ARROW_UP -> (selectedIndex - 1).coerceAtLeast(0)
            ev.key == Key.ARROW_DOWN -> (selectedIndex + 1).coerceAtMost(last)
            ev.key == Key.HOME -> 0
            ev.key == Key.END -> last
            ev.key == Key.CHAR && !ev.ctrl && !ev.alt -> when (ev.char) {
                'k' -> (selectedIndex - 1).coerceAtLeast(0)
                'j' -> (selectedIndex + 1).coerceAtMost(last)
                else -> null
            }
            else -> null
        }
    }

    fun toggledCheckedIndices(checkedIndices: Set<Int>, index: Int): Set<Int> =
        checkedIndices.toMutableSet().also {
            if (index in it) it.remove(index) else it.add(index)
        }

    fun multiSelectAll(itemCount: Int): Set<Int> =
        (0 until itemCount).toSet()

    fun selectedPrefix(isSelected: Boolean, isFocused: Boolean): String =
        if (isSelected && isFocused) "▶ " else "  "

    fun selectedStyle(isSelected: Boolean, isFocused: Boolean): Style =
        if (isSelected && isFocused) focusedRowStyle else normalRowStyle

    fun multiCursorMark(isCursor: Boolean, isFocused: Boolean): String =
        if (isCursor && isFocused) "▶" else " "

    fun checkMark(checked: Boolean): String =
        if (checked) "[x]" else "[ ]"

    fun radioMark(selected: Boolean): String =
        if (selected) "(●)" else "( )"
}

package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
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
fun <T> SelectableList(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleRows: Int? = null,
    onActivate: ((Int, T) -> Unit)? = null,
    requestInitialFocus: Boolean = false,
    itemLabel: (T) -> String = { it.toString() },
) {
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)
    if (requestInitialFocus) {
        LaunchedEffect(focusId) { focusManager.requestFocus(focusId) }
    }

    val rows = (visibleRows ?: items.size).coerceAtLeast(1)
    val scrollState = remember { mutableStateOf(0) }
    val clampedSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    var scroll = scrollState.value
    val maxScroll = (items.size - rows).coerceAtLeast(0)
    if (items.isNotEmpty()) {
        if (clampedSelected < scroll) scroll = clampedSelected
        if (clampedSelected >= scroll + rows) scroll = clampedSelected - rows + 1
    }
    scroll = scroll.coerceIn(0, maxScroll)
    if (scroll != scrollState.value) scrollState.value = scroll

    val keyHandler: (KeyEvent) -> Boolean = handler@{ ev ->
        if (items.isEmpty()) return@handler false
        val last = items.lastIndex
        when {
            ev.key == Key.ARROW_UP -> { onSelectedIndexChange((clampedSelected - 1).coerceAtLeast(0)); true }
            ev.key == Key.ARROW_DOWN -> { onSelectedIndexChange((clampedSelected + 1).coerceAtMost(last)); true }
            ev.key == Key.HOME -> { onSelectedIndexChange(0); true }
            ev.key == Key.END -> { onSelectedIndexChange(last); true }
            ev.key == Key.PAGE_UP -> { onSelectedIndexChange((clampedSelected - rows).coerceAtLeast(0)); true }
            ev.key == Key.PAGE_DOWN -> { onSelectedIndexChange((clampedSelected + rows).coerceAtMost(last)); true }
            ev.key == Key.ENTER -> { onActivate?.invoke(clampedSelected, items[clampedSelected]); onActivate != null }
            ev.key == Key.CHAR && !ev.ctrl && !ev.alt -> when (ev.char) {
                'k' -> { onSelectedIndexChange((clampedSelected - 1).coerceAtLeast(0)); true }
                'j' -> { onSelectedIndexChange((clampedSelected + 1).coerceAtMost(last)); true }
                'g' -> { onSelectedIndexChange(0); true }
                'G' -> { onSelectedIndexChange(last); true }
                else -> false
            }
            ev.key == Key.CHAR && ev.ctrl -> when (ev.char) {
                'p' -> { onSelectedIndexChange((clampedSelected - 1).coerceAtLeast(0)); true }
                'n' -> { onSelectedIndexChange((clampedSelected + 1).coerceAtMost(last)); true }
                else -> false
            }
            else -> false
        }
    }

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("SelectableList").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                focusable = true
            }
        },
        update = {
            set(focusId) { this.focusId = it }
            set(rows) { preferredHeight = it }
            set(keyHandler) { onKeyEvent = it }
            set(modifier) { applyModifier(it) }
        },
        content = {
            if (items.isEmpty()) {
                Text("  (empty)", Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK)))
            } else {
                val end = (scroll + rows).coerceAtMost(items.size)
                for (i in scroll until end) {
                    val isCursor = i == clampedSelected
                    val prefix = if (isCursor && isFocused) "▶ " else "  "
                    val rowStyle = if (isCursor && isFocused) {
                        Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
                    } else Style()
                    Text(prefix + itemLabel(items[i]), Modifier.style(rowStyle))
                }
            }
        },
    )
}

@Composable
fun <T> MultiSelectList(
    items: List<T>,
    cursorIndex: Int,
    onCursorIndexChange: (Int) -> Unit,
    checkedIndices: Set<Int>,
    onCheckedIndicesChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    visibleRows: Int? = null,
    onActivate: ((Int, T) -> Unit)? = null,
    itemLabel: (T) -> String = { it.toString() },
) {
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val rows = (visibleRows ?: items.size).coerceAtLeast(1)
    val scrollState = remember { mutableStateOf(0) }
    val clampedCursor = cursorIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    var scroll = scrollState.value
    val maxScroll = (items.size - rows).coerceAtLeast(0)
    if (items.isNotEmpty()) {
        if (clampedCursor < scroll) scroll = clampedCursor
        if (clampedCursor >= scroll + rows) scroll = clampedCursor - rows + 1
    }
    scroll = scroll.coerceIn(0, maxScroll)
    if (scroll != scrollState.value) scrollState.value = scroll

    val keyHandler: (KeyEvent) -> Boolean = handler@{ ev ->
        if (items.isEmpty()) return@handler false
        val last = items.lastIndex
        when {
            ev.key == Key.ARROW_UP -> { onCursorIndexChange((clampedCursor - 1).coerceAtLeast(0)); true }
            ev.key == Key.ARROW_DOWN -> { onCursorIndexChange((clampedCursor + 1).coerceAtMost(last)); true }
            ev.key == Key.HOME -> { onCursorIndexChange(0); true }
            ev.key == Key.END -> { onCursorIndexChange(last); true }
            ev.key == Key.PAGE_UP -> { onCursorIndexChange((clampedCursor - rows).coerceAtLeast(0)); true }
            ev.key == Key.PAGE_DOWN -> { onCursorIndexChange((clampedCursor + rows).coerceAtMost(last)); true }
            ev.key == Key.ENTER -> { onActivate?.invoke(clampedCursor, items[clampedCursor]); onActivate != null }
            ev.key == Key.CHAR && ev.char == ' ' && !ev.ctrl && !ev.alt -> {
                val next = checkedIndices.toMutableSet().also { if (clampedCursor in it) it.remove(clampedCursor) else it.add(clampedCursor) }
                onCheckedIndicesChange(next)
                true
            }
            ev.key == Key.CHAR && !ev.ctrl && !ev.alt -> when (ev.char) {
                'k' -> { onCursorIndexChange((clampedCursor - 1).coerceAtLeast(0)); true }
                'j' -> { onCursorIndexChange((clampedCursor + 1).coerceAtMost(last)); true }
                'g' -> { onCursorIndexChange(0); true }
                'G' -> { onCursorIndexChange(last); true }
                else -> false
            }
            ev.key == Key.CHAR && ev.ctrl -> when (ev.char) {
                'p' -> { onCursorIndexChange((clampedCursor - 1).coerceAtLeast(0)); true }
                'n' -> { onCursorIndexChange((clampedCursor + 1).coerceAtMost(last)); true }
                'a' -> { onCheckedIndicesChange(items.indices.toSet()); true }
                'd' -> { onCheckedIndicesChange(emptySet()); true }
                else -> false
            }
            else -> false
        }
    }

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("MultiSelectList").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                focusable = true
            }
        },
        update = {
            set(focusId) { this.focusId = it }
            set(rows) { preferredHeight = it }
            set(keyHandler) { onKeyEvent = it }
            set(modifier) { applyModifier(it) }
        },
        content = {
            if (items.isEmpty()) {
                Text("  (empty)", Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK)))
            } else {
                val end = (scroll + rows).coerceAtMost(items.size)
                for (i in scroll until end) {
                    val isCursor = i == clampedCursor
                    val cursorMark = if (isCursor && isFocused) "▶" else " "
                    val checkMark = if (i in checkedIndices) "[x]" else "[ ]"
                    val rowStyle = if (isCursor && isFocused) {
                        Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
                    } else Style()
                    Text("$cursorMark $checkMark ${itemLabel(items[i])}", Modifier.style(rowStyle))
                }
            }
        },
    )
}

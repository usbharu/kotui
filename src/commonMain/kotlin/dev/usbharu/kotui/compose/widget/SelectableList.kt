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

    val rows = ListWidgetOps.rows(visibleRows, items.size)
    val scrollState = remember { mutableStateOf(0) }
    val viewport = ListWidgetOps.viewport(items.size, selectedIndex, visibleRows, scrollState.value)
    val clampedSelected = viewport.selectedIndex
    if (viewport.scroll != scrollState.value) scrollState.value = viewport.scroll

    val keyHandler: (KeyEvent) -> Boolean = handler@{ ev ->
        if (items.isEmpty()) return@handler false
        val moved = ListWidgetOps.singleSelectMove(ev, clampedSelected, items.size, rows)
        when {
            moved != null -> {
                onSelectedIndexChange(moved)
                true
            }
            ev.key == Key.ENTER -> {
                onActivate?.invoke(clampedSelected, items[clampedSelected])
                onActivate != null
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
                for (i in viewport.scroll until viewport.end) {
                    val isCursor = i == clampedSelected
                    val prefix = ListWidgetOps.selectedPrefix(isCursor, isFocused)
                    val rowStyle = ListWidgetOps.selectedStyle(isCursor, isFocused)
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

    val rows = ListWidgetOps.rows(visibleRows, items.size)
    val scrollState = remember { mutableStateOf(0) }
    val viewport = ListWidgetOps.viewport(items.size, cursorIndex, visibleRows, scrollState.value)
    val clampedCursor = viewport.selectedIndex
    if (viewport.scroll != scrollState.value) scrollState.value = viewport.scroll

    val keyHandler: (KeyEvent) -> Boolean = handler@{ ev ->
        if (items.isEmpty()) return@handler false
        val moved = ListWidgetOps.singleSelectMove(ev, clampedCursor, items.size, rows)
        when {
            moved != null -> {
                onCursorIndexChange(moved)
                true
            }
            ev.key == Key.ENTER -> {
                onActivate?.invoke(clampedCursor, items[clampedCursor])
                onActivate != null
            }
            InteractiveWidgetOps.isPlainSpace(ev) -> {
                val next = ListWidgetOps.toggledCheckedIndices(checkedIndices, clampedCursor)
                onCheckedIndicesChange(next)
                true
            }
            ev.key == Key.CHAR && ev.ctrl -> when (ev.char) {
                'a' -> { onCheckedIndicesChange(ListWidgetOps.multiSelectAll(items.size)); true }
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
                for (i in viewport.scroll until viewport.end) {
                    val isCursor = i == clampedCursor
                    val cursorMark = ListWidgetOps.multiCursorMark(isCursor, isFocused)
                    val checkMark = ListWidgetOps.checkMark(i in checkedIndices)
                    val rowStyle = ListWidgetOps.selectedStyle(isCursor, isFocused)
                    Text("$cursorMark $checkMark ${itemLabel(items[i])}", Modifier.style(rowStyle))
                }
            }
        },
    )
}

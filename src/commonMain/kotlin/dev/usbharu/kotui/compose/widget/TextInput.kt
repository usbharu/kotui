package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.Clipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.size
import dev.usbharu.kotui.compose.modifier.zIndex
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.displayWidth

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    enableEditing: Boolean = true,
    editingFeatures: TextEditingFeatures = TextEditingFeatures.Default,
    inputValidator: TextInputValidator = TextInputValidator.Any,
    completionCandidates: List<String> = emptyList(),
    completionVisibleRows: Int = 5,
    completionShowOnEmptyQuery: Boolean = false,
    completionMatcher: (String, String) -> Boolean = { query, candidate ->
        candidate.startsWith(query, ignoreCase = true)
    },
    completionDisplay: (String) -> String = { it },
    completionTransform: (String, String) -> String = { _, candidate -> candidate },
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val cursorState = remember { mutableStateOf(value.length) }
    val anchorState = remember { mutableStateOf<Int?>(null) }
    val completionSelectionState = remember { mutableStateOf(0) }
    val completionScrollState = remember { mutableStateOf(0) }
    val completionQueryState = remember { mutableStateOf(value) }
    val completionDismissedForValueState = remember { mutableStateOf<String?>(null) }
    val bodyStyleState = remember { mutableStateOf(Style()) }
    val bodyFocusedStyleState = remember { mutableStateOf<Style?>(null) }

    // Keep cursor/anchor within bounds if the caller shrinks `value`.
    val safeCursor = TextEditOps.clampToBoundary(value, cursorState.value.coerceIn(0, value.length))
    if (safeCursor != cursorState.value) cursorState.value = safeCursor
    anchorState.value?.let { a ->
        val safeAnchor = TextEditOps.clampToBoundary(value, a.coerceIn(0, value.length))
        if (safeAnchor != a) anchorState.value = safeAnchor
        if (anchorState.value == cursorState.value) anchorState.value = null
    }

    if (completionQueryState.value != value) {
        completionQueryState.value = value
        completionSelectionState.value = 0
        completionScrollState.value = 0
    }
    completionDismissedForValueState.value?.let { dismissed ->
        if (dismissed != value) completionDismissedForValueState.value = null
    }

    val prefix = if (isFocused) "> " else "  "
    val showPlaceholder = !isFocused && value.isEmpty()
    val displayBody = if (showPlaceholder) placeholder else value
    val displayText = prefix + displayBody

    val cursorPosition = if (isFocused) {
        prefix.displayWidth() + value.substring(0, cursorState.value).displayWidth()
    } else null

    val highlights: List<TextHighlight>? = if (isFocused && editingFeatures.selection) {
        anchorState.value?.let { anchor ->
            val selStart = minOf(anchor, cursorState.value)
            val selEnd = maxOf(anchor, cursorState.value)
            if (selStart == selEnd) null
            else {
                val prefixWidth = prefix.displayWidth()
                val startCol = prefixWidth + value.substring(0, selStart).displayWidth()
                val endCol = prefixWidth + value.substring(0, selEnd).displayWidth()
                listOf(TextHighlight(startCol, endCol, Style(reverse = true)))
            }
        }
    } else null

    val completionWindow = buildCompletionWindow(
        query = value,
        candidates = completionCandidates,
        requestedVisibleRows = completionVisibleRows,
        showOnEmptyQuery = completionShowOnEmptyQuery,
        matcher = completionMatcher,
        selectedIndex = completionSelectionState.value,
        scrollIndex = completionScrollState.value,
    )
    if (completionSelectionState.value != completionWindow.selectedIndex) {
        completionSelectionState.value = completionWindow.selectedIndex
    }
    if (completionScrollState.value != completionWindow.scrollIndex) {
        completionScrollState.value = completionWindow.scrollIndex
    }

    val showCompletion = shouldShowCompletionPopup(
        isFocused = isFocused,
        enableEditing = enableEditing,
        hasCandidates = completionWindow.isVisible,
        value = value,
        dismissedForValue = completionDismissedForValueState.value,
    )

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("TextInput").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                preferredHeight = 1
            }
        },
        update = {
            set(modifier) {
                applyModifier(it)
                bodyStyleState.value = style
                bodyFocusedStyleState.value = focusedStyle
            }
        },
        content = {
            ComposeNode<TuiNode, TuiApplier>(
                factory = {
                    TuiNode("TextInputBody").apply {
                        layoutPolicy = LayoutPolicy.LEAF
                        preferredHeight = 1
                        focusable = true
                    }
                },
                update = {
                    set(displayText) { text = it }
                    set(focusId) { this.focusId = it }
                    set(cursorPosition) { cursorCol = it }
                    set(highlights) { textHighlights = it }
                    set(bodyStyleState.value) { this.style = it }
                    set(bodyFocusedStyleState.value) { this.focusedStyle = it }
                    set(
                        TextInputBindings(
                            value = value,
                            onValueChange = onValueChange,
                            onSubmit = onSubmit,
                            enableEditing = enableEditing,
                            features = editingFeatures,
                            inputValidator = inputValidator,
                            clipboard = clipboard,
                            cursor = cursorState,
                            anchor = anchorState,
                            completionSelection = completionSelectionState,
                            completionScroll = completionScrollState,
                            completionDismissedForValue = completionDismissedForValueState,
                            completionWindow = completionWindow,
                            completionTransform = completionTransform,
                        ),
                    ) { b ->
                        onKeyEvent = { event -> handleKey(b, event) }
                        onPaste = { text ->
                            if (b.enableEditing && b.features.clipboard) {
                                insertText(b, text)
                                true
                            } else false
                        }
                    }
                }
            )
            if (showCompletion) {
                val popupWidth = completionWindow.items
                    .map { completionDisplay(it).displayWidth() }
                    .maxOrNull()
                    ?.coerceAtLeast(1)
                    ?.plus(4)
                    ?: 0
                val popupHeight = completionWindow.visibleRows + 2

                Panel(
                    modifier = Modifier.size(popupWidth, popupHeight).zIndex(20),
                ) {
                    val end = (completionWindow.scrollIndex + completionWindow.visibleRows).coerceAtMost(completionWindow.items.size)
                    for (i in completionWindow.scrollIndex until end) {
                        val candidate = completionWindow.items[i]
                        val isSelected = i == completionWindow.selectedIndex
                        val rowStyle = if (isSelected) {
                            Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
                        } else {
                            Style(fg = Ansi.FG_BRIGHT_BLACK)
                        }
                        val rowPrefix = if (isSelected) "▶ " else "  "
                        Text(rowPrefix + completionDisplay(candidate), Modifier.style(rowStyle))
                    }
                }
            }
        }
    )
}

private data class TextInputBindings(
    val value: String,
    val onValueChange: (String) -> Unit,
    val onSubmit: (() -> Unit)?,
    val enableEditing: Boolean,
    val features: TextEditingFeatures,
    val inputValidator: TextInputValidator,
    val clipboard: Clipboard,
    val cursor: androidx.compose.runtime.MutableState<Int>,
    val anchor: androidx.compose.runtime.MutableState<Int?>,
    val completionSelection: androidx.compose.runtime.MutableState<Int>,
    val completionScroll: androidx.compose.runtime.MutableState<Int>,
    val completionDismissedForValue: androidx.compose.runtime.MutableState<String?>,
    val completionWindow: CompletionWindow,
    val completionTransform: (String, String) -> String,
)

private fun currentSelection(b: TextInputBindings): IntRange? {
    val a = b.anchor.value ?: return null
    val c = b.cursor.value
    if (a == c) return null
    return minOf(a, c)..maxOf(a, c)
}

private fun selectionBounds(b: TextInputBindings): Pair<Int, Int>? {
    val sel = currentSelection(b) ?: return null
    val start = sel.first.coerceIn(0, b.value.length)
    val end = sel.last.coerceIn(start, b.value.length)
    return start to end
}

private fun clearSelection(b: TextInputBindings) {
    b.anchor.value = null
}

private fun ensureAnchor(b: TextInputBindings) {
    if (b.anchor.value == null) b.anchor.value = b.cursor.value
}

private fun moveCursor(b: TextInputBindings, newPos: Int, extendSelection: Boolean) {
    val clamped = newPos.coerceIn(0, b.value.length)
    if (extendSelection && b.features.selection) {
        ensureAnchor(b)
    } else {
        clearSelection(b)
    }
    b.cursor.value = clamped
    // Dropping the selection if it collapses.
    if (b.anchor.value == b.cursor.value) b.anchor.value = null
}

private fun insertText(b: TextInputBindings, insert: String): Boolean {
    val sel = currentSelection(b)
    val (newValue, newCursor) = TextEditOps.replaceIfValid(
        value = b.value,
        cursor = b.cursor.value,
        selection = sel,
        insert = insert,
        inputValidator = b.inputValidator,
    ) ?: return false
    clearSelection(b)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
    return true
}

private fun acceptCompletion(b: TextInputBindings): Boolean {
    if (!b.completionWindow.isVisible) return false
    val candidate = b.completionWindow.items[b.completionWindow.selectedIndex]
    val commit = commitCompletionValueIfValid(
        currentValue = b.value,
        candidate = candidate,
        transform = b.completionTransform,
        inputValidator = b.inputValidator,
    ) ?: return true
    val replacement = commit.replacement
    clearSelection(b)
    b.cursor.value = replacement.length
    b.completionDismissedForValue.value = replacement
    if (replacement != b.value) {
        b.onValueChange(replacement)
    }
    return commit.consumeEnter
}

private fun deleteSelection(b: TextInputBindings): Boolean {
    val (start, end) = selectionBounds(b) ?: return false
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, start, end)
    clearSelection(b)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
    return true
}

private fun deleteBefore(b: TextInputBindings) {
    if (deleteSelection(b)) return
    if (b.cursor.value == 0) return
    val prev = TextEditOps.prevCodePoint(b.value, b.cursor.value)
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, prev, b.cursor.value)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
}

private fun deleteAfter(b: TextInputBindings) {
    if (deleteSelection(b)) return
    if (b.cursor.value >= b.value.length) return
    val next = TextEditOps.nextCodePoint(b.value, b.cursor.value)
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, b.cursor.value, next)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
}

private fun deleteWordBefore(b: TextInputBindings) {
    if (deleteSelection(b)) return
    val target = TextEditOps.prevWordBoundary(b.value, b.cursor.value)
    if (target == b.cursor.value) return
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, target, b.cursor.value)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
}

private fun deleteToLineEnd(b: TextInputBindings) {
    if (b.cursor.value >= b.value.length) return
    val (newValue, _) = TextEditOps.deleteRange(b.value, b.cursor.value, b.value.length)
    clearSelection(b)
    b.onValueChange(newValue)
}

private fun deleteToLineStart(b: TextInputBindings) {
    if (b.cursor.value == 0) return
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, 0, b.cursor.value)
    clearSelection(b)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
}

private fun handleKey(b: TextInputBindings, event: KeyEvent): Boolean {
    // Enter submits regardless of editing state.
    if (event.key == Key.ENTER) {
        if (b.enableEditing && b.completionWindow.isVisible) {
            acceptCompletion(b)
            return true
        }
        b.onSubmit?.invoke()
        return b.onSubmit != null
    }

    if (!b.enableEditing) return false

    if (event.key == Key.ESCAPE && b.completionWindow.isVisible) {
        b.completionDismissedForValue.value = b.value
        return true
    }

    if (b.completionWindow.isVisible) {
        when (event.key) {
            Key.ARROW_UP -> {
                val next = (b.completionSelection.value - 1).coerceAtLeast(0)
                b.completionSelection.value = next
                if (next < b.completionScroll.value) b.completionScroll.value = next
                return true
            }
            Key.ARROW_DOWN -> {
                val last = b.completionWindow.items.lastIndex
                val next = (b.completionSelection.value + 1).coerceAtMost(last)
                b.completionSelection.value = next
                val windowSize = b.completionWindow.visibleRows
                if (next >= b.completionScroll.value + windowSize) {
                    b.completionScroll.value = next - windowSize + 1
                }
                return true
            }
            else -> Unit
        }
    }

    val f = b.features
    val shift = event.shift
    val ctrl = event.ctrl
    val alt = event.alt

    // Cursor movement
    if (f.cursorMovement) {
        when (event.key) {
            Key.ARROW_LEFT -> {
                val target = if ((ctrl || alt) && f.wordNavigation) {
                    TextEditOps.prevWordBoundary(b.value, b.cursor.value)
                } else {
                    TextEditOps.prevCodePoint(b.value, b.cursor.value)
                }
                moveCursor(b, target, shift)
                return true
            }
            Key.ARROW_RIGHT -> {
                val target = if ((ctrl || alt) && f.wordNavigation) {
                    TextEditOps.nextWordBoundary(b.value, b.cursor.value)
                } else {
                    TextEditOps.nextCodePoint(b.value, b.cursor.value)
                }
                moveCursor(b, target, shift)
                return true
            }
            Key.HOME -> { moveCursor(b, 0, shift); return true }
            Key.END -> { moveCursor(b, b.value.length, shift); return true }
            else -> Unit
        }
    }

    // Ctrl+A / Ctrl+E as Home/End equivalents (readline style).
    if (f.cursorMovement && ctrl && event.key == Key.CHAR) {
        when (event.char) {
            'a' -> { moveCursor(b, 0, shift); return true }
            'e' -> { moveCursor(b, b.value.length, shift); return true }
            else -> Unit
        }
    }

    // Alt+b / Alt+f for word navigation
    if (f.wordNavigation && alt && event.key == Key.CHAR) {
        when (event.char) {
            'b' -> { moveCursor(b, TextEditOps.prevWordBoundary(b.value, b.cursor.value), shift); return true }
            'f' -> { moveCursor(b, TextEditOps.nextWordBoundary(b.value, b.cursor.value), shift); return true }
            else -> Unit
        }
    }

    // Deletion
    if (event.key == Key.BACKSPACE) {
        if (alt && f.wordNavigation && f.deletion) {
            deleteWordBefore(b)
        } else {
            deleteBefore(b)
        }
        return true
    }
    if (f.deletion && event.key == Key.DELETE) {
        deleteAfter(b)
        return true
    }
    if (f.deletion && ctrl && event.key == Key.CHAR) {
        when (event.char) {
            'd' -> { deleteAfter(b); return true }
            'k' -> { deleteToLineEnd(b); return true }
            'u' -> { deleteToLineStart(b); return true }
            'w' -> { deleteWordBefore(b); return true }
            else -> Unit
        }
    }

    // Clipboard
    if (f.clipboard && ctrl && event.key == Key.CHAR) {
        when (event.char) {
            'c' -> {
                val (start, end) = selectionBounds(b) ?: return false
                b.clipboard.write(b.value.substring(start, end))
                return true
            }
            'x' -> {
                val (start, end) = selectionBounds(b) ?: return false
                b.clipboard.write(b.value.substring(start, end))
                if (f.deletion) deleteSelection(b)
                return true
            }
            'v' -> {
                val text = b.clipboard.read()
                if (text.isNotEmpty()) insertText(b, text)
                return true
            }
            else -> Unit
        }
    }

    // Regular character insertion. Filter out control chars and any modifier
    // combinations we didn't handle above — letting Ctrl+C etc. fall through as
    // literal text would surprise users.
    if (event.key == Key.CHAR && !ctrl && !alt && !event.char.isISOControl()) {
        insertText(b, event.char.toString())
        return true
    }

    return false
}

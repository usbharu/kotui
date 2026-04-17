package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.Clipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.core.Style
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
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val cursorState = remember { mutableStateOf(value.length) }
    val anchorState = remember { mutableStateOf<Int?>(null) }

    // Keep cursor/anchor within bounds if the caller shrinks `value`.
    val safeCursor = TextEditOps.clampToBoundary(value, cursorState.value.coerceIn(0, value.length))
    if (safeCursor != cursorState.value) cursorState.value = safeCursor
    anchorState.value?.let { a ->
        val safeAnchor = TextEditOps.clampToBoundary(value, a.coerceIn(0, value.length))
        if (safeAnchor != a) anchorState.value = safeAnchor
        if (anchorState.value == cursorState.value) anchorState.value = null
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

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("TextInput").apply {
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
            set(TextInputBindings(value, onValueChange, onSubmit, enableEditing, editingFeatures, clipboard, cursorState, anchorState)) { b ->
                onKeyEvent = { event -> handleKey(b, event) }
                onPaste = { text ->
                    if (b.enableEditing && b.features.clipboard) {
                        insertText(b, text)
                        true
                    } else false
                }
            }
            set(modifier) { applyModifier(it) }
        }
    )
}

private data class TextInputBindings(
    val value: String,
    val onValueChange: (String) -> Unit,
    val onSubmit: (() -> Unit)?,
    val enableEditing: Boolean,
    val features: TextEditingFeatures,
    val clipboard: Clipboard,
    val cursor: androidx.compose.runtime.MutableState<Int>,
    val anchor: androidx.compose.runtime.MutableState<Int?>,
)

private fun currentSelection(b: TextInputBindings): IntRange? {
    val a = b.anchor.value ?: return null
    val c = b.cursor.value
    if (a == c) return null
    return minOf(a, c)..maxOf(a, c)
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

private fun insertText(b: TextInputBindings, insert: String) {
    val sel = currentSelection(b)
    val (newValue, newCursor) = TextEditOps.replace(b.value, b.cursor.value, sel, insert)
    clearSelection(b)
    b.cursor.value = newCursor
    b.onValueChange(newValue)
}

private fun deleteSelection(b: TextInputBindings): Boolean {
    val sel = currentSelection(b) ?: return false
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, sel.first, sel.last)
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
        b.onSubmit?.invoke()
        return b.onSubmit != null
    }

    if (!b.enableEditing) return false

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
                val sel = currentSelection(b) ?: return false
                b.clipboard.write(b.value.substring(sel.first, sel.last))
                return true
            }
            'x' -> {
                val sel = currentSelection(b) ?: return false
                b.clipboard.write(b.value.substring(sel.first, sel.last))
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

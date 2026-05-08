package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.MutableState
import dev.usbharu.kotui.compose.clipboard.Clipboard
import dev.usbharu.kotui.compose.node.TextHighlight
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.displayWidth

internal data class TextInputOpsBindings(
    val value: String,
    val onValueChange: (String) -> Unit,
    val onSubmit: (() -> Unit)?,
    val enableEditing: Boolean,
    val features: TextEditingFeatures,
    val clipboard: Clipboard,
    val cursor: MutableState<Int>,
    val anchor: MutableState<Int?>,
)

internal object TextInputOps {
    fun displayText(value: String, placeholder: String, isFocused: Boolean): String {
        val prefix = if (isFocused) "> " else "  "
        val showPlaceholder = !isFocused && value.isEmpty()
        return prefix + if (showPlaceholder) placeholder else value
    }

    fun cursorPosition(value: String, cursor: Int, isFocused: Boolean): Int? =
        if (isFocused) {
            "> ".displayWidth() + value.substring(0, cursor).displayWidth()
        } else {
            null
        }

    fun highlights(value: String, cursor: Int, anchor: Int?, isFocused: Boolean, selectionEnabled: Boolean): List<TextHighlight>? {
        if (!isFocused || !selectionEnabled || anchor == null) return null
        val selStart = minOf(anchor, cursor)
        val selEnd = maxOf(anchor, cursor)
        if (selStart == selEnd) return null

        val prefixWidth = "> ".displayWidth()
        val startCol = prefixWidth + value.substring(0, selStart).displayWidth()
        val endCol = prefixWidth + value.substring(0, selEnd).displayWidth()
        return listOf(TextHighlight(startCol, endCol, Style(reverse = true)))
    }

    fun currentSelection(b: TextInputOpsBindings): IntRange? {
        val a = b.anchor.value ?: return null
        val c = b.cursor.value
        if (a == c) return null
        return minOf(a, c)..maxOf(a, c)
    }

    fun handleKey(b: TextInputOpsBindings, event: KeyEvent): Boolean {
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
        // combinations we didn't handle above.
        if (event.key == Key.CHAR && !ctrl && !alt && !event.char.isISOControl()) {
            insertText(b, event.char.toString())
            return true
        }

        return false
    }

    fun insertText(b: TextInputOpsBindings, insert: String) {
        val sel = currentSelection(b)
        val (newValue, newCursor) = TextEditOps.replace(b.value, b.cursor.value, sel, insert)
        clearSelection(b)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
    }

    private fun selectionBounds(b: TextInputOpsBindings): Pair<Int, Int>? {
        val sel = currentSelection(b) ?: return null
        val start = sel.first.coerceIn(0, b.value.length)
        val end = sel.last.coerceIn(start, b.value.length)
        return start to end
    }

    private fun clearSelection(b: TextInputOpsBindings) {
        b.anchor.value = null
    }

    private fun ensureAnchor(b: TextInputOpsBindings) {
        if (b.anchor.value == null) b.anchor.value = b.cursor.value
    }

    private fun moveCursor(b: TextInputOpsBindings, newPos: Int, extendSelection: Boolean) {
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

    private fun deleteSelection(b: TextInputOpsBindings): Boolean {
        val (start, end) = selectionBounds(b) ?: return false
        val (newValue, newCursor) = TextEditOps.deleteRange(b.value, start, end)
        clearSelection(b)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
        return true
    }

    private fun deleteBefore(b: TextInputOpsBindings) {
        if (deleteSelection(b)) return
        if (b.cursor.value == 0) return
        val prev = TextEditOps.prevCodePoint(b.value, b.cursor.value)
        val (newValue, newCursor) = TextEditOps.deleteRange(b.value, prev, b.cursor.value)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
    }

    private fun deleteAfter(b: TextInputOpsBindings) {
        if (deleteSelection(b)) return
        if (b.cursor.value >= b.value.length) return
        val next = TextEditOps.nextCodePoint(b.value, b.cursor.value)
        val (newValue, newCursor) = TextEditOps.deleteRange(b.value, b.cursor.value, next)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
    }

    private fun deleteWordBefore(b: TextInputOpsBindings) {
        if (deleteSelection(b)) return
        val target = TextEditOps.prevWordBoundary(b.value, b.cursor.value)
        if (target == b.cursor.value) return
        val (newValue, newCursor) = TextEditOps.deleteRange(b.value, target, b.cursor.value)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
    }

    private fun deleteToLineEnd(b: TextInputOpsBindings) {
        if (b.cursor.value >= b.value.length) return
        val (newValue, _) = TextEditOps.deleteRange(b.value, b.cursor.value, b.value.length)
        clearSelection(b)
        b.onValueChange(newValue)
    }

    private fun deleteToLineStart(b: TextInputOpsBindings) {
        if (b.cursor.value == 0) return
        val (newValue, newCursor) = TextEditOps.deleteRange(b.value, 0, b.cursor.value)
        clearSelection(b)
        b.cursor.value = newCursor
        b.onValueChange(newValue)
    }
}

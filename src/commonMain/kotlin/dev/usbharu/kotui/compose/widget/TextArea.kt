package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.Clipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.weight
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.displayWidth

/**
 * Multi-line text editor. Unlike [TextInput], Enter inserts a newline into
 * [value] and does not submit. [onSubmit] is bound to Ctrl+S so callers can
 * distinguish "commit" from "new line".
 *
 * Value uses `\n` as the line separator. The widget renders one [Text] child
 * per line (inside a COLUMN layout) and positions the terminal cursor on the
 * focused widget using `cursorRow` + `cursorCol`.
 *
 * Layout note: give the caller-supplied [modifier] a `weight` / explicit height
 * so the enclosing layout reserves room for the lines (otherwise the column
 * shrinks to the current line count).
 */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    enableEditing: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val cursorState = remember { mutableStateOf(value.length) }
    val safeCursor = cursorState.value.coerceIn(0, value.length)
    if (safeCursor != cursorState.value) cursorState.value = safeCursor

    // Column-preservation for up/down navigation: remember the visual column
    // we had before a vertical move, so moving through shorter lines and back
    // returns to the original column (like most editors).
    val preferredColState = remember { mutableStateOf<Int?>(null) }

    val (cursorRow, cursorCol) = if (isFocused) {
        TextAreaOps.cursorToRowCol(value, safeCursor)
    } else (null to null)

    val lines = value.split('\n')

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("TextArea").apply {
                layoutPolicy = LayoutPolicy.COLUMN
                focusable = true
            }
        },
        update = {
            set(focusId) { this.focusId = it }
            set(cursorRow) { this.cursorRow = it }
            set(cursorCol) { this.cursorCol = it }
            set(
                TextAreaBindings(
                    value = value,
                    onValueChange = onValueChange,
                    onSubmit = onSubmit,
                    enableEditing = enableEditing,
                    clipboard = clipboard,
                    cursor = cursorState,
                    preferredCol = preferredColState,
                )
            ) { b ->
                onKeyEvent = { event -> handleKey(b, event) }
                onPaste = { text ->
                    if (b.enableEditing) {
                        insertText(b, text)
                        true
                    } else false
                }
            }
            set(modifier) { applyModifier(it) }
        },
        content = {
            // Each logical line becomes a leaf Text child. An empty last line
            // (e.g. value ending in "\n") still needs a visual row, so we emit
            // a space to keep the column layout count stable.
            for (line in lines) {
                Text(if (line.isEmpty()) " " else line)
            }
            // A trailing flex-grow Spacer absorbs any remaining vertical space so
            //   that [layoutColumn]'s "shrink to content" pass does not collapse
            //   the TextArea to its current line count (callers who pass
            //   [Modifier.weight] expect the widget to occupy that space).
            Spacer(Modifier.weight(1f))
        },
    )
}

private data class TextAreaBindings(
    val value: String,
    val onValueChange: (String) -> Unit,
    val onSubmit: (() -> Unit)?,
    val enableEditing: Boolean,
    val clipboard: Clipboard,
    val cursor: androidx.compose.runtime.MutableState<Int>,
    val preferredCol: androidx.compose.runtime.MutableState<Int?>,
)

private fun moveCursor(b: TextAreaBindings, newPos: Int, preserveColumn: Boolean = false) {
    val clamped = newPos.coerceIn(0, b.value.length)
    b.cursor.value = clamped
    if (!preserveColumn) b.preferredCol.value = null
}

private fun insertText(b: TextAreaBindings, insert: String) {
    val (newValue, newCursor) = TextEditOps.replace(b.value, b.cursor.value, null, insert)
    b.cursor.value = newCursor
    b.preferredCol.value = null
    b.onValueChange(newValue)
}

private fun deleteBefore(b: TextAreaBindings) {
    if (b.cursor.value == 0) return
    val prev = TextEditOps.prevCodePoint(b.value, b.cursor.value)
    val (newValue, newCursor) = TextEditOps.deleteRange(b.value, prev, b.cursor.value)
    b.cursor.value = newCursor
    b.preferredCol.value = null
    b.onValueChange(newValue)
}

private fun deleteAfter(b: TextAreaBindings) {
    if (b.cursor.value >= b.value.length) return
    val next = TextEditOps.nextCodePoint(b.value, b.cursor.value)
    val (newValue, _) = TextEditOps.deleteRange(b.value, b.cursor.value, next)
    b.preferredCol.value = null
    b.onValueChange(newValue)
}

private fun handleKey(b: TextAreaBindings, event: KeyEvent): Boolean {
    // Ctrl+S commits. We surface it whether or not editing is enabled so that
    // read-only views can still be "submitted" (matches TextInput's Enter).
    if (event.ctrl && event.key == Key.CHAR && event.char == 's') {
        b.onSubmit?.invoke()
        return b.onSubmit != null
    }

    if (!b.enableEditing) return false

    val ctrl = event.ctrl
    val alt = event.alt

    // Cursor movement — up/down preserve the *visual* column across lines.
    when (event.key) {
        Key.ARROW_LEFT -> {
            val target = if (ctrl || alt) {
                TextEditOps.prevWordBoundary(b.value, b.cursor.value)
            } else {
                TextEditOps.prevCodePoint(b.value, b.cursor.value)
            }
            moveCursor(b, target)
            return true
        }
        Key.ARROW_RIGHT -> {
            val target = if (ctrl || alt) {
                TextEditOps.nextWordBoundary(b.value, b.cursor.value)
            } else {
                TextEditOps.nextCodePoint(b.value, b.cursor.value)
            }
            moveCursor(b, target)
            return true
        }
        Key.ARROW_UP -> {
            val (curRow, curCol) = TextAreaOps.cursorToRowCol(b.value, b.cursor.value)
            val col = b.preferredCol.value ?: curCol
            b.preferredCol.value = col
            if (curRow == 0) {
                moveCursor(b, 0, preserveColumn = true)
            } else {
                b.cursor.value = TextAreaOps.rowColToCursor(b.value, curRow - 1, col)
            }
            return true
        }
        Key.ARROW_DOWN -> {
            val (curRow, curCol) = TextAreaOps.cursorToRowCol(b.value, b.cursor.value)
            val col = b.preferredCol.value ?: curCol
            b.preferredCol.value = col
            val lastRow = b.value.count { it == '\n' }
            if (curRow == lastRow) {
                moveCursor(b, b.value.length, preserveColumn = true)
            } else {
                b.cursor.value = TextAreaOps.rowColToCursor(b.value, curRow + 1, col)
            }
            return true
        }
        Key.HOME -> { moveCursor(b, TextAreaOps.lineStart(b.value, b.cursor.value)); return true }
        Key.END -> { moveCursor(b, TextAreaOps.lineEnd(b.value, b.cursor.value)); return true }
        Key.ENTER -> { insertText(b, "\n"); return true }
        Key.BACKSPACE -> { deleteBefore(b); return true }
        Key.DELETE -> { deleteAfter(b); return true }
        else -> Unit
    }

    // Ctrl+A / Ctrl+E as line-start / line-end (readline style).
    if (ctrl && event.key == Key.CHAR) {
        when (event.char) {
            'a' -> { moveCursor(b, TextAreaOps.lineStart(b.value, b.cursor.value)); return true }
            'e' -> { moveCursor(b, TextAreaOps.lineEnd(b.value, b.cursor.value)); return true }
            'd' -> { deleteAfter(b); return true }
            'k' -> {
                val end = TextAreaOps.lineEnd(b.value, b.cursor.value)
                if (end == b.cursor.value && b.cursor.value < b.value.length) {
                    // On the newline itself — delete the newline (join lines).
                    val (newValue, _) = TextEditOps.deleteRange(b.value, b.cursor.value, b.cursor.value + 1)
                    b.onValueChange(newValue)
                } else {
                    val (newValue, _) = TextEditOps.deleteRange(b.value, b.cursor.value, end)
                    b.onValueChange(newValue)
                }
                b.preferredCol.value = null
                return true
            }
            'u' -> {
                val start = TextAreaOps.lineStart(b.value, b.cursor.value)
                val (newValue, newCursor) = TextEditOps.deleteRange(b.value, start, b.cursor.value)
                b.cursor.value = newCursor
                b.preferredCol.value = null
                b.onValueChange(newValue)
                return true
            }
            'w' -> {
                val target = TextEditOps.prevWordBoundary(b.value, b.cursor.value)
                if (target == b.cursor.value) return true
                val (newValue, newCursor) = TextEditOps.deleteRange(b.value, target, b.cursor.value)
                b.cursor.value = newCursor
                b.preferredCol.value = null
                b.onValueChange(newValue)
                return true
            }
            'c' -> return false // no selection model yet; let it fall through
            'v' -> {
                val text = b.clipboard.read()
                if (text.isNotEmpty()) insertText(b, text)
                return true
            }
            else -> Unit
        }
    }

    // Regular char insertion — skip combinations we didn't handle above.
    if (event.key == Key.CHAR && !ctrl && !alt && !event.char.isISOControl()) {
        insertText(b, event.char.toString())
        return true
    }

    return false
}

/**
 * Line/column helpers for newline-separated text buffers. Indices are UTF-16
 * char offsets (same convention as [TextEditOps]). A cursor at the boundary
 * between lines (exactly at a `\n` position) reports the *trailing* line — i.e.
 * cursor before `\n` is at end of previous line, not start of next. Columns are
 * expressed in terminal display cells (`displayWidth`) to match where the
 * terminal cursor is drawn for wide characters.
 */
internal object TextAreaOps {
    fun lineStart(value: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, value.length)
        var i = c
        while (i > 0 && value[i - 1] != '\n') i--
        return i
    }

    fun lineEnd(value: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, value.length)
        var i = c
        while (i < value.length && value[i] != '\n') i++
        return i
    }

    /** Returns (row, col) of the cursor, col expressed in terminal display cells. */
    fun cursorToRowCol(value: String, cursor: Int): Pair<Int, Int> {
        val c = cursor.coerceIn(0, value.length)
        var row = 0
        var lineStart = 0
        for (i in 0 until c) {
            if (value[i] == '\n') {
                row++
                lineStart = i + 1
            }
        }
        val col = value.substring(lineStart, c).displayWidth()
        return row to col
    }

    /**
     * Inverse of [cursorToRowCol]. [targetCol] is in display cells; if the row
     * is shorter than targetCol, clamps to the end of that line. Never crosses
     * line boundaries in either direction.
     */
    fun rowColToCursor(value: String, targetRow: Int, targetCol: Int): Int {
        if (targetRow < 0) return 0
        var row = 0
        var lineStart = 0
        var i = 0
        while (i < value.length && row < targetRow) {
            if (value[i] == '\n') {
                row++
                lineStart = i + 1
            }
            i++
        }
        if (row < targetRow) return value.length
        // Walk forward from lineStart until we've consumed targetCol cells, or
        // hit a newline / EOF.
        var col = 0
        var p = lineStart
        while (p < value.length && value[p] != '\n' && col < targetCol) {
            val next = TextEditOps.nextCodePoint(value, p)
            val w = value.substring(p, next).displayWidth()
            if (col + w > targetCol) break
            col += w
            p = next
        }
        return p
    }
}

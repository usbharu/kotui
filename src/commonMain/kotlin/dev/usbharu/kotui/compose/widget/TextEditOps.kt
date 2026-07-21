package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.utils.nextTerminalGraphemeBoundary
import dev.usbharu.kotui.utils.previousTerminalGraphemeBoundary
import dev.usbharu.kotui.utils.terminalGraphemeStart

/**
 * Pure text-editing helpers shared by [TextInput] and tests. All indices are UTF-16
 * char indices (as returned by [String.length]), constrained to terminal grapheme boundaries.
 * Surrogate pairs, combining sequences, emoji modifiers, flags, and ZWJ sequences move as one unit.
 */
internal object TextEditOps {

    fun clampToBoundary(value: String, index: Int): Int {
        return value.terminalGraphemeStart(index)
    }

    fun nextCodePoint(value: String, index: Int): Int {
        return value.nextTerminalGraphemeBoundary(index)
    }

    fun prevCodePoint(value: String, index: Int): Int {
        return value.previousTerminalGraphemeBoundary(index)
    }

    // Word = run of [A-Za-z0-9_]. Non-ASCII letters (incl. CJK) are treated as single-cp words.
    private fun isWordChar(cp: Int): Boolean {
        if (cp == '_'.code) return true
        return cp <= Char.MAX_VALUE.code && cp.toChar().isLetterOrDigit()
    }

    private fun codePointAt(value: String, index: Int): Int {
        val c = value[index]
        return if (c.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate()) {
            0x10000 + ((c.code - 0xD800) shl 10) + (value[index + 1].code - 0xDC00)
        } else {
            c.code
        }
    }

    fun nextWordBoundary(value: String, from: Int): Int {
        var i = clampToBoundary(value, from)
        // Skip any run of non-word chars.
        while (i < value.length && !isWordChar(codePointAt(value, i))) {
            i = nextCodePoint(value, i)
        }
        // Skip the subsequent run of word chars.
        while (i < value.length && isWordChar(codePointAt(value, i))) {
            i = nextCodePoint(value, i)
        }
        return i
    }

    fun prevWordBoundary(value: String, from: Int): Int {
        var i = clampToBoundary(value, from)
        // Skip non-word chars going left.
        while (i > 0) {
            val prev = prevCodePoint(value, i)
            if (isWordChar(codePointAt(value, prev))) break
            i = prev
        }
        // Skip the word chars going left.
        while (i > 0) {
            val prev = prevCodePoint(value, i)
            if (!isWordChar(codePointAt(value, prev))) break
            i = prev
        }
        return i
    }

    /** Returns (newValue, newCursor). Replaces [selection] (if non-null) with [insert]. */
    fun replace(value: String, cursor: Int, selection: IntRange?, insert: String): Pair<String, Int> {
        return if (selection != null) {
            val start = clampToBoundary(value, selection.first)
            val end = clampToBoundary(value, selection.last).coerceAtLeast(start)
            val newValue = value.substring(0, start) + insert + value.substring(end)
            newValue to (start + insert.length)
        } else {
            val c = clampToBoundary(value, cursor)
            val newValue = value.substring(0, c) + insert + value.substring(c)
            newValue to (c + insert.length)
        }
    }

    fun replaceIfValid(
        value: String,
        cursor: Int,
        selection: IntRange?,
        insert: String,
        inputValidator: TextInputValidator,
    ): Pair<String, Int>? {
        val replacement = replace(value, cursor, selection, insert)
        return replacement.takeIf { (newValue, _) -> inputValidator.isValid(newValue) }
    }

    fun deleteRange(value: String, start: Int, end: Int): Pair<String, Int> {
        val s = clampToBoundary(value, start)
        val e = clampToBoundary(value, end).coerceAtLeast(s)
        return value.substring(0, s) + value.substring(e) to s
    }
}

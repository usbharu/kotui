package dev.usbharu.kotui.compose.widget

/**
 * Pure text-editing helpers shared by [TextInput] and tests. All indices are UTF-16
 * char indices (as returned by [String.length]), constrained to code-point boundaries.
 * Wide code points and surrogate pairs are moved as a single unit.
 */
internal object TextEditOps {

    fun clampToBoundary(value: String, index: Int): Int {
        val i = index.coerceIn(0, value.length)
        if (i in 1 until value.length && value[i].isLowSurrogate() && value[i - 1].isHighSurrogate()) {
            return i - 1
        }
        return i
    }

    fun nextCodePoint(value: String, index: Int): Int {
        if (index >= value.length) return value.length
        val c = value[index]
        return if (c.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate()) {
            index + 2
        } else {
            index + 1
        }
    }

    fun prevCodePoint(value: String, index: Int): Int {
        if (index <= 0) return 0
        val c = value[index - 1]
        return if (c.isLowSurrogate() && index - 2 >= 0 && value[index - 2].isHighSurrogate()) {
            index - 2
        } else {
            index - 1
        }
    }

    // Word = run of [A-Za-z0-9_]. Non-ASCII letters (incl. CJK) are treated as single-cp words.
    private fun isWordChar(cp: Int): Boolean {
        if (cp in 'a'.code..'z'.code) return true
        if (cp in 'A'.code..'Z'.code) return true
        if (cp in '0'.code..'9'.code) return true
        if (cp == '_'.code) return true
        return false
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
        var i = from
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
        var i = from
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
            val start = selection.first.coerceIn(0, value.length)
            val end = selection.last.coerceIn(start, value.length)
            val newValue = value.substring(0, start) + insert + value.substring(end)
            newValue to (start + insert.length)
        } else {
            val c = cursor.coerceIn(0, value.length)
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
        val s = start.coerceAtLeast(0).coerceAtMost(value.length)
        val e = end.coerceAtLeast(s).coerceAtMost(value.length)
        return value.substring(0, s) + value.substring(e) to s
    }
}

package dev.usbharu.kotui.compose.runtime

sealed interface InputEvent

enum class Key {
    CHAR, TAB, ESCAPE, ENTER, BACKSPACE,
    ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
    HOME, END, DELETE, PAGE_UP, PAGE_DOWN,
    UNKNOWN
}

data class KeyEvent(
    val char: Char,
    val key: Key,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) : InputEvent {
    companion object {
        fun fromChar(ch: Char): KeyEvent {
            val key = classify(ch)
            if (key != Key.CHAR) return KeyEvent(ch, key)
            return when (ch.code) {
                0 -> KeyEvent(' ', Key.CHAR, ctrl = true)
                in 0x01..0x1A -> KeyEvent('a' + (ch.code - 1), Key.CHAR, ctrl = true)
                in 0x1C..0x1F -> KeyEvent("\\]^_"[ch.code - 0x1C], Key.CHAR, ctrl = true)
                else -> KeyEvent(ch, Key.CHAR)
            }
        }

        private fun classify(ch: Char): Key = when (ch) {
            '\t' -> Key.TAB
            '\r', '\n' -> Key.ENTER
            '\u001B' -> Key.ESCAPE
            '\u007F', '\b' -> Key.BACKSPACE
            else -> Key.CHAR
        }
    }
}

data class PasteEvent(val text: String) : InputEvent

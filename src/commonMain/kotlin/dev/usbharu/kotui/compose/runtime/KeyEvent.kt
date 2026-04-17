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
        fun fromChar(ch: Char): KeyEvent = KeyEvent(ch, classify(ch))

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

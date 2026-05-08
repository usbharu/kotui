package dev.usbharu.kotui.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyEventTest {
    @Test
    fun fromCharClassifiesControlCharacters() {
        assertEquals(Key.TAB, KeyEvent.fromChar('\t').key)
        assertEquals(Key.ENTER, KeyEvent.fromChar('\r').key)
        assertEquals(Key.ENTER, KeyEvent.fromChar('\n').key)
        assertEquals(Key.ESCAPE, KeyEvent.fromChar('\u001B').key)
        assertEquals(Key.BACKSPACE, KeyEvent.fromChar('\u007F').key)
        assertEquals(Key.BACKSPACE, KeyEvent.fromChar('\b').key)
        assertEquals(Key.CHAR, KeyEvent.fromChar('x').key)
    }

    @Test
    fun pasteEventStoresText() {
        assertEquals("pasted", PasteEvent("pasted").text)
    }
}

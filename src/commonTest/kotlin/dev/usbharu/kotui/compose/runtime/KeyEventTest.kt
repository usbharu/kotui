package dev.usbharu.kotui.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyEventTest {
    @Test
    fun fromCharPreservesShortcutMeaningOfAsciiControls() {
        val ctrlA = KeyEvent.fromChar('\u0001')
        val ctrlSpace = KeyEvent.fromChar('\u0000')
        val ctrlBackslash = KeyEvent.fromChar('\u001C')

        assertEquals('a', ctrlA.char)
        assertEquals(' ', ctrlSpace.char)
        assertEquals('\\', ctrlBackslash.char)
        assertTrue(ctrlA.ctrl && ctrlSpace.ctrl && ctrlBackslash.ctrl)
    }

    @Test
    fun fromCharStillClassifiesSemanticControlsFirst() {
        assertEquals(Key.TAB, KeyEvent.fromChar('\t').key)
        assertEquals(Key.ENTER, KeyEvent.fromChar('\r').key)
        assertEquals(Key.ESCAPE, KeyEvent.fromChar('\u001B').key)
        assertEquals(Key.BACKSPACE, KeyEvent.fromChar('\u007F').key)
    }

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

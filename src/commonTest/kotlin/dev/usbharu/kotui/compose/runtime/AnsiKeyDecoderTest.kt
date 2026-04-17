package dev.usbharu.kotui.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnsiKeyDecoderTest {

    private fun decode(input: String, flushAtEnd: Boolean = true): List<InputEvent> {
        val decoder = AnsiKeyDecoder()
        val out = mutableListOf<InputEvent>()
        for (c in input) out += decoder.feed(c)
        if (flushAtEnd) out += decoder.flush()
        return out
    }

    @Test
    fun plainAscii() {
        val events = decode("abc")
        assertEquals(3, events.size)
        events.forEachIndexed { i, e ->
            val k = e as KeyEvent
            assertEquals(Key.CHAR, k.key)
            assertEquals("abc"[i], k.char)
            assertEquals(false, k.ctrl)
        }
    }

    @Test
    fun tabEnterBackspace() {
        val events = decode("\t\r\u007F")
        assertEquals(3, events.size)
        assertEquals(Key.TAB, (events[0] as KeyEvent).key)
        assertEquals(Key.ENTER, (events[1] as KeyEvent).key)
        assertEquals(Key.BACKSPACE, (events[2] as KeyEvent).key)
    }

    @Test
    fun ctrlLetter() {
        val events = decode("\u0001\u0003\u0016") // Ctrl+A, Ctrl+C, Ctrl+V
        assertEquals(3, events.size)
        val a = events[0] as KeyEvent
        assertEquals('a', a.char); assertTrue(a.ctrl); assertEquals(Key.CHAR, a.key)
        val c = events[1] as KeyEvent
        assertEquals('c', c.char); assertTrue(c.ctrl)
        val v = events[2] as KeyEvent
        assertEquals('v', v.char); assertTrue(v.ctrl)
    }

    @Test
    fun arrowKeys() {
        val events = decode("\u001B[A\u001B[B\u001B[C\u001B[D")
        assertEquals(4, events.size)
        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.ARROW_DOWN, (events[1] as KeyEvent).key)
        assertEquals(Key.ARROW_RIGHT, (events[2] as KeyEvent).key)
        assertEquals(Key.ARROW_LEFT, (events[3] as KeyEvent).key)
    }

    @Test
    fun shiftArrow() {
        val events = decode("\u001B[1;2C")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals(Key.ARROW_RIGHT, k.key)
        assertTrue(k.shift)
        assertEquals(false, k.ctrl)
        assertEquals(false, k.alt)
    }

    @Test
    fun ctrlArrow() {
        val events = decode("\u001B[1;5D")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals(Key.ARROW_LEFT, k.key)
        assertTrue(k.ctrl)
        assertEquals(false, k.shift)
    }

    @Test
    fun homeEndDelete() {
        val events = decode("\u001B[H\u001B[F\u001B[3~")
        assertEquals(3, events.size)
        assertEquals(Key.HOME, (events[0] as KeyEvent).key)
        assertEquals(Key.END, (events[1] as KeyEvent).key)
        assertEquals(Key.DELETE, (events[2] as KeyEvent).key)
    }

    @Test
    fun homeEndTildeForm() {
        val events = decode("\u001B[1~\u001B[4~")
        assertEquals(2, events.size)
        assertEquals(Key.HOME, (events[0] as KeyEvent).key)
        assertEquals(Key.END, (events[1] as KeyEvent).key)
    }

    @Test
    fun pageUpDown() {
        val events = decode("\u001B[5~\u001B[6~")
        assertEquals(Key.PAGE_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.PAGE_DOWN, (events[1] as KeyEvent).key)
    }

    @Test
    fun ss3ArrowKeys() {
        val events = decode("\u001BOA\u001BOH")
        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.HOME, (events[1] as KeyEvent).key)
    }

    @Test
    fun escapeAloneViaFlush() {
        val events = decode("\u001B")
        assertEquals(1, events.size)
        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
    }

    @Test
    fun altCharViaEsc() {
        val events = decode("\u001Bf")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals('f', k.char)
        assertTrue(k.alt)
        assertEquals(Key.CHAR, k.key)
    }

    @Test
    fun bracketedPaste() {
        val events = decode("\u001B[200~hello world\u001B[201~")
        assertEquals(1, events.size)
        val p = events[0] as PasteEvent
        assertEquals("hello world", p.text)
    }

    @Test
    fun bracketedPasteAroundText() {
        val events = decode("a\u001B[200~xy\u001B[201~b")
        assertEquals(3, events.size)
        assertEquals('a', (events[0] as KeyEvent).char)
        assertEquals("xy", (events[1] as PasteEvent).text)
        assertEquals('b', (events[2] as KeyEvent).char)
    }
}

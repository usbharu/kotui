package dev.usbharu.kotui.compose.clipboard

import kotlin.test.Test
import kotlin.test.assertEquals

class ClipboardTest {
    @Test
    fun inMemoryClipboardReadsAndWritesContents() {
        val clipboard = InMemoryClipboard("initial")

        assertEquals("initial", clipboard.read())
        clipboard.write("updated")
        assertEquals("updated", clipboard.read())
    }

    @Test
    fun systemClipboardAlwaysUpdatesMemoryWhenSideEffectsAreDisabled() {
        val memory = InMemoryClipboard()
        val clipboard = SystemClipboard(memory, emitOsc52 = false, useSystemHelper = false)

        clipboard.write("copied")

        assertEquals("copied", clipboard.read())
        assertEquals("copied", memory.read())
    }

    @Test
    fun systemClipboardCanEmitOsc52WithoutSystemHelper() {
        val memory = InMemoryClipboard()
        val clipboard = SystemClipboard(memory, emitOsc52 = true, useSystemHelper = false)

        clipboard.write("remote")

        assertEquals("remote", clipboard.read())
        assertEquals("remote", memory.read())
    }
}

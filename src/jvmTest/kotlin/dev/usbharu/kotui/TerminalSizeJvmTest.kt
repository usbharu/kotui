package dev.usbharu.kotui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalSizeJvmTest {
    @Test
    fun sttyOutputAcceptsArbitraryWhitespace() {
        assertEquals(TerminalSize(120, 40), parseTerminalSizeOutput("  40   120  "))
        assertEquals(TerminalSize(90, 30), parseTerminalSizeOutput("30\t90"))
    }

    @Test
    fun sttyOutputRejectsNonPositiveOrExtraFields() {
        assertNull(parseTerminalSizeOutput("0 80"))
        assertNull(parseTerminalSizeOutput("24 80 extra"))
    }
}

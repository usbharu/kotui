package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnsiTest {
    @Test
    fun cursorPositionRejectsZeroAndNegativeCoordinates() {
        assertFailsWith<IllegalArgumentException> { Ansi.cursorTo(0, 1) }
        assertFailsWith<IllegalArgumentException> { Ansi.cursorTo(1, -1) }
        assertEquals("\u001B[2;3H", Ansi.cursorTo(2, 3))
    }

    @Test
    fun cursorMovementRejectsNegativeDistances() {
        assertFailsWith<IllegalArgumentException> { Ansi.cursorUp(-1) }
        assertFailsWith<IllegalArgumentException> { Ansi.cursorDown(-1) }
        assertFailsWith<IllegalArgumentException> { Ansi.cursorForward(-1) }
        assertFailsWith<IllegalArgumentException> { Ansi.cursorBack(-1) }
    }

    @Test
    fun indexedColorsRejectValuesOutsideByteRange() {
        assertFailsWith<IllegalArgumentException> { Ansi.fg256(-1) }
        assertFailsWith<IllegalArgumentException> { Ansi.bg256(256) }
        assertEquals("\u001B[38;5;255m", Ansi.fg256(255))
    }

    @Test
    fun rgbColorsRejectInvalidComponents() {
        assertFailsWith<IllegalArgumentException> { Ansi.fgRgb(-1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { Ansi.fgRgb(0, 256, 0) }
        assertFailsWith<IllegalArgumentException> { Ansi.bgRgb(0, 0, 999) }
    }
}

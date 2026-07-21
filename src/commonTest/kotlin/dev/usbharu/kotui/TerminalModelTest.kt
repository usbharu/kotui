package dev.usbharu.kotui

import dev.usbharu.kotui.layout.Constraints
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalModelTest {
    @Test
    fun terminalSizeRejectsImpossibleDimensions() {
        assertFailsWith<IllegalArgumentException> { TerminalSize(0, 24) }
        assertFailsWith<IllegalArgumentException> { TerminalSize(80, -1) }
    }

    @Test
    fun constraintsRejectNegativeBounds() {
        assertFailsWith<IllegalArgumentException> { Constraints(-1, 1) }
        assertFailsWith<IllegalArgumentException> { Constraints(1, 1, minHeight = -1) }
    }

    @Test
    fun constraintsRejectMinimumAboveMaximum() {
        assertFailsWith<IllegalArgumentException> { Constraints(maxWidth = 3, maxHeight = 4, minWidth = 4) }
        assertFailsWith<IllegalArgumentException> { Constraints(maxWidth = 3, maxHeight = 4, minHeight = 5) }
    }

    @Test
    fun resizeTrackerSuppressesDuplicateAndUnknownSizes() {
        val tracker = TerminalSizeChangeTracker(TerminalSize(80, 24))

        assertNull(tracker.changedTo(TerminalSize(80, 24)))
        assertNull(tracker.changedTo(null))
        assertEquals(TerminalSize(100, 30), tracker.changedTo(TerminalSize(100, 30)))
        assertNull(tracker.changedTo(TerminalSize(100, 30)))
    }
}

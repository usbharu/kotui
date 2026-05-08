package dev.usbharu.kotui.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectTest {
    @Test
    fun containsUsesInclusiveStartAndExclusiveEnd() {
        val rect = Rect(2, 3, 4, 5)

        assertTrue(rect.contains(2, 3))
        assertTrue(rect.contains(5, 7))
        assertFalse(rect.contains(6, 7))
        assertFalse(rect.contains(5, 8))
        assertFalse(rect.contains(1, 3))
        assertFalse(rect.contains(2, 2))
    }

    @Test
    fun zeroRectContainsNoCells() {
        assertFalse(Rect.ZERO.contains(0, 0))
    }
}

package dev.usbharu.kotui.image

import kotlin.test.Test
import kotlin.test.assertEquals

class TargetSizeTest {

    @Test
    fun keeps_small_images_as_is() {
        val t = computeTargetSize(100, 50, maxWidth = 800, maxHeight = 600)
        assertEquals(100, t.width)
        assertEquals(50, t.height)
    }

    @Test
    fun scales_down_preserving_aspect_ratio() {
        val t = computeTargetSize(800, 400, maxWidth = 200, maxHeight = 200)
        assertEquals(200, t.width)
        assertEquals(100, t.height)
    }

    @Test
    fun null_limits_are_treated_as_unbounded() {
        val t = computeTargetSize(4000, 2000, maxWidth = null, maxHeight = null)
        assertEquals(4000, t.width)
        assertEquals(2000, t.height)
    }

    @Test
    fun non_positive_limit_is_treated_as_unbounded() {
        val t = computeTargetSize(300, 300, maxWidth = 0, maxHeight = -1)
        assertEquals(300, t.width)
        assertEquals(300, t.height)
    }
}

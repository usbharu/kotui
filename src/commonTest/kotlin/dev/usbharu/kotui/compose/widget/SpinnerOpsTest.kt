package dev.usbharu.kotui.compose.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class SpinnerOpsTest {
    @Test
    fun spinnerFrameWrapsPositiveAndNegativeFrames() {
        val chars = listOf('|', '/', '-', '\\')

        assertEquals("|", spinnerFrame(0, chars))
        assertEquals("/", spinnerFrame(1, chars))
        assertEquals("|", spinnerFrame(4, chars))
        assertEquals("\\", spinnerFrame(-1, chars))
        assertEquals("-", spinnerFrame(-2, chars))
    }

    @Test
    fun spinnerFrameFallsBackToBlankForEmptyFrames() {
        assertEquals(" ", spinnerFrame(99, emptyList()))
    }
}

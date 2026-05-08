package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Base64Test {
    @Test
    fun encodesTextAndByteArrayRemainders() {
        assertEquals("", Base64.encode(""))
        assertEquals("Zg==", Base64.encode("f"))
        assertEquals("Zm8=", Base64.encode("fo"))
        assertEquals("Zm9v", Base64.encode("foo"))
        assertEquals("Zm9vYg==", Base64.encode("foob"))
    }

    @Test
    fun encodesByteArraySlices() {
        val bytes = byteArrayOf(0, 'f'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte(), 0)

        assertEquals("Zm9v", Base64.encode(bytes, offset = 1, length = 3))
        assertEquals("Zg==", Base64.encode(bytes, offset = 1, length = 1))
        assertEquals("Zm8=", Base64.encode(bytes, offset = 1, length = 2))
    }

    @Test
    fun rejectsInvalidSliceBounds() {
        val bytes = byteArrayOf(1, 2, 3)

        assertFailsWith<IllegalArgumentException> { Base64.encode(bytes, offset = -1) }
        assertFailsWith<IllegalArgumentException> { Base64.encode(bytes, offset = 0, length = -1) }
        assertFailsWith<IllegalArgumentException> { Base64.encode(bytes, offset = 2, length = 2) }
    }
}

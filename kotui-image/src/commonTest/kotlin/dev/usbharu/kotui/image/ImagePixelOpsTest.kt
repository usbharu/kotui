package dev.usbharu.kotui.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImagePixelOpsTest {
    @Test
    fun pngDetectionChecksTheCompleteEightByteSignature() {
        val valid = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals(true, hasPngSignature(valid))

        val falsePositive = valid.copyOf().also { it[7] = 0 }
        assertEquals(false, hasPngSignature(falsePositive))
    }

    @Test
    fun rgbaByteCountRejectsMultiplicationOverflow() {
        assertEquals(16, rgbaByteCount(2, 2))
        assertFailsWith<IllegalArgumentException> { rgbaByteCount(Int.MAX_VALUE, 2) }
    }

    @Test
    fun unpremultiplyRestoresStraightAlphaColors() {
        val rgba = byteArrayOf(50, 25, 0, 128.toByte(), 10, 20, 30, 255.toByte())

        unpremultiplyRgbaInPlace(rgba)

        assertContentEquals(byteArrayOf(100, 50, 0, 128.toByte(), 10, 20, 30, 255.toByte()), rgba)
    }

    @Test
    fun transparentPremultipliedPixelIsCanonicalizedToBlack() {
        val rgba = byteArrayOf(30, 20, 10, 0)

        unpremultiplyRgbaInPlace(rgba)

        assertContentEquals(byteArrayOf(0, 0, 0, 0), rgba)
    }

    @Test
    fun unpremultiplyRejectsPartialPixel() {
        assertFailsWith<IllegalArgumentException> { unpremultiplyRgbaInPlace(ByteArray(3)) }
    }
}

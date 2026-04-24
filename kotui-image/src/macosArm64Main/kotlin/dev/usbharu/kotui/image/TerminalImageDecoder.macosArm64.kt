@file:OptIn(ExperimentalForeignApi::class)

package dev.usbharu.kotui.image

import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.decoder.RawImageData
import com.skydoves.landscapist.core.model.ImageResult
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData

// CGImage / CGBitmap flags. Exposed via platform.CoreGraphics in some Kotlin/Native
// releases but not reliably as top-level constants, so we reference the well-known
// numeric values from CGImage.h:
//   kCGImageAlphaPremultipliedLast = 1
//   kCGBitmapByteOrder32Big        = 4 << 12 == 16384
private const val KCG_IMAGE_ALPHA_PREMULTIPLIED_LAST: UInt = 1u
private const val KCG_BITMAP_BYTE_ORDER_32_BIG: UInt = 16384u

internal actual fun loadImage(
    url: String,
    maxPixelWidth: Int?,
    maxPixelHeight: Int?,
): Flow<ImageLoadState> = flow {
    emit(ImageLoadState.Loading)
    try {
        Landscapist.getInstance().load(ImageRequest(model = url)).collect { result ->
            when (result) {
                ImageResult.Loading -> emit(ImageLoadState.Loading)
                is ImageResult.Failure -> {
                    val msg = result.message ?: result.throwable?.message
                        ?: result.throwable?.let { it::class.simpleName } ?: "unknown"
                    emit(ImageLoadState.Failure(msg))
                }
                is ImageResult.Success -> emit(decodeMacos(result, maxPixelWidth, maxPixelHeight))
            }
        }
    } catch (t: Throwable) {
        emit(ImageLoadState.Failure(t.message ?: t::class.simpleName ?: "unknown"))
    }
}

private fun decodeMacos(
    success: ImageResult.Success,
    maxW: Int?,
    maxH: Int?,
): ImageLoadState {
    // Apple 向け landscapist-core は `Success.data = RawImageData(bytes, mimeType)` を返し、
    // `Success.rawData` は埋めない (`ImageDecoder.apple.kt` 参照)。
    val bytes = (success.data as? RawImageData)?.data
        ?: success.rawData
        ?: return ImageLoadState.Failure("no image bytes (data=${success.data::class.simpleName})")
    if (bytes.isEmpty()) return ImageLoadState.Failure("empty image")
    val decoded = decodeWithCoreGraphics(bytes, maxW, maxH)
        ?: return ImageLoadState.Failure("core graphics decode failed")
    return ImageLoadState.Success(decoded.rgba, decoded.width, decoded.height)
}

private data class DecodedRgba(val rgba: ByteArray, val width: Int, val height: Int)

private fun decodeWithCoreGraphics(bytes: ByteArray, maxW: Int?, maxH: Int?): DecodedRgba? {
    return bytes.usePinned { pinnedInput ->
        val cfData = CFDataCreate(
            null,
            pinnedInput.addressOf(0).reinterpret<ByteVar>().reinterpret(),
            bytes.size.convert(),
        ) ?: return@usePinned null
        try {
            val source = CGImageSourceCreateWithData(cfData, null) ?: return@usePinned null
            try {
                val image = CGImageSourceCreateImageAtIndex(source, 0.convert(), null)
                    ?: return@usePinned null
                try {
                    val origW = CGImageGetWidth(image).toInt()
                    val origH = CGImageGetHeight(image).toInt()
                    if (origW <= 0 || origH <= 0) return@usePinned null

                    val target = computeTargetSize(origW, origH, maxW, maxH)
                    val finalW = target.width
                    val finalH = target.height
                    val stride = finalW * 4

                    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return@usePinned null
                    try {
                        val buffer = ByteArray(stride * finalH)
                        val drawn = buffer.usePinned { pinnedOut ->
                            val bitmapInfo: UInt =
                                KCG_IMAGE_ALPHA_PREMULTIPLIED_LAST or KCG_BITMAP_BYTE_ORDER_32_BIG
                            val ctx = CGBitmapContextCreate(
                                pinnedOut.addressOf(0),
                                finalW.convert(),
                                finalH.convert(),
                                8.convert(),
                                stride.convert(),
                                colorSpace,
                                bitmapInfo,
                            ) ?: return@usePinned false
                            try {
                                val rect = CGRectMake(0.0, 0.0, finalW.toDouble(), finalH.toDouble())
                                CGContextDrawImage(ctx, rect, image)
                            } finally {
                                CFRelease(ctx)
                            }
                            true
                        }
                        if (!drawn) return@usePinned null

                        // CG bitmap contexts store row 0 at the bottom; flip to top-down order.
                        val flipped = ByteArray(buffer.size)
                        for (y in 0 until finalH) {
                            val srcY = finalH - 1 - y
                            buffer.copyInto(flipped, y * stride, srcY * stride, srcY * stride + stride)
                        }
                        DecodedRgba(flipped, finalW, finalH)
                    } finally {
                        CFRelease(colorSpace)
                    }
                } finally {
                    CFRelease(image)
                }
            } finally {
                CFRelease(source)
            }
        } finally {
            CFRelease(cfData)
        }
    }
}

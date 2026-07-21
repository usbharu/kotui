package dev.usbharu.kotui.image

import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.model.ImageResult
import java.awt.Image as AwtImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CancellationException

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
                is ImageResult.Success -> emit(decodeJvm(result, maxPixelWidth, maxPixelHeight))
            }
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        emit(ImageLoadState.Failure(t.message ?: t::class.simpleName ?: "unknown"))
    }
}

private fun decodeJvm(
    success: ImageResult.Success,
    maxW: Int?,
    maxH: Int?,
): ImageLoadState {
    val source = (success.data as? BufferedImage) ?: success.rawData?.let { bytes ->
        ImageIO.read(ByteArrayInputStream(bytes))
    } ?: return ImageLoadState.Failure("unsupported image format")
    val resized = resizeIfNeeded(source, maxW, maxH)
    val w = resized.width
    val h = resized.height
    if (w <= 0 || h <= 0) return ImageLoadState.Failure("empty image")
    val pixelCountLong = w.toLong() * h.toLong()
    if (pixelCountLong > Int.MAX_VALUE / 4L) return ImageLoadState.Failure("image is too large")
    val pixelCount = pixelCountLong.toInt()
    val rgba = ByteArray(pixelCount * 4)
    val argb = IntArray(pixelCount)
    resized.getRGB(0, 0, w, h, argb, 0, w)
    var o = 0
    for (p in argb) {
        rgba[o++] = ((p shr 16) and 0xFF).toByte()
        rgba[o++] = ((p shr 8) and 0xFF).toByte()
        rgba[o++] = (p and 0xFF).toByte()
        rgba[o++] = ((p ushr 24) and 0xFF).toByte()
    }
    return ImageLoadState.Success(rgba, w, h)
}

private fun resizeIfNeeded(src: BufferedImage, maxW: Int?, maxH: Int?): BufferedImage {
    val target = computeTargetSize(src.width, src.height, maxW, maxH)
    if (target.width == src.width && target.height == src.height) return src
    val scaled = src.getScaledInstance(target.width, target.height, AwtImage.SCALE_SMOOTH)
    val out = BufferedImage(target.width, target.height, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    try {
        g.drawImage(scaled, 0, 0, null)
    } finally {
        g.dispose()
    }
    return out
}

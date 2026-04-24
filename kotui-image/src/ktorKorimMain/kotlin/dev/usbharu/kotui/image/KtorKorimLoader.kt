package dev.usbharu.kotui.image

import korlibs.image.bitmap.Bitmap32
import korlibs.image.format.PNG
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 各プラットフォーム固有の HttpClient を返す。
 * linuxX64 / mingwX64 は Curl エンジン、js は Js エンジン。
 */
internal expect fun createKtorHttpClient(): HttpClient

internal actual fun loadImage(
    url: String,
    maxPixelWidth: Int?,
    maxPixelHeight: Int?,
): Flow<ImageLoadState> = flow {
    emit(ImageLoadState.Loading)
    val bytes = try {
        createKtorHttpClient().use { client ->
            val response = client.get(url)
            response.readBytes()
        }
    } catch (t: Throwable) {
        emit(ImageLoadState.Failure(t.message ?: t::class.simpleName ?: "network error"))
        return@flow
    }
    val decoded = runCatching { decodePng(bytes, maxPixelWidth, maxPixelHeight) }
        .getOrElse {
            emit(ImageLoadState.Failure(it.message ?: "decode error"))
            return@flow
        }
    if (decoded == null) {
        emit(ImageLoadState.Failure("only PNG decoding is supported on this platform"))
        return@flow
    }
    emit(ImageLoadState.Success(decoded.rgba, decoded.width, decoded.height))
}

private data class DecodedRgba(val rgba: ByteArray, val width: Int, val height: Int)

private fun decodePng(bytes: ByteArray, maxW: Int?, maxH: Int?): DecodedRgba? {
    if (!looksLikePng(bytes)) return null
    val bitmap: Bitmap32 = PNG.read(bytes, "image.png").toBMP32()
    val target = computeTargetSize(bitmap.width, bitmap.height, maxW, maxH)
    val scaled = if (target.width == bitmap.width && target.height == bitmap.height) {
        bitmap
    } else {
        nearestNeighborScale(bitmap, target.width, target.height)
    }
    return DecodedRgba(bitmapToRgbaBytes(scaled), scaled.width, scaled.height)
}

private fun looksLikePng(bytes: ByteArray): Boolean =
    bytes.size >= 8 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'N'.code.toByte() &&
        bytes[3] == 'G'.code.toByte()

/**
 * korim の `Bitmap32.intData` は「R が LSB、A が MSB」でパックされた ARGB32 相当の
 * Int。メモリへ順次書き出すと little-endian 環境で R G B A の順になる。
 */
private fun bitmapToRgbaBytes(bitmap: Bitmap32): ByteArray {
    val w = bitmap.width
    val h = bitmap.height
    val ints = bitmap.ints
    val out = ByteArray(w * h * 4)
    var o = 0
    for (i in 0 until (w * h)) {
        val v = ints[i]
        out[o++] = (v and 0xFF).toByte()            // R
        out[o++] = ((v ushr 8) and 0xFF).toByte()   // G
        out[o++] = ((v ushr 16) and 0xFF).toByte()  // B
        out[o++] = ((v ushr 24) and 0xFF).toByte()  // A
    }
    return out
}

/** 大きい画像を縮小する単純な最近傍サンプリング。 */
private fun nearestNeighborScale(src: Bitmap32, newW: Int, newH: Int): Bitmap32 {
    val srcW = src.width
    val srcH = src.height
    val out = Bitmap32(newW, newH, premultiplied = src.premultiplied)
    val srcInts = src.ints
    val dstInts = out.ints
    for (y in 0 until newH) {
        val sy = (y.toLong() * srcH / newH).toInt().coerceIn(0, srcH - 1)
        val srcRow = sy * srcW
        val dstRow = y * newW
        for (x in 0 until newW) {
            val sx = (x.toLong() * srcW / newW).toInt().coerceIn(0, srcW - 1)
            dstInts[dstRow + x] = srcInts[srcRow + sx]
        }
    }
    return out
}

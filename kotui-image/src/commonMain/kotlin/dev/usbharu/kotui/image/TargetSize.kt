package dev.usbharu.kotui.image

import kotlin.math.max
import kotlin.math.min

internal data class TargetSize(val width: Int, val height: Int)

internal fun computeTargetSize(
    width: Int,
    height: Int,
    maxWidth: Int?,
    maxHeight: Int?,
): TargetSize {
    require(width > 0 && height > 0) { "source dimensions must be positive" }
    val limitW = maxWidth?.takeIf { it > 0 } ?: Int.MAX_VALUE
    val limitH = maxHeight?.takeIf { it > 0 } ?: Int.MAX_VALUE
    if (width <= limitW && height <= limitH) return TargetSize(width, height)
    val scale = min(limitW.toDouble() / width, limitH.toDouble() / height)
    val nw = max(1, (width * scale).toInt())
    val nh = max(1, (height * scale).toInt())
    return TargetSize(nw, nh)
}

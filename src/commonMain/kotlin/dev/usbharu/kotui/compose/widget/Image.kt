package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.utils.Kitty
import dev.usbharu.kotui.utils.Sixel

/**
 * Raw pixel image ready to be emitted to the terminal. The encoded escape
 * strings for both supported protocols (sixel and the Kitty graphics
 * protocol) are built lazily, so reusing the same instance across
 * recompositions avoids re-encoding.
 *
 * [cellPixelWidth] / [cellPixelHeight] describe the terminal font metrics for
 * cell-layout purposes. The defaults match common xterm settings (10×20); use
 * the values reported by the platform's [dev.usbharu.kotui.utils.SixelSupport]
 * for better accuracy.
 */
class TerminalImage(
    rgba: ByteArray,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val cellPixelWidth: Int = 10,
    val cellPixelHeight: Int = 20,
    val maxColors: Int = 256,
    val fallbackText: String? = null,
) {
    init {
        require(pixelWidth > 0 && pixelHeight > 0) { "pixel dimensions must be positive" }
        require(cellPixelWidth > 0 && cellPixelHeight > 0) { "cell pixel dimensions must be positive" }
        val pixelCount = pixelWidth.toLong() * pixelHeight.toLong()
        require(pixelCount <= Int.MAX_VALUE / 4L) { "image is too large" }
        val requiredBytes = pixelCount * 4L
        require(rgba.size.toLong() >= requiredBytes) { "rgba buffer too small" }
        require(maxColors in 1..256) { "maxColors must be in 1..256" }
    }

    private val rgbaRef = rgba.copyOf((pixelWidth.toLong() * pixelHeight.toLong() * 4L).toInt())

    val cellWidth: Int = 1 + (pixelWidth - 1) / cellPixelWidth
    val cellHeight: Int = 1 + (pixelHeight - 1) / cellPixelHeight

    /** Sixel escape sequence for this image. */
    val sixel: String by lazy { Sixel.encode(rgbaRef, pixelWidth, pixelHeight, maxColors) }

    /** Kitty graphics-protocol escape sequence for this image. */
    val kitty: String by lazy { Kitty.encode(rgbaRef, pixelWidth, pixelHeight) }
}

@Composable
fun Image(image: TerminalImage, modifier: Modifier = Modifier) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("Image").apply {
                layoutPolicy = LayoutPolicy.LEAF
            }
        },
        update = {
            set(image) {
                this.image = it
                this.preferredWidth = it.cellWidth
                this.preferredHeight = it.cellHeight
            }
            reconcile { applyModifier(modifier) }
        }
    )
}

package dev.usbharu.kotui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.widget.Image
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.compose.widget.Text

/**
 * URL から画像を読み込み、ターミナル上に表示する。
 *
 * 裏側のネットワーク取得 + デコード ([loadImage]) は各プラットフォーム実装に
 * 委譲する。`commonMain` は特定の画像ローダに依存しない。
 */
@Composable
public fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    cellPixelWidth: Int = 10,
    cellPixelHeight: Int = 20,
    maxColors: Int = 256,
    maxPixelWidth: Int? = 800,
    maxPixelHeight: Int? = 600,
    fallbackText: String? = null,
    loadingText: String = "[loading image…]",
    errorTextPrefix: String = "[image load failed: ",
) {
    val state by produceState<ImageLoadState>(
        initialValue = ImageLoadState.Loading,
        key1 = url,
        key2 = maxPixelWidth,
        key3 = maxPixelHeight,
    ) {
        loadImage(url, maxPixelWidth, maxPixelHeight).collect { value = it }
    }

    when (val s = state) {
        ImageLoadState.Loading -> Text(loadingText, modifier)
        is ImageLoadState.Failure -> Text("$errorTextPrefix${s.message}]", modifier)
        is ImageLoadState.Success -> {
            val terminal = remember(s, cellPixelWidth, cellPixelHeight, maxColors, fallbackText) {
                TerminalImage(
                    rgba = s.rgba,
                    pixelWidth = s.pixelWidth,
                    pixelHeight = s.pixelHeight,
                    cellPixelWidth = cellPixelWidth,
                    cellPixelHeight = cellPixelHeight,
                    maxColors = maxColors,
                    fallbackText = fallbackText,
                )
            }
            Image(terminal, modifier)
        }
    }
}

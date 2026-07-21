package dev.usbharu.kotui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.widget.Image
import dev.usbharu.kotui.compose.widget.TerminalImage
import dev.usbharu.kotui.compose.widget.Text
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.SixelSupport

/**
 * 画像が正常にデコードされたあと、グラフィクスプロトコル（Sixel / Kitty）が
 * 使えない／`forceFallback` が立っている場合に出す既定のフォールバックスタイル。
 */
public val DefaultFallbackStyle: Style = Style(
    fg = Ansi.FG_BRIGHT_WHITE,
    bg = Ansi.BG_BRIGHT_BLACK,
)

/**
 * URL から画像を読み込み、ターミナル上に表示する。
 *
 * フォールバックは 3 段階:
 *  1. [fallbackUrls] — 主 URL が失敗したときに順に試される代替 URL 群。
 *  2. [failure] — すべての URL が失敗したとき描画される Composable。
 *  3. [fallbackText] — Sixel / Kitty 非対応ターミナルで画像の代わりに描画される
 *                     テキスト。このとき画像のセルサイズと同じ領域を [fallbackStyle]
 *                     の背景色で確保し、1 行目にテキストを載せる。
 *                     [forceFallback] を true にすると能力検出に関わらず常に
 *                     フォールバックブロックを描画する。
 *
 * [loading] スロットは読み込み中、[failure] スロットは全 URL 失敗時を差し替える。
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
    fallbackUrls: List<String> = emptyList(),
    fallbackText: String? = null,
    fallbackStyle: Style = DefaultFallbackStyle,
    forceFallback: Boolean = false,
    loading: @Composable () -> Unit = { Text("[loading image…]", modifier) },
    failure: @Composable (String) -> Unit = { msg -> Text("[image load failed: $msg]", modifier) },
) {
    require(cellPixelWidth > 0 && cellPixelHeight > 0) { "cell pixel dimensions must be positive" }
    require(maxColors in 1..256) { "maxColors must be in 1..256" }
    val state by produceState<ImageLoadState>(
        initialValue = ImageLoadState.Loading,
        key1 = url,
        key2 = fallbackUrls,
        key3 = maxPixelWidth to maxPixelHeight,
    ) {
        val candidates = buildList {
            add(url)
            addAll(fallbackUrls)
        }
        var lastFailure: ImageLoadState.Failure? = null
        for (candidate in candidates) {
            value = ImageLoadState.Loading
            var terminal: ImageLoadState = ImageLoadState.Loading
            loadImage(candidate, maxPixelWidth, maxPixelHeight).collect { terminal = it }
            when (val finalState = terminal) {
                is ImageLoadState.Success -> {
                    value = finalState
                    return@produceState
                }
                is ImageLoadState.Failure -> lastFailure = finalState
                ImageLoadState.Loading -> Unit
            }
        }
        value = lastFailure ?: ImageLoadState.Failure("no image source succeeded")
    }

    when (val s = state) {
        ImageLoadState.Loading -> loading()
        is ImageLoadState.Failure -> failure(s.message)
        is ImageLoadState.Success -> {
            // Sixel / Kitty のどちらも使えない端末（Zellij / tmux の中や、素の
            // Terminal.app 等）ではピクセル画像を出しても見えない / ゴミになる。
            // デコード自体は成功しているので、画像と同じセル寸法のブロックを
            // [fallbackStyle] で塗り、1 行目に [fallbackText] を載せて表示領域
            // だけ確保する。
            val caps = SixelSupport.cached
            val graphicsSupported = caps != null && (caps.sixelSupported || caps.kittySupported)
            if (forceFallback || !graphicsSupported) {
                val widthCells = cellCountForPixels(s.pixelWidth, cellPixelWidth)
                val heightCells = cellCountForPixels(s.pixelHeight, cellPixelHeight)
                FallbackBlock(
                    widthCells = widthCells,
                    heightCells = heightCells,
                    text = fallbackText ?: "[image ${s.pixelWidth}×${s.pixelHeight}]",
                    style = fallbackStyle,
                    modifier = modifier,
                )
            } else {
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
}

internal fun cellCountForPixels(pixelCount: Int, pixelsPerCell: Int): Int {
    require(pixelCount > 0) { "pixel count must be positive" }
    require(pixelsPerCell > 0) { "pixels per cell must be positive" }
    return 1 + (pixelCount - 1) / pixelsPerCell
}

/**
 * `fillChar = ' '` + `text` を同じノードに持たせた LEAF。renderer は
 *  1. bounds 全体を [style] の背景色で塗る（fillChar ループ）
 *  2. 1 行目に [text] を上書き（writeString, 同じ activeStyle）
 * の順で処理するので、カラーブロックの左上にテキストが載る形になる。
 */
@Composable
private fun FallbackBlock(
    widthCells: Int,
    heightCells: Int,
    text: String,
    style: Style,
    modifier: Modifier,
) {
    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("ImageFallbackBlock").apply {
                layoutPolicy = LayoutPolicy.LEAF
                fillChar = ' '
            }
        },
        update = {
            reconcile { beginModifierUpdate() }
            set(widthCells) { this.preferredWidth = it }
            set(heightCells) { this.preferredHeight = it }
            set(text) { this.text = it }
            set(style) { this.style = it }
            reconcile { applyModifier(modifier) }
        },
    )
}

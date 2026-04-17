package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode

private val DEFAULT_FRAMES = listOf('|', '/', '-', '\\')

/**
 * 1文字のローディング表示。`frame` を呼び出し側で進めることでアニメーションする。
 *
 * 注: 現状の `runTui` は入力イベント駆動でしか再描画しないため、自動アニメには
 * 別途 `LaunchedEffect` で `delay` + `frame++` を回し、何らかの入力イベントを
 * 待つ必要がある。完全自動アニメは `runTui` の改修待ち。
 */
@Composable
fun Spinner(
    frame: Int,
    chars: List<Char> = DEFAULT_FRAMES,
    modifier: Modifier = Modifier,
) {
    val display = if (chars.isEmpty()) " " else chars[((frame % chars.size) + chars.size) % chars.size].toString()

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("Spinner").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
                preferredWidth = 1
            }
        },
        update = {
            set(display) { text = it }
            set(modifier) { applyModifier(it) }
        }
    )
}

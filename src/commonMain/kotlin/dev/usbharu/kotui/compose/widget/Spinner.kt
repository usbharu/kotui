package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import kotlinx.coroutines.delay

private val DEFAULT_FRAMES = listOf('⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏')
private const val DEFAULT_INTERVAL_MS = 80L

object SpinnerFrames {
    val CLASSIC = listOf('|', '/', '-', '\\')
    val BRAILLE = listOf('⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏')
    val DOTS = listOf('⠁', '⠂', '⠄', '⠂')
    val ARROW = listOf('←', '↖', '↑', '↗', '→', '↘', '↓', '↙')
    val CIRCLE = listOf('◐', '◓', '◑', '◒')
    val TRIANGLE = listOf('◢', '◣', '◤', '◥')
    val BAR = listOf('▁', '▃', '▄', '▅', '▆', '▇', '█', '▇', '▆', '▅', '▄', '▃')
}

/**
 * キー入力がなくても時間経過で自動的にフレームが進むローディング表示。
 * `runTui` のレンダーループは `BroadcastFrameClock` の待機者がいる間は
 * 周期的に `driveFrame()` を呼ぶので、`LaunchedEffect` + `delay` でカウンタを
 * 進めれば自動でアニメーションする。
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    chars: List<Char> = DEFAULT_FRAMES,
    intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(chars, intervalMs) {
        while (true) {
            delay(intervalMs)
            frame++
        }
    }
    Spinner(frame = frame, chars = chars, modifier = modifier)
}

/**
 * フレーム番号を呼び出し側で管理する低レベル版。進行を外部ロジックと同期させたい
 * 場合に使う。通常は引数なしの [Spinner] を使えばよい。
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

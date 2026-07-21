package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.utils.displayWidth

/**
 * 垂直の区切り線。親 `Row` の高さに合わせて自動的に伸びる。
 *
 * `LayoutEngine` が Row の高さを「子の最大高さ」から推定するため、
 * 隣に高さを持つ要素（例: `Center(Modifier.size(w, h))`）があれば
 * VerticalDivider 自身に高さ指定をしなくても複数行に描画される。
 */
@Composable
fun VerticalDivider(char: Char = '│', modifier: Modifier = Modifier) {
    val charWidth = char.toString().displayWidth()
    require(charWidth > 0) { "divider character must be visible" }
    ComposeNode<TuiNode, TuiApplier>(
        factory = { TuiNode("VerticalDivider").apply { layoutPolicy = LayoutPolicy.LEAF; preferredWidth = 1 } },
        update = {
            reconcile { beginModifierUpdate() }
            set(char) { fillChar = it }
            set(charWidth) { preferredWidth = it }
            reconcile { applyModifier(modifier) }
        }
    )
}

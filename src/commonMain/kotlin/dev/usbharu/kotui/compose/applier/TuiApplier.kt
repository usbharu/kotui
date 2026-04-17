package dev.usbharu.kotui.compose.applier

import androidx.compose.runtime.AbstractApplier
import dev.usbharu.kotui.compose.node.TuiNode

class TuiApplier(root: TuiNode) : AbstractApplier<TuiNode>(root) {
    override fun insertTopDown(index: Int, instance: TuiNode) {
        current.insertAt(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: TuiNode) {}

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.clear()
    }
}

package dev.usbharu.kotui.compose.focus

import androidx.compose.runtime.mutableStateOf
import dev.usbharu.kotui.compose.node.TuiNode

class FocusManager {
    private val _focusedId = mutableStateOf(-1)
    val focusedId: Int get() = _focusedId.value

    private var nextFocusId: Int = 0

    fun allocateFocusId(): Int = nextFocusId++

    fun isFocused(focusId: Int): Boolean = _focusedId.value == focusId

    fun requestFocus(focusId: Int) {
        _focusedId.value = focusId
    }

    fun focusNext(root: TuiNode) {
        val scope = findFocusScope(root, _focusedId.value) ?: root
        val focusables = collectFocusableIds(scope)
        if (focusables.isEmpty()) return
        val idx = focusables.indexOf(_focusedId.value)
        _focusedId.value = focusables[if (idx < 0) 0 else (idx + 1) % focusables.size]
    }

    fun focusPrevious(root: TuiNode) {
        val scope = findFocusScope(root, _focusedId.value) ?: root
        val focusables = collectFocusableIds(scope)
        if (focusables.isEmpty()) return
        val idx = focusables.indexOf(_focusedId.value)
        _focusedId.value = focusables[if (idx <= 0) focusables.size - 1 else idx - 1]
    }

    fun autoFocus(root: TuiNode) {
        if (_focusedId.value >= 0 && findNodeByFocusId(root, _focusedId.value) != null) return
        _focusedId.value = collectFocusableIds(root).firstOrNull() ?: -1
    }

    fun findFocusedNode(root: TuiNode): TuiNode? {
        if (_focusedId.value < 0) return null
        return findNodeByFocusId(root, _focusedId.value)
    }

    private fun findNodeByFocusId(node: TuiNode, id: Int): TuiNode? {
        if (node.focusId == id) return node
        for (child in node.children) {
            findNodeByFocusId(child, id)?.let { return it }
        }
        return null
    }

    private fun findFocusScope(root: TuiNode, focusId: Int): TuiNode? {
        val node = findNodeByFocusId(root, focusId) ?: return null
        var current = node.parent
        while (current != null) {
            if (current.focusScope) return current
            current = current.parent
        }
        return null
    }

    private fun collectFocusableIds(root: TuiNode): List<Int> {
        val result = mutableListOf<Int>()
        fun walk(node: TuiNode) {
            if (node.focusable && node.focusId >= 0) result.add(node.focusId)
            node.children.forEach(::walk)
        }
        walk(root)
        return result
    }
}

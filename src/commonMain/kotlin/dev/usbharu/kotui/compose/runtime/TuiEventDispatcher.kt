package dev.usbharu.kotui.compose.runtime

import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.TuiNode

internal class TuiEventDispatcher(
    private val rootNode: TuiNode,
    private val focusManager: FocusManager,
    private val clearHandledKey: () -> Unit = {},
    private val publishUnhandledKey: (KeyEvent) -> Unit = {},
) {
    fun dispatchPaste(text: String): Boolean {
        val focused = focusManager.findFocusedNode(rootNode) ?: return false
        if (focused.onPaste?.invoke(text) == true) return true
        var current: TuiNode? = focused.parent
        while (current != null) {
            if (current.onPaste?.invoke(text) == true) return true
            current = current.parent
        }
        return false
    }

    fun dispatchKey(event: KeyEvent): Boolean {
        if (event.key == Key.TAB) {
            focusManager.focusNext(rootNode)
            clearHandledKey()
            return true
        }

        val focusedNode = focusManager.findFocusedNode(rootNode)
        if (focusedNode?.onKeyEvent?.invoke(event) == true) {
            clearHandledKey()
            return true
        }
        if (focusedNode != null) {
            var current = focusedNode.parent
            while (current != null) {
                if (current.onKeyEvent?.invoke(event) == true) {
                    clearHandledKey()
                    return true
                }
                current = current.parent
            }
        }

        publishUnhandledKey(event)
        return false
    }

    fun dispatchEvent(event: InputEvent) {
        when (event) {
            is KeyEvent -> dispatchKey(event)
            is PasteEvent -> dispatchPaste(event.text)
        }
    }
}

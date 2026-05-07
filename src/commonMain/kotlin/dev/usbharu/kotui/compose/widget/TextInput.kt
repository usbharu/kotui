package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.LocalFocusManager

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    enableEditing: Boolean = true,
    editingFeatures: TextEditingFeatures = TextEditingFeatures.Default,
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val cursorState = remember { mutableStateOf(value.length) }
    val anchorState = remember { mutableStateOf<Int?>(null) }

    // Keep cursor/anchor within bounds if the caller shrinks `value`.
    val safeCursor = TextEditOps.clampToBoundary(value, cursorState.value.coerceIn(0, value.length))
    if (safeCursor != cursorState.value) cursorState.value = safeCursor
    anchorState.value?.let { a ->
        val safeAnchor = TextEditOps.clampToBoundary(value, a.coerceIn(0, value.length))
        if (safeAnchor != a) anchorState.value = safeAnchor
        if (anchorState.value == cursorState.value) anchorState.value = null
    }

    val displayText = TextInputOps.displayText(value, placeholder, isFocused)
    val cursorPosition = TextInputOps.cursorPosition(value, cursorState.value, isFocused)
    val highlights = TextInputOps.highlights(
        value = value,
        cursor = cursorState.value,
        anchor = anchorState.value,
        isFocused = isFocused,
        selectionEnabled = editingFeatures.selection,
    )

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("TextInput").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
                focusable = true
            }
        },
        update = {
            set(displayText) { text = it }
            set(focusId) { this.focusId = it }
            set(cursorPosition) { cursorCol = it }
            set(highlights) { textHighlights = it }
            set(TextInputBindings(value, onValueChange, onSubmit, enableEditing, editingFeatures, clipboard, cursorState, anchorState)) { b ->
                onKeyEvent = { event -> TextInputOps.handleKey(b, event) }
                onPaste = { text ->
                    if (b.enableEditing && b.features.clipboard) {
                        TextInputOps.insertText(b, text)
                        true
                    } else false
                }
            }
            set(modifier) { applyModifier(it) }
        }
    )
}

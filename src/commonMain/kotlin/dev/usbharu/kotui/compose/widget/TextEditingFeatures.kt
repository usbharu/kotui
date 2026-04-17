package dev.usbharu.kotui.compose.widget

/**
 * Fine-grained switches for [TextInput] editing behaviour. Combined with the top-level
 * `enableEditing` flag: if `enableEditing` is false the input is read-only regardless
 * of this object.
 */
data class TextEditingFeatures(
    /** Arrow keys, Home/End, Ctrl+A/E. */
    val cursorMovement: Boolean = true,
    /** Ctrl/Alt + Left/Right, Alt+b/f, Ctrl+W (paired with [deletion]). */
    val wordNavigation: Boolean = true,
    /** Delete / Ctrl+D / Ctrl+K / Ctrl+U / Ctrl+W. Backspace is always available if editing is enabled. */
    val deletion: Boolean = true,
    /** Ctrl+C / Ctrl+X / Ctrl+V and bracketed paste. */
    val clipboard: Boolean = true,
    /** Shift+arrow/Home/End selection extension and selection highlighting. */
    val selection: Boolean = true,
) {
    companion object {
        val Default = TextEditingFeatures()
        val Basic = TextEditingFeatures(
            cursorMovement = true,
            wordNavigation = false,
            deletion = true,
            clipboard = false,
            selection = false,
        )
        val None = TextEditingFeatures(
            cursorMovement = false,
            wordNavigation = false,
            deletion = false,
            clipboard = false,
            selection = false,
        )
    }
}

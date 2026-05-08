package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

data class TextInputDecoration(
    val style: Style = Style(bg = Ansi.BG_BRIGHT_BLACK, underline = true),
    val focusedStyle: Style? = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true, underline = true),
) {
    companion object {
        val Default = TextInputDecoration()
        val None = TextInputDecoration(style = Style(), focusedStyle = null)
    }
}

internal data class TextInputResolvedStyles(
    val style: Style,
    val focusedStyle: Style?,
)

internal fun resolveTextInputStyles(
    enableEditing: Boolean,
    decoration: TextInputDecoration,
    modifierStyle: Style,
    modifierFocusedStyle: Style?,
): TextInputResolvedStyles {
    if (!enableEditing) {
        return TextInputResolvedStyles(
            style = modifierStyle,
            focusedStyle = modifierFocusedStyle,
        )
    }

    val resolvedStyle = decoration.style.mergeWith(modifierStyle)
    val resolvedFocusedStyle = modifierFocusedStyle
        ?: decoration.focusedStyle?.mergeWith(modifierStyle)

    return TextInputResolvedStyles(
        style = resolvedStyle,
        focusedStyle = resolvedFocusedStyle,
    )
}

private fun Style.mergeWith(override: Style): Style =
    copy(
        fg = override.fg ?: fg,
        bg = override.bg ?: bg,
        bold = bold || override.bold,
        underline = underline || override.underline,
        reverse = reverse || override.reverse,
    )

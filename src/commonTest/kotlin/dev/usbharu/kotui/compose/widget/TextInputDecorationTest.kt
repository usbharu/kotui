package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextInputDecorationTest {

    @Test
    fun defaultEditableStyleUsesSubtleBackgroundAndUnderline() {
        val styles = resolveTextInputStyles(
            enableEditing = true,
            decoration = TextInputDecoration.Default,
            modifierStyle = Style(),
            modifierFocusedStyle = null,
        )

        assertEquals(Ansi.BG_BRIGHT_BLACK, styles.style.bg)
        assertTrue(styles.style.underline)
    }

    @Test
    fun defaultFocusedStyleIsAppliedWhenModifierFocusedStyleIsAbsent() {
        val styles = resolveTextInputStyles(
            enableEditing = true,
            decoration = TextInputDecoration.Default,
            modifierStyle = Style(fg = Ansi.FG_BRIGHT_WHITE),
            modifierFocusedStyle = null,
        )

        assertEquals(Ansi.FG_BRIGHT_WHITE, styles.focusedStyle?.fg)
        assertEquals(Ansi.BG_CYAN, styles.focusedStyle?.bg)
        assertTrue(styles.focusedStyle?.bold == true)
        assertTrue(styles.focusedStyle.underline)
    }

    @Test
    fun disabledEditingDoesNotApplyDecoration() {
        val modifierStyle = Style(fg = Ansi.FG_YELLOW)
        val styles = resolveTextInputStyles(
            enableEditing = false,
            decoration = TextInputDecoration.Default,
            modifierStyle = modifierStyle,
            modifierFocusedStyle = null,
        )

        assertEquals(modifierStyle, styles.style)
        assertNull(styles.focusedStyle)
    }

    @Test
    fun noneDecorationRestoresUndecoratedDefaults() {
        val styles = resolveTextInputStyles(
            enableEditing = true,
            decoration = TextInputDecoration.None,
            modifierStyle = Style(),
            modifierFocusedStyle = null,
        )

        assertNull(styles.style.bg)
        assertFalse(styles.style.underline)
        assertNull(styles.focusedStyle)
    }

    @Test
    fun explicitModifierFocusedStyleIsAuthoritative() {
        val explicitFocusedStyle = Style(fg = Ansi.FG_RED)
        val styles = resolveTextInputStyles(
            enableEditing = true,
            decoration = TextInputDecoration.Default,
            modifierStyle = Style(bg = Ansi.BG_BLUE, underline = true),
            modifierFocusedStyle = explicitFocusedStyle,
        )

        assertEquals(explicitFocusedStyle, styles.focusedStyle)
    }
}

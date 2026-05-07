package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.utils.Ansi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InteractiveWidgetOpsTest {
    @Test
    fun buttonAndSelectTextReflectFocusState() {
        assertEquals("[ Save ]", InteractiveWidgetOps.buttonText("Save", isFocused = true))
        assertEquals("  Save  ", InteractiveWidgetOps.buttonText("Save", isFocused = false))
        assertEquals("▶ Alpha ▾ ", InteractiveWidgetOps.selectText("Alpha", isFocused = true))
        assertEquals("  Alpha ▾ ", InteractiveWidgetOps.selectText("Alpha", isFocused = false))
    }

    @Test
    fun checkboxTextIncludesStateLabelAndFocusMarker() {
        assertEquals("▶ [x] Done", InteractiveWidgetOps.checkboxText(true, "Done", isFocused = true))
        assertEquals("  [ ]", InteractiveWidgetOps.checkboxText(false, "", isFocused = false))
    }

    @Test
    fun activationHelpersAcceptEnterAndPlainSpaceOnlyWhereAppropriate() {
        val enter = KeyEvent('\n', Key.ENTER)
        val plainSpace = KeyEvent(' ', Key.CHAR)
        val ctrlSpace = KeyEvent(' ', Key.CHAR, ctrl = true)
        val altSpace = KeyEvent(' ', Key.CHAR, alt = true)

        assertTrue(InteractiveWidgetOps.buttonAccepts(enter))
        assertFalse(InteractiveWidgetOps.buttonAccepts(plainSpace))
        assertTrue(InteractiveWidgetOps.isActivate(enter))
        assertTrue(InteractiveWidgetOps.isActivate(plainSpace))
        assertFalse(InteractiveWidgetOps.isActivate(ctrlSpace))
        assertFalse(InteractiveWidgetOps.isActivate(altSpace))
    }

    @Test
    fun sharedFocusedStyleMatchesInteractiveWidgets() {
        val style = InteractiveWidgetOps.focusedStyle

        assertEquals(Ansi.FG_BLACK, style.fg)
        assertEquals(Ansi.BG_CYAN, style.bg)
        assertTrue(style.bold)
    }
}

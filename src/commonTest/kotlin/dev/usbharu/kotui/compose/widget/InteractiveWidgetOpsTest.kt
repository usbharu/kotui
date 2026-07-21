package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.focusedStyle
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import dev.usbharu.kotui.utils.Ansi

class InteractiveWidgetOpsTest {
    @Test
    fun callerStylesOverrideInteractiveWidgetDefaults() {
        val normal = Style(bold = true)
        val focused = Style(reverse = true)
        val node = TuiNode("interactive")

        node.applyModifier(
            interactiveWidgetModifier(
                Modifier.style(normal).focusedStyle(focused),
            ),
        )

        assertEquals(normal, node.style)
        assertEquals(focused, node.focusedStyle)
    }

    @Test
    fun spaceActivatesButtonsWithoutModifiers() {
        assertTrue(isUnmodifiedActivationKey(KeyEvent(' ', Key.CHAR)))
        assertFalse(isUnmodifiedActivationKey(KeyEvent(' ', Key.CHAR, ctrl = true)))
        assertFalse(isUnmodifiedActivationKey(KeyEvent(' ', Key.CHAR, alt = true)))
    }

    @Test
    fun modifiedEnterDoesNotAccidentallyActivateWidget() {
        assertTrue(isUnmodifiedActivationKey(KeyEvent('\r', Key.ENTER)))
        assertFalse(isUnmodifiedActivationKey(KeyEvent('\r', Key.ENTER, alt = true)))
        assertFalse(isUnmodifiedActivationKey(KeyEvent('\r', Key.ENTER, ctrl = true)))
    }

    @Test
    fun listWindowDoesNotOverflowWithMaxVisibleRows() {
        val window = calculateListWindow(
            itemCount = 3,
            selectedIndex = 2,
            visibleRows = Int.MAX_VALUE,
            previousScroll = 1,
        )

        assertEquals(0, window.scroll)
        assertEquals(3, window.endExclusive)
    }

    @Test
    fun listWindowClampsStaleScrollAfterItemsShrink() {
        val window = calculateListWindow(2, 1, 1, Int.MAX_VALUE)

        assertEquals(1, window.scroll)
        assertEquals(2, window.endExclusive)
    }

    @Test
    fun staleMultiSelectIndicesAreRemoved() {
        assertEquals(linkedSetOf(0, 2), validCheckedIndices(setOf(-1, 0, 2, 3, 99), 3))
        assertEquals(emptySet(), validCheckedIndices(setOf(0), 0))
    }

    @Test
    fun singleLinePasteCollapsesEveryLineSeparator() {
        assertEquals("one two three four", normalizeSingleLinePaste("one\r\ntwo\nthree\rfour"))
        assertEquals("a b", normalizeSingleLinePaste("a\tb\u001B"))
    }

    @Test
    fun singleLineModelRejectsEmbeddedLineBreaks() {
        assertEquals("okay", requireSingleLineValue("okay"))
        kotlin.test.assertFailsWith<IllegalArgumentException> { requireSingleLineValue("a\nb") }
    }

    @Test
    fun multilinePasteCanonicalizesWindowsAndClassicMacLineEndings() {
        assertEquals("one\ntwo\nthree", normalizeMultilinePaste("one\r\ntwo\rthree"))
    }

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
        assertTrue(InteractiveWidgetOps.buttonAccepts(plainSpace))
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

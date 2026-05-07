package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.mutableStateOf
import dev.usbharu.kotui.compose.clipboard.InMemoryClipboard
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextInputOpsTest {
    @Test
    fun displayCursorAndHighlightPresentation() {
        assertEquals("  hint", TextInputOps.displayText("", "hint", isFocused = false))
        assertEquals("> ", TextInputOps.displayText("", "hint", isFocused = true))
        assertEquals("  value", TextInputOps.displayText("value", "hint", isFocused = false))
        assertEquals(3, TextInputOps.cursorPosition("a界", 1, isFocused = true))
        assertNull(TextInputOps.cursorPosition("abc", 1, isFocused = false))
        assertNull(TextInputOps.highlights("abcdef", 2, null, isFocused = true, selectionEnabled = true))
        assertNull(TextInputOps.highlights("abcdef", 2, 2, isFocused = true, selectionEnabled = true))
        assertNull(TextInputOps.highlights("abcdef", 2, 0, isFocused = false, selectionEnabled = true))
        assertNull(TextInputOps.highlights("abcdef", 2, 0, isFocused = true, selectionEnabled = false))

        val highlight = TextInputOps.highlights("a界cd", 3, 1, isFocused = true, selectionEnabled = true)!!.single()
        assertEquals(3, highlight.startCol)
        assertEquals(6, highlight.endCol)
        assertTrue(highlight.style.reverse)
    }

    @Test
    fun submitWorksEvenWhenEditingIsDisabled() {
        var submitted = 0
        val b = bindings("abc", cursor = 1, enableEditing = false, onSubmit = { submitted++ })

        assertTrue(TextInputOps.handleKey(b, KeyEvent('\n', Key.ENTER)))
        assertEquals(1, submitted)
        assertFalse(TextInputOps.handleKey(bindings("abc", onSubmit = null), KeyEvent('\n', Key.ENTER)))
        assertFalse(TextInputOps.handleKey(bindings("abc", enableEditing = false), KeyEvent('x', Key.CHAR)))
    }

    @Test
    fun cursorMovementSupportsCodePointsWordsSelectionAndReadlineBindings() {
        val left = bindings("ab界 cd", cursor = 3)
        assertTrue(TextInputOps.handleKey(left, KeyEvent('\u0000', Key.ARROW_LEFT)))
        assertEquals(2, left.cursor.value)
        assertNull(left.anchor.value)

        val wordLeft = bindings("ab cd", cursor = 5)
        assertTrue(TextInputOps.handleKey(wordLeft, KeyEvent('\u0000', Key.ARROW_LEFT, ctrl = true)))
        assertEquals(3, wordLeft.cursor.value)

        val right = bindings("ab界 cd", cursor = 2)
        assertTrue(TextInputOps.handleKey(right, KeyEvent('\u0000', Key.ARROW_RIGHT)))
        assertEquals(3, right.cursor.value)

        val wordRight = bindings("ab cd", cursor = 0)
        assertTrue(TextInputOps.handleKey(wordRight, KeyEvent('\u0000', Key.ARROW_RIGHT, alt = true)))
        assertEquals(2, wordRight.cursor.value)

        val ctrlWordRight = bindings("ab cd", cursor = 0)
        assertTrue(TextInputOps.handleKey(ctrlWordRight, KeyEvent('\u0000', Key.ARROW_RIGHT, ctrl = true)))
        assertEquals(2, ctrlWordRight.cursor.value)

        val home = bindings("abc", cursor = 2)
        assertTrue(TextInputOps.handleKey(home, KeyEvent('\u0000', Key.HOME, shift = true)))
        assertEquals(0, home.cursor.value)
        assertEquals(2, home.anchor.value)
        assertTrue(TextInputOps.handleKey(home, KeyEvent('\u0000', Key.END, shift = true)))
        assertEquals(3, home.cursor.value)
        assertEquals(2, home.anchor.value)

        val ctrl = bindings("abcdef", cursor = 3)
        assertTrue(TextInputOps.handleKey(ctrl, KeyEvent('a', Key.CHAR, ctrl = true)))
        assertEquals(0, ctrl.cursor.value)
        assertTrue(TextInputOps.handleKey(ctrl, KeyEvent('e', Key.CHAR, ctrl = true)))
        assertEquals(6, ctrl.cursor.value)

        val alt = bindings("ab cd", cursor = 3)
        assertTrue(TextInputOps.handleKey(alt, KeyEvent('b', Key.CHAR, alt = true)))
        assertEquals(0, alt.cursor.value)
        assertTrue(TextInputOps.handleKey(alt, KeyEvent('f', Key.CHAR, alt = true)))
        assertEquals(2, alt.cursor.value)
    }

    @Test
    fun movementFeatureFlagsDisableUnhandledMovementKeys() {
        val none = bindings("abc", cursor = 1, features = TextEditingFeatures.None)
        assertFalse(TextInputOps.handleKey(none, KeyEvent('\u0000', Key.ARROW_LEFT)))
        assertFalse(TextInputOps.handleKey(none, KeyEvent('a', Key.CHAR, ctrl = true)))

        val noWord = bindings("ab cd", cursor = 5, features = TextEditingFeatures.Basic)
        assertTrue(TextInputOps.handleKey(noWord, KeyEvent('\u0000', Key.ARROW_LEFT, ctrl = true)))
        assertEquals(4, noWord.cursor.value)
        assertFalse(TextInputOps.handleKey(noWord, KeyEvent('b', Key.CHAR, alt = true)))

        val noSelection = bindings("abc", cursor = 2, features = TextEditingFeatures.Basic)
        assertTrue(TextInputOps.handleKey(noSelection, KeyEvent('\u0000', Key.HOME, shift = true)))
        assertEquals(0, noSelection.cursor.value)
        assertNull(noSelection.anchor.value)
    }

    @Test
    fun insertionAndSelectionReplacementClearAnchor() {
        val b = bindings("abcdef", cursor = 4, anchor = 1)
        assertTrue(TextInputOps.handleKey(b, KeyEvent('X', Key.CHAR)))

        assertEquals("aXef", b.lastValue)
        assertEquals(2, b.cursor.value)
        assertNull(b.anchor.value)
        assertFalse(TextInputOps.handleKey(bindings("abc"), KeyEvent('x', Key.CHAR, ctrl = true)))
        assertFalse(TextInputOps.handleKey(bindings("abc"), KeyEvent('x', Key.CHAR, alt = true)))
        assertFalse(TextInputOps.handleKey(bindings("abc"), KeyEvent('\u0001', Key.CHAR)))
    }

    @Test
    fun deletionHandlesSelectionEdgesWordsAndLineCommands() {
        val selected = bindings("abcdef", cursor = 5, anchor = 2)
        assertTrue(TextInputOps.handleKey(selected, KeyEvent('\b', Key.BACKSPACE)))
        assertEquals("abf", selected.lastValue)
        assertEquals(2, selected.cursor.value)

        val beforeStart = bindings("abc", cursor = 0)
        assertTrue(TextInputOps.handleKey(beforeStart, KeyEvent('\b', Key.BACKSPACE)))
        assertNull(beforeStart.lastValue)

        val afterEnd = bindings("abc", cursor = 3)
        assertTrue(TextInputOps.handleKey(afterEnd, KeyEvent('\u007f', Key.DELETE)))
        assertNull(afterEnd.lastValue)

        val deleteAfterSelection = bindings("abcdef", cursor = 4, anchor = 1)
        assertTrue(TextInputOps.handleKey(deleteAfterSelection, KeyEvent('\u007f', Key.DELETE)))
        assertEquals("aef", deleteAfterSelection.lastValue)
        assertEquals(1, deleteAfterSelection.cursor.value)

        val deleteAfter = bindings("abc", cursor = 1)
        assertTrue(TextInputOps.handleKey(deleteAfter, KeyEvent('\u007f', Key.DELETE)))
        assertEquals("ac", deleteAfter.lastValue)
        assertEquals(1, deleteAfter.cursor.value)

        val word = bindings("ab cd", cursor = 5)
        assertTrue(TextInputOps.handleKey(word, KeyEvent('\b', Key.BACKSPACE, alt = true)))
        assertEquals("ab ", word.lastValue)
        assertEquals(3, word.cursor.value)

        val ctrlD = bindings("abc", cursor = 1)
        assertTrue(TextInputOps.handleKey(ctrlD, KeyEvent('d', Key.CHAR, ctrl = true)))
        assertEquals("ac", ctrlD.lastValue)

        val ctrlK = bindings("abcdef", cursor = 2)
        assertTrue(TextInputOps.handleKey(ctrlK, KeyEvent('k', Key.CHAR, ctrl = true)))
        assertEquals("ab", ctrlK.lastValue)

        val ctrlKAtEnd = bindings("abcdef", cursor = 6)
        assertTrue(TextInputOps.handleKey(ctrlKAtEnd, KeyEvent('k', Key.CHAR, ctrl = true)))
        assertNull(ctrlKAtEnd.lastValue)

        val ctrlU = bindings("abcdef", cursor = 2)
        assertTrue(TextInputOps.handleKey(ctrlU, KeyEvent('u', Key.CHAR, ctrl = true)))
        assertEquals("cdef", ctrlU.lastValue)

        val ctrlUAtStart = bindings("abcdef", cursor = 0)
        assertTrue(TextInputOps.handleKey(ctrlUAtStart, KeyEvent('u', Key.CHAR, ctrl = true)))
        assertNull(ctrlUAtStart.lastValue)

        val ctrlWAtBoundary = bindings("abc", cursor = 0)
        assertTrue(TextInputOps.handleKey(ctrlWAtBoundary, KeyEvent('w', Key.CHAR, ctrl = true)))
        assertNull(ctrlWAtBoundary.lastValue)
    }

    @Test
    fun deletionFeatureFlagDisablesDeleteAndCtrlDeletionButBackspaceStillDeletesCharacter() {
        val b = bindings("abc", cursor = 2, features = TextEditingFeatures(deletion = false))
        assertFalse(TextInputOps.handleKey(b, KeyEvent('\u007f', Key.DELETE)))
        assertFalse(TextInputOps.handleKey(b, KeyEvent('d', Key.CHAR, ctrl = true)))
        assertTrue(TextInputOps.handleKey(b, KeyEvent('\b', Key.BACKSPACE)))
        assertEquals("ac", b.lastValue)
    }

    @Test
    fun clipboardCopiesCutsPastesAndReportsMissingSelection() {
        val clipboard = InMemoryClipboard("paste")
        val copy = bindings("abcdef", cursor = 5, anchor = 2, clipboard = clipboard)
        assertTrue(TextInputOps.handleKey(copy, KeyEvent('c', Key.CHAR, ctrl = true)))
        assertEquals("cde", clipboard.read())
        assertEquals(null, copy.lastValue)

        val noSelection = bindings("abcdef", cursor = 2, clipboard = clipboard)
        assertFalse(TextInputOps.handleKey(noSelection, KeyEvent('c', Key.CHAR, ctrl = true)))
        assertFalse(TextInputOps.handleKey(noSelection, KeyEvent('x', Key.CHAR, ctrl = true)))

        val cut = bindings("abcdef", cursor = 5, anchor = 2, clipboard = clipboard)
        assertTrue(TextInputOps.handleKey(cut, KeyEvent('x', Key.CHAR, ctrl = true)))
        assertEquals("cde", clipboard.read())
        assertEquals("abf", cut.lastValue)

        val cutNoDelete = bindings("abcdef", cursor = 5, anchor = 2, clipboard = clipboard, features = TextEditingFeatures(deletion = false))
        assertTrue(TextInputOps.handleKey(cutNoDelete, KeyEvent('x', Key.CHAR, ctrl = true)))
        assertEquals("cde", clipboard.read())
        assertNull(cutNoDelete.lastValue)

        val paste = bindings("ab", cursor = 1, clipboard = clipboard)
        assertTrue(TextInputOps.handleKey(paste, KeyEvent('v', Key.CHAR, ctrl = true)))
        assertEquals("acdeb", paste.lastValue)

        val emptyPaste = bindings("ab", cursor = 1, clipboard = InMemoryClipboard(""))
        assertTrue(TextInputOps.handleKey(emptyPaste, KeyEvent('v', Key.CHAR, ctrl = true)))
        assertNull(emptyPaste.lastValue)

        val disabled = bindings("ab", cursor = 1, clipboard = clipboard, features = TextEditingFeatures.Basic)
        assertFalse(TextInputOps.handleKey(disabled, KeyEvent('v', Key.CHAR, ctrl = true)))
    }

    @Test
    fun currentSelectionNormalizesBothDirections() {
        assertEquals(1..4, TextInputOps.currentSelection(bindings("abcdef", cursor = 4, anchor = 1)))
        assertEquals(1..4, TextInputOps.currentSelection(bindings("abcdef", cursor = 1, anchor = 4)))
        assertNull(TextInputOps.currentSelection(bindings("abcdef", cursor = 1, anchor = 1)))
        assertNull(TextInputOps.currentSelection(bindings("abcdef", cursor = 1, anchor = null)))
    }

    private class TestBindings(
        val bindings: TextInputBindings,
        var lastValue: String?,
    ) {
        val cursor get() = bindings.cursor
        val anchor get() = bindings.anchor
    }

    private fun bindings(
        value: String,
        cursor: Int = value.length,
        anchor: Int? = null,
        enableEditing: Boolean = true,
        features: TextEditingFeatures = TextEditingFeatures.Default,
        clipboard: InMemoryClipboard = InMemoryClipboard(),
        onSubmit: (() -> Unit)? = null,
    ): TestBindings {
        var holder: TestBindings? = null
        val bindings = TextInputBindings(
            value = value,
            onValueChange = { next -> holder!!.lastValue = next },
            onSubmit = onSubmit,
            enableEditing = enableEditing,
            features = features,
            clipboard = clipboard,
            cursor = mutableStateOf(cursor),
            anchor = mutableStateOf(anchor),
        )
        return TestBindings(bindings, null).also { holder = it }
    }

    private fun TextInputOps.handleKey(b: TestBindings, event: KeyEvent): Boolean =
        handleKey(b.bindings, event)

    private fun TextInputOps.currentSelection(b: TestBindings): IntRange? =
        currentSelection(b.bindings)
}

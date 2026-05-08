package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.utils.Ansi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListWidgetOpsTest {
    @Test
    fun viewportClampsRowsSelectionAndScroll() {
        assertEquals(ListViewport(rows = 1, selectedIndex = 0, scroll = 0, end = 0), ListWidgetOps.viewport(0, 99, null, 8))
        assertEquals(ListViewport(rows = 3, selectedIndex = 4, scroll = 2, end = 5), ListWidgetOps.viewport(5, 4, 3, 0))
        assertEquals(ListViewport(rows = 3, selectedIndex = 1, scroll = 1, end = 4), ListWidgetOps.viewport(5, 1, 3, 4))
        assertEquals(ListViewport(rows = 10, selectedIndex = 2, scroll = 0, end = 5), ListWidgetOps.viewport(5, 2, 10, 3))
        assertEquals(1, ListWidgetOps.rows(null, 0))
        assertEquals(4, ListWidgetOps.rows(null, 4))
        assertEquals(1, ListWidgetOps.rows(0, 4))
        assertEquals(0, ListWidgetOps.selectedIndex(-4, 3))
        assertEquals(2, ListWidgetOps.selectedIndex(99, 3))
    }

    @Test
    fun singleSelectMoveHandlesArrowsPagesVimAndCtrlBindings() {
        assertEquals(1, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.ARROW_UP), 2, 5, 3))
        assertEquals(3, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.ARROW_DOWN), 2, 5, 3))
        assertEquals(0, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.HOME), 2, 5, 3))
        assertEquals(4, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.END), 2, 5, 3))
        assertEquals(0, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.PAGE_UP), 2, 5, 3))
        assertEquals(4, ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.PAGE_DOWN), 2, 5, 3))
        assertEquals(1, ListWidgetOps.singleSelectMove(KeyEvent('k', Key.CHAR), 2, 5, 3))
        assertEquals(3, ListWidgetOps.singleSelectMove(KeyEvent('j', Key.CHAR), 2, 5, 3))
        assertEquals(0, ListWidgetOps.singleSelectMove(KeyEvent('g', Key.CHAR), 2, 5, 3))
        assertEquals(4, ListWidgetOps.singleSelectMove(KeyEvent('G', Key.CHAR), 2, 5, 3))
        assertEquals(1, ListWidgetOps.singleSelectMove(KeyEvent('p', Key.CHAR, ctrl = true), 2, 5, 3))
        assertEquals(3, ListWidgetOps.singleSelectMove(KeyEvent('n', Key.CHAR, ctrl = true), 2, 5, 3))
    }

    @Test
    fun singleSelectMoveRejectsEmptyListsAndUnknownKeys() {
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('\u0000', Key.ARROW_DOWN), 0, 0, 3))
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('x', Key.CHAR), 0, 5, 3))
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('j', Key.CHAR, alt = true), 0, 5, 3))
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('j', Key.CHAR, ctrl = true, alt = true), 0, 5, 3))
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('x', Key.CHAR, ctrl = true), 0, 5, 3))
        assertNull(ListWidgetOps.singleSelectMove(KeyEvent('\n', Key.ENTER), 0, 5, 3))
    }

    @Test
    fun radioMoveSupportsSubsetOfListNavigation() {
        assertEquals(1, ListWidgetOps.radioMove(KeyEvent('\u0000', Key.ARROW_DOWN), 0, 3))
        assertEquals(0, ListWidgetOps.radioMove(KeyEvent('\u0000', Key.ARROW_UP), 0, 3))
        assertEquals(0, ListWidgetOps.radioMove(KeyEvent('\u0000', Key.HOME), 2, 3))
        assertEquals(2, ListWidgetOps.radioMove(KeyEvent('\u0000', Key.END), 0, 3))
        assertEquals(1, ListWidgetOps.radioMove(KeyEvent('k', Key.CHAR), 2, 3))
        assertEquals(2, ListWidgetOps.radioMove(KeyEvent('j', Key.CHAR), 1, 3))
        assertNull(ListWidgetOps.radioMove(KeyEvent('g', Key.CHAR), 1, 3))
        assertNull(ListWidgetOps.radioMove(KeyEvent('j', Key.CHAR, alt = true), 1, 3))
        assertNull(ListWidgetOps.radioMove(KeyEvent('j', Key.CHAR, ctrl = true), 1, 3))
        assertNull(ListWidgetOps.radioMove(KeyEvent('\n', Key.ENTER), 1, 3))
        assertNull(ListWidgetOps.radioMove(KeyEvent('\u0000', Key.ARROW_DOWN), 0, 0))
    }

    @Test
    fun multiSelectHelpersToggleAndBulkSelect() {
        assertEquals(setOf(1, 3), ListWidgetOps.toggledCheckedIndices(setOf(1), 3))
        assertEquals(setOf(1), ListWidgetOps.toggledCheckedIndices(setOf(1, 3), 3))
        assertEquals(setOf(0, 1, 2), ListWidgetOps.multiSelectAll(3))
        assertEquals(emptySet(), ListWidgetOps.multiSelectAll(0))
    }

    @Test
    fun rowPresentationHelpersExposeStableMarkersAndStyles() {
        assertEquals("▶ ", ListWidgetOps.selectedPrefix(isSelected = true, isFocused = true))
        assertEquals("  ", ListWidgetOps.selectedPrefix(isSelected = true, isFocused = false))
        assertEquals("  ", ListWidgetOps.selectedPrefix(isSelected = false, isFocused = true))
        assertEquals("▶", ListWidgetOps.multiCursorMark(isCursor = true, isFocused = true))
        assertEquals(" ", ListWidgetOps.multiCursorMark(isCursor = false, isFocused = true))
        assertEquals(" ", ListWidgetOps.multiCursorMark(isCursor = true, isFocused = false))
        assertEquals("[x]", ListWidgetOps.checkMark(true))
        assertEquals("[ ]", ListWidgetOps.checkMark(false))
        assertEquals("(●)", ListWidgetOps.radioMark(true))
        assertEquals("( )", ListWidgetOps.radioMark(false))

        val style = ListWidgetOps.selectedStyle(isSelected = true, isFocused = true)
        assertEquals(Ansi.FG_BLACK, style.fg)
        assertEquals(Ansi.BG_CYAN, style.bg)
        assertTrue(style.bold)
        assertEquals(ListWidgetOps.normalRowStyle, ListWidgetOps.selectedStyle(isSelected = false, isFocused = true))
        assertEquals(ListWidgetOps.normalRowStyle, ListWidgetOps.selectedStyle(isSelected = true, isFocused = false))
    }
}

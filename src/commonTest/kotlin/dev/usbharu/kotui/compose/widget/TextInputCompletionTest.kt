package dev.usbharu.kotui.compose.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextInputCompletionTest {

    @Test
    fun filtersAndClampsSelectedItem() {
        val window = buildCompletionWindow(
            query = "ap",
            candidates = listOf("apple", "apricot", "banana"),
            requestedVisibleRows = 4,
            showOnEmptyQuery = false,
            matcher = { query, candidate -> candidate.startsWith(query, ignoreCase = true) },
            selectedIndex = 9,
            scrollIndex = 9,
        )

        assertEquals(listOf("apple", "apricot"), window.items)
        assertEquals(1, window.selectedIndex)
        assertEquals(0, window.scrollIndex)
        assertEquals(2, window.visibleRows)
    }

    @Test
    fun hidesSuggestionsForEmptyQueryUnlessEnabled() {
        val hidden = buildCompletionWindow(
            query = "",
            candidates = listOf("apple", "apricot"),
            requestedVisibleRows = 4,
            showOnEmptyQuery = false,
            matcher = { _, _ -> true },
            selectedIndex = 0,
            scrollIndex = 0,
        )

        assertEquals(emptyList(), hidden.items)
        assertEquals(0, hidden.visibleRows)
        assertEquals(0, hidden.selectedIndex)
        assertEquals(0, hidden.scrollIndex)

        val shown = buildCompletionWindow(
            query = "",
            candidates = listOf("apple", "apricot"),
            requestedVisibleRows = 4,
            showOnEmptyQuery = true,
            matcher = { _, _ -> true },
            selectedIndex = 1,
            scrollIndex = 0,
        )

        assertEquals(listOf("apple", "apricot"), shown.items)
        assertEquals(1, shown.selectedIndex)
        assertEquals(0, shown.scrollIndex)
        assertEquals(2, shown.visibleRows)
    }

    @Test
    fun completionTransformCanRewriteTheChosenValue() {
        val rewritten = applyCompletionValue(
            currentValue = "ap",
            candidate = "apple",
            transform = { current, candidate -> "$current:$candidate" },
        )

        assertEquals("ap:apple", rewritten)
    }

    @Test
    fun completionCommitReturnsReplacement() {
        val commit = commitCompletionValue(
            currentValue = "apple",
            candidate = "apple",
            transform = { _, candidate -> candidate },
        )

        assertEquals("apple", commit.replacement)
    }

    @Test
    fun completionCommitRejectsInvalidReplacement() {
        val commit = commitCompletionValueIfValid(
            currentValue = "12",
            candidate = "12a",
            transform = { _, candidate -> candidate },
            inputValidator = TextInputValidator.DigitsOnly,
        )

        assertNull(commit)
    }

    @Test
    fun completionPopupWidthClampsLongSuggestions() {
        val width = completionPopupWidth(
            items = listOf("a".repeat(200)),
            display = { it },
            maxWidth = 40,
        )

        assertEquals(40, width)
    }

    @Test
    fun completionPopupWidthIncludesPanelPadding() {
        val width = completionPopupWidth(
            items = listOf("apple"),
            display = { it },
            maxWidth = 40,
        )

        assertEquals(9, width)
    }

    @Test
    fun dismissedPopupStaysHiddenForTheSameValue() {
        assertEquals(
            false,
            shouldShowCompletionPopup(
                isFocused = true,
                enableEditing = true,
                hasCandidates = true,
                value = "app",
                dismissedForValue = "app",
            ),
        )

        assertEquals(
            true,
            shouldShowCompletionPopup(
                isFocused = true,
                enableEditing = true,
                hasCandidates = true,
                value = "appl",
                dismissedForValue = "app",
            ),
        )
    }
}

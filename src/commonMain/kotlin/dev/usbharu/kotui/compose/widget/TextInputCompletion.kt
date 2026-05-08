package dev.usbharu.kotui.compose.widget

import dev.usbharu.kotui.utils.displayWidth

internal const val DEFAULT_COMPLETION_POPUP_MAX_WIDTH = 80

internal data class CompletionWindow(
    val items: List<String>,
    val selectedIndex: Int,
    val scrollIndex: Int,
    val visibleRows: Int,
) {
    val isVisible: Boolean get() = items.isNotEmpty() && visibleRows > 0
}

internal val EmptyCompletionWindow = CompletionWindow(
    items = emptyList(),
    selectedIndex = 0,
    scrollIndex = 0,
    visibleRows = 0,
)

internal fun buildCompletionWindow(
    query: String,
    candidates: List<String>,
    requestedVisibleRows: Int,
    showOnEmptyQuery: Boolean,
    matcher: (String, String) -> Boolean,
    selectedIndex: Int,
    scrollIndex: Int,
): CompletionWindow {
    val items = if (!showOnEmptyQuery && query.isEmpty()) {
        emptyList()
    } else {
        candidates.filter { matcher(query, it) }
    }

    if (items.isEmpty()) {
        return EmptyCompletionWindow
    }

    val visibleRows = requestedVisibleRows.coerceAtLeast(1).coerceAtMost(items.size)
    val lastIndex = items.lastIndex
    val clampedSelection = selectedIndex.coerceIn(0, lastIndex)
    val maxScroll = (items.size - visibleRows).coerceAtLeast(0)
    var clampedScroll = scrollIndex.coerceIn(0, maxScroll)

    if (clampedSelection < clampedScroll) {
        clampedScroll = clampedSelection
    }
    if (clampedSelection >= clampedScroll + visibleRows) {
        clampedScroll = clampedSelection - visibleRows + 1
    }
    clampedScroll = clampedScroll.coerceIn(0, maxScroll)

    return CompletionWindow(
        items = items,
        selectedIndex = clampedSelection,
        scrollIndex = clampedScroll,
        visibleRows = visibleRows,
    )
}

internal fun applyCompletionValue(
    currentValue: String,
    candidate: String,
    transform: (String, String) -> String,
): String = transform(currentValue, candidate)

internal data class CompletionCommitResult(
    val replacement: String,
)

internal fun commitCompletionValue(
    currentValue: String,
    candidate: String,
    transform: (String, String) -> String,
): CompletionCommitResult {
    val replacement = applyCompletionValue(currentValue, candidate, transform)
    return CompletionCommitResult(replacement = replacement)
}

internal fun commitCompletionValueIfValid(
    currentValue: String,
    candidate: String,
    transform: (String, String) -> String,
    inputValidator: TextInputValidator,
): CompletionCommitResult? {
    val commit = commitCompletionValue(currentValue, candidate, transform)
    return commit.takeIf { inputValidator.isValid(it.replacement) }
}

internal fun completionPopupWidth(
    items: List<String>,
    display: (String) -> String,
    maxWidth: Int = DEFAULT_COMPLETION_POPUP_MAX_WIDTH,
): Int {
    val contentWidth = items.maxOfOrNull { display(it).displayWidth() } ?: return 0
    val borderedWidth = contentWidth.coerceAtLeast(1) + 4
    return borderedWidth.coerceAtMost(maxWidth.coerceAtLeast(1))
}

internal fun shouldShowCompletionPopup(
    isFocused: Boolean,
    enableEditing: Boolean,
    hasCandidates: Boolean,
    value: String,
    dismissedForValue: String?,
): Boolean {
    return isFocused && enableEditing && hasCandidates && dismissedForValue != value
}

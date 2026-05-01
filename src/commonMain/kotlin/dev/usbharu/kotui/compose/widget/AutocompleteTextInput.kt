package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import dev.usbharu.kotui.compose.modifier.Modifier

@Composable
fun AutocompleteTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    enableEditing: Boolean = true,
    editingFeatures: TextEditingFeatures = TextEditingFeatures.Default,
    inputValidator: TextInputValidator = TextInputValidator.AllowAny,
    visibleRows: Int = 5,
    showOnEmptyQuery: Boolean = false,
    suggestionMatcher: (String, String) -> Boolean = { query, candidate ->
        candidate.startsWith(query, ignoreCase = true)
    },
    suggestionDisplay: (String) -> String = { it },
    suggestionTransform: (String, String) -> String = { _, candidate -> candidate },
) {
    TextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        onSubmit = onSubmit,
        enableEditing = enableEditing,
        editingFeatures = editingFeatures,
        inputValidator = inputValidator,
        completionCandidates = suggestions,
        completionVisibleRows = visibleRows,
        completionShowOnEmptyQuery = showOnEmptyQuery,
        completionMatcher = suggestionMatcher,
        completionDisplay = suggestionDisplay,
        completionTransform = suggestionTransform,
    )
}

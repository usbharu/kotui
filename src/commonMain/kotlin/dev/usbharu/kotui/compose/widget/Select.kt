package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.offset
import dev.usbharu.kotui.compose.modifier.size
import dev.usbharu.kotui.compose.node.LayoutPolicy
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.Key
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import dev.usbharu.kotui.compose.runtime.onKey

/**
 * Collapsed dropdown. Looks like a button while collapsed; expands into a [Modal]
 * hosting a [SelectableList] when activated. The dropdown panel is placed at
 * absolute [dropdownOffsetX]/[dropdownOffsetY] (matching how [Modal] uses
 * [Modifier.offset]).
 */
@Composable
fun <T> Select(
    items: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    dropdownOffsetX: Int = 0,
    dropdownOffsetY: Int = 1,
    dropdownWidth: Int = 24,
    dropdownHeight: Int = 8,
    itemLabel: (T) -> String = { it.toString() },
) {
    validateSelectDimensions(dropdownWidth, dropdownHeight)
    val focusManager = LocalFocusManager.current
    val focusId = remember { focusManager.allocateFocusId() }
    val isFocused = focusManager.isFocused(focusId)

    val expandedState = remember { mutableStateOf(false) }
    val cursorState = remember { mutableStateOf(items.indexOf(selected).coerceAtLeast(0)) }

    val label = itemLabel(selected)
    val displayText = InteractiveWidgetOps.selectText(label, isFocused)
    val focusStyled = InteractiveWidgetOps.focusStyled(modifier)

    val openDropdown: () -> Unit = {
        if (items.isNotEmpty()) {
            cursorState.value = items.indexOf(selected).coerceAtLeast(0)
            expandedState.value = true
        }
    }

    ComposeNode<TuiNode, TuiApplier>(
        factory = {
            TuiNode("Select").apply {
                layoutPolicy = LayoutPolicy.LEAF
                preferredHeight = 1
                focusable = true
            }
        },
        update = {
            reconcile { beginModifierUpdate() }
            set(displayText) { text = it }
            set(focusId) { this.focusId = it }
            set(openDropdown) { cb ->
                onActivate = cb
                onKeyEvent = { ev ->
                    if (InteractiveWidgetOps.isActivate(ev)) { cb(); true } else false
                }
            }
            reconcile { applyModifier(focusStyled) }
        },
    )

    if (expandedState.value) {
        val cursor = cursorState.value.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        Modal(
            modifier = Modifier
                .offset(dropdownOffsetX, dropdownOffsetY)
                .size(dropdownWidth, dropdownHeight),
        ) {
            onKey { ev -> if (ev.key == Key.ESCAPE) expandedState.value = false }
            SelectableList(
                items = items,
                selectedIndex = cursor,
                onSelectedIndexChange = { cursorState.value = it },
                visibleRows = (dropdownHeight - 2).coerceAtLeast(1),
                onActivate = { _, value ->
                    onSelectedChange(value)
                    expandedState.value = false
                },
                requestInitialFocus = true,
                itemLabel = itemLabel,
            )
        }
    }
}

internal fun validateSelectDimensions(width: Int, height: Int) {
    require(width > 0 && height > 0) { "dropdown dimensions must be positive" }
}

package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Invokes [handler] exactly once per physical key press, regardless of how many
 * recompositions happen while the event is latched in [LocalKeyEvent].
 *
 * The raw [LocalKeyEvent] holds the most recently dispatched key event and is
 * read by every recomposition — including those triggered by animation ticks or
 * unrelated state writes. Reading it directly in a `when (keyEvent?.char)`
 * block therefore re-fires the branch on every frame, causing side-effects like
 * `selected++` to advance once per animation frame instead of once per press.
 * This helper de-duplicates via reference identity: each new [KeyEvent]
 * instance is handled exactly once.
 */
@Composable
fun onKey(handler: (KeyEvent) -> Unit) {
    val keyEvent = LocalKeyEvent.current
    var lastHandled by remember { mutableStateOf<KeyEvent?>(null) }
    if (keyEvent != null && keyEvent !== lastHandled) {
        lastHandled = keyEvent
        handler(keyEvent)
    }
}

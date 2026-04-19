package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.staticCompositionLocalOf
import dev.usbharu.kotui.compose.focus.FocusManager

val LocalFocusManager = staticCompositionLocalOf<FocusManager> {
    error("No FocusManager provided. Are you inside runTui {}?")
}

// neverEqualPolicy: two back-to-back key presses with identical char/key would
// be treated as "unchanged" under the default structural-equality policy, so
// the CompositionLocalProvider would skip propagating the second press and
// onKey handlers would never re-fire.
val LocalKeyEvent = compositionLocalOf<KeyEvent?>(policy = neverEqualPolicy()) { null }

val LocalQuit = staticCompositionLocalOf<() -> Unit> {
    error("No quit handler provided. Are you inside runTui {}?")
}

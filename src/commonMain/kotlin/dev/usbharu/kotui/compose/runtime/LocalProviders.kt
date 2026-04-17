package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import dev.usbharu.kotui.compose.focus.FocusManager

val LocalFocusManager = staticCompositionLocalOf<FocusManager> {
    error("No FocusManager provided. Are you inside runTui {}?")
}

val LocalKeyEvent = compositionLocalOf<KeyEvent?> { null }

val LocalQuit = staticCompositionLocalOf<() -> Unit> {
    error("No quit handler provided. Are you inside runTui {}?")
}

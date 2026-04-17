package dev.usbharu.kotui

import dev.usbharu.kotui.compose.runtime.InputEvent

expect fun enableRawMode()
expect fun disableRawMode()

/**
 * Blocking loop that reads terminal input, decodes ANSI escape sequences / bracketed
 * paste, and invokes [onEvent] for every resulting [InputEvent]. Returning false from
 * the callback stops the loop.
 */
expect fun onInputEvent(onEvent: (InputEvent) -> Boolean)

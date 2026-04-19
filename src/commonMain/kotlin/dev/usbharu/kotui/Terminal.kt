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

data class TerminalSize(val cols: Int, val rows: Int)

/**
 * Returns the terminal's current column/row count, or null when the size cannot be
 * determined (e.g. stdout is not a tty). Used by fullscreen mode and the resize
 * poller in the main loop.
 */
expect fun terminalSize(): TerminalSize?

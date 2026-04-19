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
 * determined (e.g. stdout is not a tty). Used by fullscreen mode and as the
 * source of truth inside resize watchers.
 */
expect fun terminalSize(): TerminalSize?

/**
 * Starts watching for terminal size changes. [onResize] is invoked whenever the
 * terminal dimensions actually change; it is NOT fired for the initial size.
 * The returned [TerminalResizeWatcher] must be closed when the TUI exits.
 *
 * Implementations are either event-driven (SIGWINCH on Unix / `resize` event on
 * Node.js) or polling-based (Windows). Callers should not depend on the exact
 * mechanism or timing — only that [onResize] eventually fires after a change.
 */
expect fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher

interface TerminalResizeWatcher {
    fun close()
}

package dev.usbharu.kotui.utils

object Ansi {
    const val ESC = "\u001B"
    const val CSI = "$ESC["

    // Screen
    const val ALTERNATE_SCREEN_ON = "$CSI?1049h"
    const val ALTERNATE_SCREEN_OFF = "$CSI?1049l"
    const val CLEAR_SCREEN = "${CSI}2J"
    const val CLEAR_SCREEN_TO_END = "${CSI}0J"
    const val CLEAR_SCREEN_TO_START = "${CSI}1J"

    // Line
    const val CLEAR_LINE = "${CSI}2K"
    const val CLEAR_LINE_TO_END = "${CSI}0K"
    const val CLEAR_LINE_TO_START = "${CSI}1K"

    // Cursor
    const val CURSOR_HOME = "${CSI}H"
    const val CURSOR_HIDE = "$CSI?25l"
    const val CURSOR_SHOW = "$CSI?25h"
    const val CURSOR_SAVE = "${CSI}s"
    const val CURSOR_RESTORE = "${CSI}u"

    fun cursorTo(row: Int, col: Int): String {
        require(row >= 1 && col >= 1) { "cursor coordinates are one-based" }
        return "$CSI$row;${col}H"
    }
    fun cursorUp(n: Int = 1): String = cursorMove(n, 'A')
    fun cursorDown(n: Int = 1): String = cursorMove(n, 'B')
    fun cursorForward(n: Int = 1): String = cursorMove(n, 'C')
    fun cursorBack(n: Int = 1): String = cursorMove(n, 'D')

    private fun cursorMove(n: Int, command: Char): String {
        require(n >= 0) { "cursor distance must be non-negative" }
        return "$CSI$n$command"
    }

    // Style
    const val RESET = "${CSI}0m"
    const val BOLD = "${CSI}1m"
    const val DIM = "${CSI}2m"
    const val ITALIC = "${CSI}3m"
    const val UNDERLINE = "${CSI}4m"
    const val BLINK = "${CSI}5m"
    const val REVERSE = "${CSI}7m"
    const val HIDDEN = "${CSI}8m"
    const val STRIKETHROUGH = "${CSI}9m"

    // Foreground colors
    const val FG_BLACK = "${CSI}30m"
    const val FG_RED = "${CSI}31m"
    const val FG_GREEN = "${CSI}32m"
    const val FG_YELLOW = "${CSI}33m"
    const val FG_BLUE = "${CSI}34m"
    const val FG_MAGENTA = "${CSI}35m"
    const val FG_CYAN = "${CSI}36m"
    const val FG_WHITE = "${CSI}37m"
    const val FG_DEFAULT = "${CSI}39m"

    // Background colors
    const val BG_BLACK = "${CSI}40m"
    const val BG_RED = "${CSI}41m"
    const val BG_GREEN = "${CSI}42m"
    const val BG_YELLOW = "${CSI}43m"
    const val BG_BLUE = "${CSI}44m"
    const val BG_MAGENTA = "${CSI}45m"
    const val BG_CYAN = "${CSI}46m"
    const val BG_WHITE = "${CSI}47m"
    const val BG_DEFAULT = "${CSI}49m"

    // Bright foreground colors
    const val FG_BRIGHT_BLACK = "${CSI}90m"
    const val FG_BRIGHT_RED = "${CSI}91m"
    const val FG_BRIGHT_GREEN = "${CSI}92m"
    const val FG_BRIGHT_YELLOW = "${CSI}93m"
    const val FG_BRIGHT_BLUE = "${CSI}94m"
    const val FG_BRIGHT_MAGENTA = "${CSI}95m"
    const val FG_BRIGHT_CYAN = "${CSI}96m"
    const val FG_BRIGHT_WHITE = "${CSI}97m"

    // Bright background colors
    const val BG_BRIGHT_BLACK = "${CSI}100m"
    const val BG_BRIGHT_RED = "${CSI}101m"
    const val BG_BRIGHT_GREEN = "${CSI}102m"
    const val BG_BRIGHT_YELLOW = "${CSI}103m"
    const val BG_BRIGHT_BLUE = "${CSI}104m"
    const val BG_BRIGHT_MAGENTA = "${CSI}105m"
    const val BG_BRIGHT_CYAN = "${CSI}106m"
    const val BG_BRIGHT_WHITE = "${CSI}107m"

    // 256-color / RGB
    fun fg256(n: Int): String { requireByte(n); return "${CSI}38;5;${n}m" }
    fun bg256(n: Int): String { requireByte(n); return "${CSI}48;5;${n}m" }
    fun fgRgb(r: Int, g: Int, b: Int): String { requireRgb(r, g, b); return "${CSI}38;2;$r;$g;${b}m" }
    fun bgRgb(r: Int, g: Int, b: Int): String { requireRgb(r, g, b); return "${CSI}48;2;$r;$g;${b}m" }

    private fun requireByte(value: Int) {
        require(value in 0..255) { "color component must be in 0..255" }
    }

    private fun requireRgb(r: Int, g: Int, b: Int) {
        requireByte(r); requireByte(g); requireByte(b)
    }

    // Bracketed paste mode. When enabled, pasted text is wrapped with
    // ESC[200~ and ESC[201~ so the TUI can treat it as a single paste event.
    const val BRACKETED_PASTE_ON = "$CSI?2004h"
    const val BRACKETED_PASTE_OFF = "$CSI?2004l"

    /** OSC 52 sequence that asks the terminal to place [text] on the system clipboard. */
    fun osc52Copy(text: String): String = "${ESC}]52;c;${Base64.encode(text)}\u0007"
}

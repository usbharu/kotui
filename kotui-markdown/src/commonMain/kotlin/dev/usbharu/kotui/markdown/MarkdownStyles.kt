package dev.usbharu.kotui.markdown

import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

data class MarkdownStyles(
    val h1: Style = Style(fg = Ansi.FG_BRIGHT_CYAN, bold = true, underline = true),
    val h2: Style = Style(fg = Ansi.FG_CYAN, bold = true),
    val h3: Style = Style(fg = Ansi.FG_BRIGHT_MAGENTA, bold = true),
    val h4: Style = Style(fg = Ansi.FG_MAGENTA, bold = true),
    val h5: Style = Style(fg = Ansi.FG_BRIGHT_BLUE, bold = true),
    val h6: Style = Style(fg = Ansi.FG_BLUE, bold = true),
    val paragraph: Style = Style(),
    val bold: Style = Style(bold = true),
    val italic: Style = Style(fg = Ansi.FG_BRIGHT_WHITE, underline = true),
    val strike: Style = Style(reverse = true),
    val inlineCode: Style = Style(fg = Ansi.FG_YELLOW, bg = Ansi.BG_BRIGHT_BLACK),
    val link: Style = Style(fg = Ansi.FG_BRIGHT_BLUE, underline = true),
    val quoteBar: Style = Style(fg = Ansi.FG_BRIGHT_BLACK, bold = true),
    val quoteText: Style = Style(fg = Ansi.FG_BRIGHT_BLACK),
    val listBullet: Style = Style(fg = Ansi.FG_CYAN, bold = true),
    val listNumber: Style = Style(fg = Ansi.FG_CYAN, bold = true),
    val imageFallback: Style = Style(fg = Ansi.FG_BRIGHT_BLACK),
    val errorStyle: Style = Style(fg = Ansi.FG_RED, bold = true),
) {
    fun headerStyle(level: Int): Style = when (level) {
        1 -> h1
        2 -> h2
        3 -> h3
        4 -> h4
        5 -> h5
        else -> h6
    }
}

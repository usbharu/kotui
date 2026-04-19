package dev.usbharu.kotui.markdown

import androidx.compose.runtime.Composable
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.runtime.LocalQuit
import dev.usbharu.kotui.compose.runtime.onKey
import dev.usbharu.kotui.compose.runtime.runTui
import dev.usbharu.kotui.compose.widget.Column
import dev.usbharu.kotui.compose.widget.Divider
import dev.usbharu.kotui.compose.widget.Text
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi

private val SAMPLE = """
# kotui-markdown demo

A tour of **bold**, *italic*, ~~strike~~, and `inline code`.

## Features
- Nested lists
  - like this
  - and this
- Inline [links](https://example.com)

1. Ordered one
2. Ordered two

> A quote.
> > A nested quote with `code` inside.

---

![cat](https://example.com/cat.png)

End of demo — press q to quit.
""".trimIndent()

@Composable
private fun DemoApp() {
    val quit = LocalQuit.current
    onKey { ev -> if (ev.char == 'q' || ev.char == '\u0003') quit() }

    Column {
        Text(
            " kotui-markdown demo — q to quit ",
            Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)),
        )
        Divider()
        Markdown(SAMPLE)
    }
}

fun main() = runTui(fullscreen = true) { DemoApp() }

package dev.usbharu.kotui.image

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

private const val SAMPLE_URL =
    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Kotlin_icon_%282021-present%29.svg/960px-Kotlin_icon_%282021-present%29.svg.png"

@Composable
private fun DemoApp() {
    val quit = LocalQuit.current
    onKey { ev -> if (ev.char == 'q' || ev.char == '\u0003') quit() }

    Column {
        Text(
            " kotui-image demo — q to quit ",
            Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)),
        )
        Divider()
        Text("URL: $SAMPLE_URL")
        Divider()
        NetworkImage(url = SAMPLE_URL, maxPixelWidth = 480, maxPixelHeight = 320)
    }
}

fun main() = runTui(fullscreen = true) { DemoApp() }

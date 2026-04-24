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
import dev.usbharu.kotui.utils.SixelSupport

private const val SAMPLE_URL =
    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Kotlin_icon_%282021-present%29.svg/960px-Kotlin_icon_%282021-present%29.svg.png"

@Composable
private fun DemoApp() {
    val quit = LocalQuit.current
    onKey { ev -> if (ev.char == 'q' || ev.char == '') quit() }

    val caps = SixelSupport.cached
    val capsLabel = if (caps == null) {
        "SixelSupport.cached = null"
    } else {
        "caps: sixel=${caps.sixelSupported} kitty=${caps.kittySupported} cell=${caps.cellPixelWidth}×${caps.cellPixelHeight}"
    }

    Column {
        Text(
            " kotui-image demo — q to quit ",
            Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)),
        )
        Text(
            " $capsLabel ",
            Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_WHITE)),
        )
        Divider()
        Text(" [1] normal — image on capable terminals, fallback text otherwise: ")
        NetworkImage(
            url = SAMPLE_URL,
            fallbackText = "[sixel/kitty unsupported — placeholder #1]",
            maxPixelWidth = 240,
            maxPixelHeight = 160,
            loading = { Text(" fetching #1… ", Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_YELLOW))) },
        )
        Divider()
        Text(" [2] forceFallback=true — colored placeholder block sized like the image: ")
        NetworkImage(
            url = SAMPLE_URL,
            fallbackText = "[forced fallback — image would render here on capable terminals]",
            fallbackStyle = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_BRIGHT_MAGENTA, bold = true),
            forceFallback = true,
            maxPixelWidth = 240,
            maxPixelHeight = 160,
            loading = { Text(" fetching #2… ", Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_YELLOW))) },
        )
    }
}

fun main() = runTui(fullscreen = true) { DemoApp() }

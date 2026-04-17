package dev.usbharu.kotui

import androidx.compose.runtime.*
import dev.usbharu.kotui.compose.modifier.*
import dev.usbharu.kotui.compose.runtime.LocalKeyEvent
import dev.usbharu.kotui.compose.runtime.LocalQuit
import dev.usbharu.kotui.compose.runtime.runTui
import dev.usbharu.kotui.compose.widget.*
import dev.usbharu.kotui.core.Style
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.SixelSupport

@Composable
fun App() {
    var screen by remember { mutableStateOf("list") }
    var tasks by remember { mutableStateOf(listOf("Sample task 1", "Sample task 2")) }
    var editIndex by remember { mutableStateOf(-1) }

    val quit = LocalQuit.current
    val keyEvent = LocalKeyEvent.current

    if (keyEvent?.char == 'q' && screen == "list") {
        quit()
    }
    if (keyEvent?.char == 's' && screen == "list") {
        screen = "showcase"
    }
    if (keyEvent?.char == 'i' && screen == "list") {
        screen = "image"
    }
    if (keyEvent?.char == '\u001B' && (screen == "showcase" || screen == "image")) {
        screen = "list"
    }

    when (screen) {
        "list" -> TaskList(
            tasks = tasks,
            onAdd = { screen = "editor"; editIndex = -1 },
            onEdit = { screen = "editor"; editIndex = it },
            onDelete = { i -> tasks = tasks.filterIndexed { idx, _ -> idx != i } }
        )
        "editor" -> TaskEditor(
            initialText = if (editIndex >= 0) tasks[editIndex] else "",
            onSave = { text ->
                tasks = if (editIndex >= 0)
                    tasks.mapIndexed { i, t -> if (i == editIndex) text else t }
                else tasks + text
                screen = "list"
            },
            onCancel = { screen = "list" }
        )
        "showcase" -> Showcase()
        "image" -> ImageTest()
    }
}

@Composable
fun ImageTest() {
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))
    val caps = SixelSupport.cached
    val status = when {
        caps == null -> "not probed"
        caps.kittySupported -> "kitty gfx (cell ${caps.cellPixelWidth}x${caps.cellPixelHeight}px)"
        caps.sixelSupported -> "sixel (cell ${caps.cellPixelWidth}x${caps.cellPixelHeight}px)"
        else -> "not supported"
    }
    val image = remember { demoGradient() }
    Column {
        Text("  === Sixel image test ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
        Text("  Sixel: $status", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        Text(
            "  Image: ${image.pixelWidth}x${image.pixelHeight}px  (" +
                "${image.cellWidth}x${image.cellHeight} cells at ${image.cellPixelWidth}x${image.cellPixelHeight})",
            dim
        )
        Text("", dim)
        Image(image)
        Text("", dim)
        Text("  Esc=Back", dim)
    }
}

@Composable
fun TaskList(
    tasks: List<String>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))

    val keyEvent = LocalKeyEvent.current
    if (keyEvent?.char == '?') {
        showHelp = !showHelp
    }

    Box {
        Column {
            Text("  === Task Manager ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
            Text("  Compose Runtime TUI Demo", dim)
            Text("  " + "-".repeat(50), dim)

            Text("  Tasks (${tasks.size}):", Modifier.style(Style(fg = Ansi.FG_YELLOW, bold = true)))
            tasks.forEachIndexed { i, task ->
                Text("    [${i + 1}] $task")
            }
            if (tasks.isEmpty()) {
                Text("    (no tasks)", dim)
            }

            Text("  " + "-".repeat(50), dim)

            Row {
                Button("New Task") { onAdd() }
                if (tasks.isNotEmpty()) {
                    Button("Edit #1") { onEdit(0) }
                    Button("Delete #1") { onDelete(0) }
                }
                Button("Help ?") { showHelp = !showHelp }
            }

            Text("  Tab=Focus  ?=Help  s=Showcase  i=Image  q=Quit", dim)
        }

        if (showHelp) {
            Modal("Help", Modifier.offset(15, 4).size(50, 10).style(Style(fg = Ansi.FG_YELLOW, bg = Ansi.BG_BLUE))) {
                val ms = Modifier.style(Style(fg = Ansi.FG_WHITE, bg = Ansi.BG_BLUE))
                Text("", ms)
                Text(" Tab     - Cycle focus within scope", ms)
                Text(" Enter   - Activate button", ms)
                Text(" ?       - Toggle help", ms)
                Text(" q       - Quit (from list screen)", ms)
                Text("", ms)
                Text(" Z-index overlay + focusScope!", Modifier.style(Style(fg = Ansi.FG_BRIGHT_CYAN, bg = Ansi.BG_BLUE, bold = true)))
                Button("Close") { showHelp = false }
            }
        }
    }
}

@Composable
fun TaskEditor(initialText: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))

    val keyEvent = LocalKeyEvent.current
    if (keyEvent?.char == '\u001B') {
        onCancel()
    }

    Column(modifier = Modifier.focusScope()) {
        Text("  === Task Editor ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
        Text("  " + "-".repeat(50), dim)
        Text("  Task name:", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        TextInput(
            value = text,
            onValueChange = { text = it },
            placeholder = "Enter task name...",
            modifier = Modifier.width(60),
            onSubmit = { if (text.isNotBlank()) onSave(text) }
        )
        Text("  " + "-".repeat(50), dim)
        Row {
            Button("Save") { if (text.isNotBlank()) onSave(text) }
            Button("Cancel") { onCancel() }
        }
        Text("  Esc=Cancel  Enter=Save  Tab=Focus", dim)
    }
}

@Composable
fun Showcase() {
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))
    val title = Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true))

    Column {
        Text("  === Component Showcase ===", title)
        Divider()

        Text("  Badges:", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        Row {
            Badge("NEW")
            Badge("BETA", Modifier.style(Style(fg = Ansi.FG_WHITE, bg = Ansi.BG_BLUE, bold = true)))
            Badge("DONE", Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_GREEN, bold = true)))
            Badge("WARN", Modifier.style(Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_YELLOW, bold = true)))
        }

        Spacer(Modifier.height(1))

        Text("  Progress bars:", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        ProgressBar(progress = 0.25f, width = 24)
        ProgressBar(progress = 0.6f, width = 24, modifier = Modifier.style(Style(fg = Ansi.FG_GREEN)))
        ProgressBar(progress = 1.0f, width = 24, modifier = Modifier.style(Style(fg = Ansi.FG_MAGENTA, bold = true)))

        Spacer(Modifier.height(1))

        Text("  Spinners (auto-animated):", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        Row {
            Text("  braille ")
            Spinner(
                chars = SpinnerFrames.BRAILLE,
                intervalMs = 80,
                modifier = Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)),
            )
            Text("   classic ")
            Spinner(
                chars = SpinnerFrames.CLASSIC,
                intervalMs = 120,
                modifier = Modifier.style(Style(fg = Ansi.FG_GREEN, bold = true)),
            )
            Text("   arrow ")
            Spinner(
                chars = SpinnerFrames.ARROW,
                intervalMs = 100,
                modifier = Modifier.style(Style(fg = Ansi.FG_MAGENTA, bold = true)),
            )
            Text("   circle ")
            Spinner(
                chars = SpinnerFrames.CIRCLE,
                intervalMs = 150,
                modifier = Modifier.style(Style(fg = Ansi.FG_YELLOW, bold = true)),
            )
            Text("   bar ")
            Spinner(
                chars = SpinnerFrames.BAR,
                intervalMs = 70,
                modifier = Modifier.style(Style(fg = Ansi.FG_BRIGHT_CYAN, bold = true)),
            )
            Text("   dots ")
            Spinner(
                chars = SpinnerFrames.DOTS,
                intervalMs = 180,
                modifier = Modifier.style(Style(fg = Ansi.FG_BRIGHT_MAGENTA)),
            )
        }

        Divider(char = '=')

        Text("  Layout: Row + VerticalDivider + Center", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        Panel("Split", Modifier.size(60, 7)) {
            Row {
                Center(Modifier.size(28, 5)) {
                    Text("LEFT", Modifier.style(Style(fg = Ansi.FG_GREEN, bold = true)))
                }
                VerticalDivider(modifier = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK)))
                Center(Modifier.size(28, 5)) {
                    Text("RIGHT", Modifier.style(Style(fg = Ansi.FG_BLUE, bold = true)))
                }
            }
        }

        Divider()
        Text("  Esc=Back to list", dim)
    }
}

private fun demoGradient(): TerminalImage {
    val w = 64
    val h = 32
    val rgba = ByteArray(w * h * 4)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val base = (y * w + x) * 4
            rgba[base]     = ((x * 255) / (w - 1)).toByte()
            rgba[base + 1] = ((y * 255) / (h - 1)).toByte()
            rgba[base + 2] = (((x + y) * 255) / (w + h - 2)).toByte()
            rgba[base + 3] = 0xFF.toByte()
        }
    }
    return TerminalImage(rgba, w, h, fallbackText = "[gradient]")
}

fun main() = runTui { App() }

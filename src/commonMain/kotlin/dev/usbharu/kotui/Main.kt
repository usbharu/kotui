package dev.usbharu.kotui

import androidx.compose.runtime.*
import dev.usbharu.kotui.compose.layout.AlignItems
import dev.usbharu.kotui.compose.layout.JustifyContent
import dev.usbharu.kotui.compose.modifier.*
import dev.usbharu.kotui.compose.runtime.LocalQuit
import dev.usbharu.kotui.compose.runtime.onKey
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

    // Global shortcuts while we're not in the editor (which needs text input).
    if (screen != "editor") {
        onKey { ev ->
            when (ev.char) {
                'q' -> quit()
                's' -> screen = "showcase"
                'i' -> screen = "image"
                'f' -> screen = "flex"
                'l' -> screen = "list"
                'm' -> screen = "select"
                '\u001B' -> if (screen != "list") screen = "list"
            }
        }
    }

    AppChrome(screen) {
        when (screen) {
            "list" -> TaskList(
                tasks = tasks,
                onAdd = { screen = "editor"; editIndex = -1 },
                onEdit = { screen = "editor"; editIndex = it },
                onDelete = { i -> tasks = tasks.filterIndexed { idx, _ -> idx != i } },
            )
            "editor" -> TaskEditor(
                initialText = if (editIndex >= 0) tasks[editIndex] else "",
                suggestions = tasks + listOf("Write tests", "Refactor autocomplete", "Ship patch"),
                onSave = { text ->
                    tasks = if (editIndex >= 0)
                        tasks.mapIndexed { i, t -> if (i == editIndex) text else t }
                    else tasks + text
                    screen = "list"
                },
                onCancel = { screen = "list" },
            )
            "showcase" -> Showcase()
            "image" -> ImageTest()
            "flex" -> FlexShowcase()
            "select" -> SelectShowcase()
        }
    }
}

@Composable
private fun AppChrome(screen: String, content: @Composable () -> Unit) {
    val headerStyle = Style(fg = Ansi.FG_BLACK, bg = Ansi.BG_CYAN, bold = true)
    val footerStyle = Style(fg = Ansi.FG_BRIGHT_BLACK)

    Column(gap = 0) {
        Row(modifier = Modifier.height(1).style(headerStyle), justifyContent = JustifyContent.SpaceBetween) {
            Text(" kotui — fullscreen + flex TUI ", Modifier.style(headerStyle))
            Text(" screen: $screen ", Modifier.style(headerStyle))
        }

        Box(modifier = Modifier.weight(1f).flexBasis(0)) {
            content()
        }

        Row(modifier = Modifier.height(1), justifyContent = JustifyContent.SpaceBetween) {
            Text(" l=list  s=showcase  i=image  f=flex  m=select ", Modifier.style(footerStyle))
            Text(" Esc=back  q=quit ", Modifier.style(footerStyle))
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<String>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))
    val selectedTask = tasks.getOrNull(selectedIndex)

    LaunchedEffect(tasks.size) {
        selectedIndex = if (tasks.isEmpty()) 0 else selectedIndex.coerceIn(0, tasks.lastIndex)
    }

    onKey { ev -> if (ev.char == '?') showHelp = !showHelp }

    Box {
        Column {
            Text("  === Task Manager ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
            Text("  Compose Runtime TUI Demo", dim)
            Text("  " + "-".repeat(50), dim)

            Text("  Tasks (${tasks.size}):", Modifier.style(Style(fg = Ansi.FG_YELLOW, bold = true)))
            if (tasks.isEmpty()) {
                Text("    (no tasks)", dim)
            } else {
                SelectableList(
                    items = tasks,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { selectedIndex = it },
                    onActivate = { index, _ -> onEdit(index) },
                    requestInitialFocus = true,
                    visibleRows = 6,
                    itemLabel = { it },
                )
                selectedTask?.let {
                    Text("  Selected: [${selectedIndex + 1}] $it", Modifier.style(Style(fg = Ansi.FG_BRIGHT_CYAN)))
                }
            }

            Text("  " + "-".repeat(50), dim)

            Row {
                Button("New Task") { onAdd() }
                if (tasks.isNotEmpty()) {
                    Button("Edit Selected") { onEdit(selectedIndex.coerceIn(0, tasks.lastIndex)) }
                    Button("Delete Selected") { onDelete(selectedIndex.coerceIn(0, tasks.lastIndex)) }
                }
                Button("Help ?") { showHelp = !showHelp }
            }

            Text("  Tab=Focus  Enter=Edit selected  ?=Help", dim)
        }

        if (showHelp) {
            Modal("Help", Modifier.offset(15, 4).size(50, 10).style(Style(fg = Ansi.FG_YELLOW, bg = Ansi.BG_BLUE))) {
                val ms = Modifier.style(Style(fg = Ansi.FG_WHITE, bg = Ansi.BG_BLUE))
                Text("", ms)
                Text(" Up/Down - Move task selection", ms)
                Text(" Enter   - Edit selected task", ms)
                Text(" Delete  - Use Delete Selected button", ms)
                Text(" Tab     - Cycle focus within scope", ms)
                Text(" ?       - Toggle help", ms)
                Text(" q       - Quit", ms)
                Text("", ms)
                Text(" Z-index overlay + focusScope!", Modifier.style(Style(fg = Ansi.FG_BRIGHT_CYAN, bg = Ansi.BG_BLUE, bold = true)))
                Button("Close") { showHelp = false }
            }
        }
    }
}

@Composable
private fun TaskEditor(
    initialText: String,
    suggestions: List<String>,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))

    onKey { ev -> if (ev.char == '\u001B') onCancel() }

    Column(modifier = Modifier.focusScope()) {
        Text("  === Task Editor ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
        Text("  " + "-".repeat(50), dim)
        Text("  Task name:", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        AutocompleteTextInput(
            value = text,
            onValueChange = { text = it },
            placeholder = "Enter task name...",
            modifier = Modifier.width(60),
            onSubmit = { if (text.isNotBlank()) onSave(text) },
            suggestions = suggestions,
            visibleRows = 4,
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
private fun Showcase() {
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
            Spinner(chars = SpinnerFrames.BRAILLE, intervalMs = 80,
                modifier = Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
            Text("   classic ")
            Spinner(chars = SpinnerFrames.CLASSIC, intervalMs = 120,
                modifier = Modifier.style(Style(fg = Ansi.FG_GREEN, bold = true)))
            Text("   arrow ")
            Spinner(chars = SpinnerFrames.ARROW, intervalMs = 100,
                modifier = Modifier.style(Style(fg = Ansi.FG_MAGENTA, bold = true)))
            Text("   circle ")
            Spinner(chars = SpinnerFrames.CIRCLE, intervalMs = 150,
                modifier = Modifier.style(Style(fg = Ansi.FG_YELLOW, bold = true)))
            Text("   bar ")
            Spinner(chars = SpinnerFrames.BAR, intervalMs = 70,
                modifier = Modifier.style(Style(fg = Ansi.FG_BRIGHT_CYAN, bold = true)))
            Text("   dots ")
            Spinner(chars = SpinnerFrames.DOTS, intervalMs = 180,
                modifier = Modifier.style(Style(fg = Ansi.FG_BRIGHT_MAGENTA)))
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
    }
}

@Composable
private fun ImageTest() {
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
        Text("  === Image test ===", Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true)))
        Text("  Support: $status", Modifier.style(Style(fg = Ansi.FG_YELLOW)))
        Text(
            "  Image: ${image.pixelWidth}x${image.pixelHeight}px  (" +
                "${image.cellWidth}x${image.cellHeight} cells at ${image.cellPixelWidth}x${image.cellPixelHeight})",
            dim,
        )
        Spacer(Modifier.height(1))
        Image(image)
    }
}

@Composable
private fun FlexShowcase() {
    var selected by remember { mutableStateOf(0) }
    val dim = Style(fg = Ansi.FG_BRIGHT_BLACK)
    val menuItems = listOf("Overview", "Details", "Settings")

    Row(gap = 1, alignItems = AlignItems.Stretch) {
        // Sidebar — fixed flex basis, no grow.
        Panel(title = "Menu", modifier = Modifier.flexBasis(22)) {
            SelectableList(
                items = menuItems,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
            )
            Spacer(Modifier.height(1))
            Text(" Tab to focus, j/k or arrows", Modifier.style(dim))
        }

        // Main content grows to fill remaining width.
        Panel(title = "Main", modifier = Modifier.weight(1f).flexBasis(0)) {
            when (selected) {
                0 -> FlexOverview()
                1 -> FlexDetails()
                else -> FlexSettings()
            }
        }

        // Status pane — fixed width.
        Panel(title = "Info", modifier = Modifier.width(22)) {
            Text(" selected: $selected", Modifier.style(dim))
            Text(" resize me →", Modifier.style(dim))
            Spacer(Modifier.height(1))
            Text(" ok", Modifier.style(Style(fg = Ansi.FG_GREEN, bold = true)))
        }
    }
}

@Composable
private fun FlexOverview() {
    Column {
        Text(" weight(1f) / flexBasis / justify / align",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_WHITE, bold = true)))
        Spacer(Modifier.height(1))
        Text(" Sidebar: Modifier.flexBasis(22) — fixed starting width.")
        Text(" Main:    Modifier.weight(1f) — absorbs the remaining width.")
        Text(" Info:    Modifier.width(22) — fixed, doesn't flex.")
        Spacer(Modifier.height(1))
        Text(" Resize the terminal — layout reflows in real time.",
            Modifier.style(Style(fg = Ansi.FG_YELLOW)))
    }
}

@Composable
private fun FlexDetails() {
    Column(gap = 1) {
        Text(" 3-way split with weight 1:2:1",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_WHITE, bold = true)))
        Row(modifier = Modifier.height(5), gap = 1, alignItems = AlignItems.Stretch) {
            Panel(title = "A", modifier = Modifier.weight(1f).flexBasis(0)) {
                Text(" w=1")
            }
            Panel(title = "B", modifier = Modifier.weight(2f).flexBasis(0)) {
                Text(" w=2 (double)")
            }
            Panel(title = "C", modifier = Modifier.weight(1f).flexBasis(0)) {
                Text(" w=1")
            }
        }

        Text(" justifyContent = SpaceBetween",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_WHITE, bold = true)))
        Row(justifyContent = JustifyContent.SpaceBetween) {
            Badge("left")
            Badge("center")
            Badge("right")
        }

        Text(" justifyContent = SpaceAround",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_WHITE, bold = true)))
        Row(justifyContent = JustifyContent.SpaceAround) {
            Badge("a")
            Badge("b")
            Badge("c")
        }
    }
}

@Composable
private fun FlexSettings() {
    Column(justifyContent = JustifyContent.Center, alignItems = AlignItems.Center) {
        Text("Settings",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_MAGENTA, bold = true)))
        Spacer(Modifier.height(1))
        Text("JustifyContent.Center + AlignItems.Center")
        Text("centers the children on both axes.",
            Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK)))
    }
}

@Composable
private fun SelectShowcase() {
    val dim = Modifier.style(Style(fg = Ansi.FG_BRIGHT_BLACK))
    val title = Modifier.style(Style(fg = Ansi.FG_CYAN, bold = true))

    val fruits = remember { listOf("Apple", "Banana", "Cherry", "Durian", "Elderberry", "Fig", "Grape", "Honeydew", "Kiwi", "Lemon") }
    var listIndex by remember { mutableStateOf(0) }

    var checkedA by remember { mutableStateOf(false) }
    var checkedB by remember { mutableStateOf(true) }

    val sizes = remember { listOf("Small", "Medium", "Large", "X-Large") }
    var size by remember { mutableStateOf(sizes[1]) }

    val colors = remember { listOf("Red", "Green", "Blue", "Yellow", "Magenta", "Cyan") }
    var color by remember { mutableStateOf(colors[0]) }

    val tags = remember { listOf("urgent", "backend", "bug", "feature", "docs", "test", "chore", "refactor") }
    var tagCursor by remember { mutableStateOf(0) }
    var checkedTags by remember { mutableStateOf(setOf(0, 2)) }

    Column(gap = 1) {
        Text("  === Select Showcase ===", title)
        Text("  Tab=next focus  Enter=activate  Space=toggle", dim)

        Row(gap = 2, alignItems = AlignItems.Stretch) {
            Panel(title = "SelectableList (single)", modifier = Modifier.flexBasis(32)) {
                SelectableList(
                    items = fruits,
                    selectedIndex = listIndex,
                    onSelectedIndexChange = { listIndex = it },
                    visibleRows = 5,
                    onActivate = { _, _ -> /* demo */ },
                )
                Text("  picked: ${fruits[listIndex]}", dim)
            }

            Panel(title = "MultiSelectList", modifier = Modifier.flexBasis(34)) {
                MultiSelectList(
                    items = tags,
                    cursorIndex = tagCursor,
                    onCursorIndexChange = { tagCursor = it },
                    checkedIndices = checkedTags,
                    onCheckedIndicesChange = { checkedTags = it },
                    visibleRows = 5,
                )
                Text("  " + checkedTags.sorted().joinToString(",") { tags[it] }, dim)
                Text("  Space=toggle  Ctrl+A=all  Ctrl+D=none", dim)
            }
        }

        Row(gap = 2, alignItems = AlignItems.Stretch) {
            Panel(title = "Checkbox", modifier = Modifier.flexBasis(28)) {
                Checkbox(checkedA, { checkedA = it }, label = "Enable cache")
                Checkbox(checkedB, { checkedB = it }, label = "Dark theme")
            }

            Panel(title = "RadioGroup: size", modifier = Modifier.flexBasis(22)) {
                RadioGroup(
                    options = sizes,
                    selected = size,
                    onSelectedChange = { size = it },
                )
                Text("  size = $size", dim)
            }

            Panel(title = "Select: color", modifier = Modifier.flexBasis(30)) {
                Select(
                    items = colors,
                    selected = color,
                    onSelectedChange = { color = it },
                    dropdownOffsetX = 4,
                    dropdownOffsetY = 14,
                    dropdownWidth = 20,
                    dropdownHeight = 8,
                )
                Text("  color = $color", dim)
                Text("  Enter to open", dim)
            }
        }
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

fun main() = runTui(fullscreen = true) { App() }

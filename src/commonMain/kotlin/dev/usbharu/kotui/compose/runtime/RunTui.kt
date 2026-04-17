package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.Clipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.clipboard.SystemClipboard
import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.layout.LayoutEngine
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.render.TuiRenderer
import dev.usbharu.kotui.disableRawMode
import dev.usbharu.kotui.enableRawMode
import dev.usbharu.kotui.onInputEvent
import dev.usbharu.kotui.utils.Ansi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun runTui(
    screenWidth: Int = 80,
    screenHeight: Int = 24,
    clipboard: Clipboard = SystemClipboard(),
    content: @Composable () -> Unit,
) {
    val frameClock = BroadcastFrameClock()
    val coroutineContext = Dispatchers.Unconfined + frameClock
    val scope = CoroutineScope(coroutineContext)

    val focusManager = FocusManager()
    val renderer = TuiRenderer(screenWidth, screenHeight)
    val rootNode = TuiNode("Root").apply {
        layoutPolicy = dev.usbharu.kotui.compose.node.LayoutPolicy.BOX
    }
    val keyEventState = mutableStateOf<KeyEvent?>(null)

    var running = true
    val quit: () -> Unit = { running = false }

    val recomposer = Recomposer(coroutineContext)
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = Composition(TuiApplier(rootNode), recomposer)
    composition.setContent {
        CompositionLocalProvider(
            LocalFocusManager provides focusManager,
            LocalKeyEvent provides keyEventState.value,
            LocalQuit provides quit,
            LocalClipboard provides clipboard,
        ) {
            content()
        }
    }

    fun driveFrame() {
        Snapshot.sendApplyNotifications()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            frameClock.sendFrame(frameTimeNanos())
        }
        LayoutEngine.layout(rootNode, screenWidth, screenHeight)
        focusManager.autoFocus(rootNode)
        renderer.render(rootNode, focusManager)
    }

    var cleanedUp = false
    fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
        print(Ansi.BRACKETED_PASTE_OFF)
        print(Ansi.CURSOR_SHOW)
        print(Ansi.ALTERNATE_SCREEN_OFF)
        disableRawMode()
    }

    enableRawMode()
    print(Ansi.ALTERNATE_SCREEN_ON)
    print(Ansi.CURSOR_HIDE)
    print(Ansi.BRACKETED_PASTE_ON)
    driveFrame()

    fun dispatchPaste(text: String): Boolean {
        val focused = focusManager.findFocusedNode(rootNode) ?: return false
        if (focused.onPaste?.invoke(text) == true) return true
        var current: TuiNode? = focused.parent
        while (current != null) {
            if (current.onPaste?.invoke(text) == true) return true
            current = current.parent
        }
        return false
    }

    fun dispatchKey(event: KeyEvent): Boolean {
        if (event.key == Key.TAB) {
            focusManager.focusNext(rootNode)
            return true
        }

        val focusedNode = focusManager.findFocusedNode(rootNode)
        if (focusedNode?.onKeyEvent?.invoke(event) == true) return true
        if (focusedNode != null) {
            var current = focusedNode.parent
            while (current != null) {
                if (current.onKeyEvent?.invoke(event) == true) return true
                current = current.parent
            }
        }

        keyEventState.value = event
        return false
    }

    onInputEvent { event ->
        if (!running) { cleanup(); return@onInputEvent false }

        when (event) {
            is KeyEvent -> dispatchKey(event)
            is PasteEvent -> dispatchPaste(event.text)
        }
        driveFrame()
        if (!running) { cleanup(); return@onInputEvent false }
        true
    }

    cleanup()
}

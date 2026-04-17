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
import dev.usbharu.kotui.utils.Kitty
import dev.usbharu.kotui.utils.SixelSupport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

private const val ACTIVE_FRAME_INTERVAL_MS = 16L
private const val IDLE_FRAME_INTERVAL_MS = 50L

fun runTui(
    screenWidth: Int = 80,
    screenHeight: Int = 24,
    clipboard: Clipboard = SystemClipboard(),
    content: @Composable () -> Unit,
) {
    val frameClock = BroadcastFrameClock()
    val focusManager = FocusManager()
    val renderer = TuiRenderer(screenWidth, screenHeight)
    val rootNode = TuiNode("Root").apply {
        layoutPolicy = dev.usbharu.kotui.compose.node.LayoutPolicy.BOX
    }
    val keyEventState = mutableStateOf<KeyEvent?>(null)
    val inputChannel = Channel<InputEvent>(Channel.UNLIMITED)

    var running = true
    val quit: () -> Unit = { running = false }

    var cleanedUp = false
    fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        inputChannel.close()
        if (SixelSupport.cached?.kittySupported == true) {
            print(Kitty.DELETE_ALL)
        }
        print(Ansi.BRACKETED_PASTE_OFF)
        print(Ansi.CURSOR_SHOW)
        print(Ansi.ALTERNATE_SCREEN_OFF)
        disableRawMode()
    }

    enableRawMode()
    // Probe terminal capabilities once before the main input loop starts so DA1
    // / CSI 16 t responses are not consumed as key events. If the caller has
    // already primed the cache (e.g. tests), the previous value is kept.
    if (SixelSupport.cached == null) {
        SixelSupport.detect()
    }
    print(Ansi.ALTERNATE_SCREEN_ON)
    print(Ansi.CURSOR_HIDE)
    print(Ansi.BRACKETED_PASTE_ON)

    try {
    runMainLoop {
        // Use an independent Job so cancelling our scope does NOT cascade up to
        // the `runBlocking` coroutine (which would throw JobCancellationException
        // out of `runMainLoop`).
        val scopeJob = Job()
        val ctx = this.coroutineContext + frameClock + scopeJob
        val scope = CoroutineScope(ctx)

        val recomposer = Recomposer(ctx)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                recomposer.runRecomposeAndApplyChanges()
            } catch (_: CancellationException) {
                // Expected on shutdown.
            }
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

        fun renderNow() {
            LayoutEngine.layout(rootNode, screenWidth, screenHeight)
            focusManager.autoFocus(rootNode)
            renderer.render(rootNode, focusManager)
        }

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

        fun dispatchEvent(event: InputEvent) {
            when (event) {
                is KeyEvent -> dispatchKey(event)
                is PasteEvent -> dispatchPaste(event.text)
            }
        }

        // Pump terminal input on a background dispatcher so the main render
        // loop can also be woken by timers (e.g. animations) without waiting
        // for a keypress.
        val inputJob = scope.launch(Dispatchers.Default) {
            onInputEvent { event ->
                if (!running) return@onInputEvent false
                inputChannel.trySend(event)
                running
            }
        }

        // Initial render.
        Snapshot.sendApplyNotifications()
        frameClock.sendFrame(frameTimeNanos())
        yield()
        renderNow()

        while (running) {
            // Deliver any state changes made by LaunchedEffect / background
            // coroutines since the last tick so the Recomposer can schedule a
            // frame await (which flips `hasAwaiters` below).
            Snapshot.sendApplyNotifications()
            yield()

            val awaiters = frameClock.hasAwaiters
            val timeoutMs = if (awaiters) ACTIVE_FRAME_INTERVAL_MS else IDLE_FRAME_INTERVAL_MS
            val event = withTimeoutOrNull(timeoutMs) { inputChannel.receive() }
            if (event != null) dispatchEvent(event)
            if (!running) break

            // Pick up any state writes from the wait window, then drive a frame.
            Snapshot.sendApplyNotifications()
            val dirty = event != null || frameClock.hasAwaiters
            if (dirty) {
                frameClock.sendFrame(frameTimeNanos())
                yield()
                renderNow()
            }
        }

        try {
            composition.dispose()
        } catch (_: Throwable) {
        }
        try {
            recomposer.cancel()
        } catch (_: Throwable) {
        }
        inputJob.cancel()
        scopeJob.cancel()
    }
    } finally {
        cleanup()
    }
    // The background worker running `onInputEvent` blocks in a native read()
    // that cannot be interrupted via coroutine cancellation. Force process
    // termination so the TUI actually exits when the user quits.
    forceExit(0)
}

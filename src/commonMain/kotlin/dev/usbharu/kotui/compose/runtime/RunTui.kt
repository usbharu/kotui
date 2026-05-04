package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.snapshots.Snapshot
import dev.usbharu.kotui.TerminalResizeWatcher
import dev.usbharu.kotui.TerminalSize
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
import dev.usbharu.kotui.terminalSize
import dev.usbharu.kotui.watchTerminalResize
import dev.usbharu.kotui.utils.Ansi
import dev.usbharu.kotui.utils.Kitty
import dev.usbharu.kotui.utils.SixelSupport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

private const val ACTIVE_FRAME_INTERVAL_MS = 16L
private const val IDLE_FRAME_INTERVAL_MS = 50L

/**
 * @param exitOnQuit When true (default) the process is terminated via [forceExit] once
 *   the TUI loop ends. This is required for standalone TUI binaries because the platform
 *   input pump blocks in a native `read()` that cannot be coroutine-cancelled, so the
 *   hosting process would otherwise hang waiting for the next keypress. Set to false to
 *   embed kotui inside a larger application; the caller accepts that a background input
 *   thread may remain alive until the next stdin byte arrives (at which point it exits).
 */
fun runTui(
    screenWidth: Int = 80,
    screenHeight: Int = 24,
    fullscreen: Boolean = false,
    clipboard: Clipboard = SystemClipboard(),
    exitOnQuit: Boolean = true,
    content: @Composable () -> Unit,
) {
    val frameClock = BroadcastFrameClock()
    val focusManager = FocusManager()

    var currentWidth = screenWidth
    var currentHeight = screenHeight
    if (fullscreen) {
        terminalSize()?.let {
            currentWidth = it.cols
            currentHeight = it.rows
        }
    }
    val renderer = TuiRenderer(currentWidth, currentHeight)
    val rootNode = TuiNode("Root").apply {
        layoutPolicy = dev.usbharu.kotui.compose.node.LayoutPolicy.BOX
    }
    // Use neverEqualPolicy so every dispatch invalidates observers, even when
    // the next key has identical fields. Default (structural) policy would
    // suppress the second press of 'j' because KeyEvent is a data class and
    // back-to-back equal events would be treated as "no change".
    val keyEventState = mutableStateOf<KeyEvent?>(null, policy = neverEqualPolicy())
    // Bounded buffer so a stuck render loop cannot allow unbounded input growth
    // (bracketed paste or held-down keys can produce events faster than we render).
    // DROP_OLDEST favours responsiveness over completeness for key floods.
    val inputChannel = Channel<InputEvent>(capacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resizeChannel = Channel<TerminalSize>(Channel.CONFLATED)

    var running = true
    val quit: () -> Unit = { running = false }

    var resizeWatcher: TerminalResizeWatcher? = null
    var cleanedUp = false
    fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        resizeWatcher?.close()
        inputChannel.close()
        resizeChannel.close()
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
    if (fullscreen) {
        print(Ansi.CLEAR_SCREEN)
        print(Ansi.CURSOR_HOME)
    }

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
            LayoutEngine.layout(rootNode, currentWidth, currentHeight)
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
                keyEventState.value = null
                return true
            }

            val focusedNode = focusManager.findFocusedNode(rootNode)
            if (focusedNode?.onKeyEvent?.invoke(event) == true) {
                keyEventState.value = null
                return true
            }
            if (focusedNode != null) {
                var current = focusedNode.parent
                while (current != null) {
                    if (current.onKeyEvent?.invoke(event) == true) {
                        keyEventState.value = null
                        return true
                    }
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

        // Watch for terminal resize. Platform actuals pick the best mechanism
        // (SIGWINCH on Unix / `resize` event on Node / polling on Windows). We
        // watch in every mode so non-fullscreen layouts can also react; the
        // fullscreen-only behavior (full CLEAR_SCREEN) is suppressed below.
        resizeWatcher = watchTerminalResize { size ->
            resizeChannel.trySend(size)
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
            var event: InputEvent? = null
            var resized = false
            withTimeoutOrNull(timeoutMs) {
                select<Unit> {
                    inputChannel.onReceive { event = it }
                    resizeChannel.onReceive { size ->
                        currentWidth = size.cols
                        currentHeight = size.rows
                        renderer.resize(currentWidth, currentHeight)
                        resized = true
                    }
                }
            }
            event?.let { dispatchEvent(it) }
            if (!running) break

            // Pick up any state writes from the wait window, then drive a frame.
            Snapshot.sendApplyNotifications()
            val dirty = event != null || resized || frameClock.hasAwaiters
            if (dirty) {
                if (resized && fullscreen) print(Ansi.CLEAR_SCREEN)
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
    if (exitOnQuit) {
        // The background worker running `onInputEvent` blocks in a native read()
        // that cannot be interrupted via coroutine cancellation. Force process
        // termination so the TUI actually exits when the user quits.
        forceExit(0)
    }
    // exitOnQuit=false: return normally. The input-pump thread remains blocked
    // in read() until the next byte arrives; callers must tolerate this.
}

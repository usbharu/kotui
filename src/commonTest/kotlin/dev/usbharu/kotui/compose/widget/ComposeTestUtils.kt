package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.clipboard.InMemoryClipboard
import dev.usbharu.kotui.compose.clipboard.LocalClipboard
import dev.usbharu.kotui.compose.focus.FocusManager
import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.LocalFocusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class ComposeTestSession(
    val root: TuiNode,
    val composition: Composition,
    val recomposer: Recomposer,
    val frameClock: BroadcastFrameClock,
    val focusManager: FocusManager,
    private val job: kotlinx.coroutines.Job,
) {
    fun dispose() {
        composition.dispose()
        recomposer.close()
        job.cancel()
    }
}

fun CoroutineScope.composeWithDefaults(content: @Composable () -> Unit): ComposeTestSession {
    val root = TuiNode("Root")
    val frameClock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + frameClock)
    val focusManager = FocusManager()
    val job = launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
    val composition = Composition(TuiApplier(root), recomposer)

    composition.setContent {
        CompositionLocalProvider(
            LocalFocusManager provides focusManager,
            LocalClipboard provides InMemoryClipboard(),
        ) {
            content()
        }
    }

    return ComposeTestSession(root, composition, recomposer, frameClock, focusManager, job)
}

suspend fun ComposeTestSession.awaitIdle() {
    recomposer.awaitIdle()
}

suspend fun ComposeTestSession.applySnapshotAndAwaitIdle() {
    Snapshot.sendApplyNotifications()
    yield()
    frameClock.sendFrame(0L)
    yield()
    awaitIdle()
}

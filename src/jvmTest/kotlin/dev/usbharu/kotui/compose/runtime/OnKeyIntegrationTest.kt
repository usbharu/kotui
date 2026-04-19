package dev.usbharu.kotui.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.node.TuiNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reproduces the RunTui main-loop driving pattern so we can exercise onKey +
 * LocalKeyEvent without needing a real TTY. Verifies that repeated equal
 * key events correctly invalidate composition and fire handlers.
 */
class OnKeyIntegrationTest {

    private class Harness {
        val frameClock = BroadcastFrameClock()
        val keyEventState = mutableStateOf<KeyEvent?>(null, policy = neverEqualPolicy())
        val rootNode = TuiNode("Root")
        val job = Job()
        lateinit var scope: CoroutineScope
        lateinit var recomposer: Recomposer
        lateinit var composition: Composition

        fun start(parent: CoroutineScope, content: @Composable () -> Unit) {
            scope = CoroutineScope(parent.coroutineContext + frameClock + job)
            recomposer = Recomposer(scope.coroutineContext)
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try { recomposer.runRecomposeAndApplyChanges() } catch (_: Throwable) {}
            }
            composition = Composition(TuiApplier(rootNode), recomposer)
            composition.setContent {
                CompositionLocalProvider(LocalKeyEvent provides keyEventState.value) {
                    content()
                }
            }
        }

        suspend fun tick(rounds: Int = 1) {
            repeat(rounds) {
                Snapshot.sendApplyNotifications()
                delay(5)
                frameClock.sendFrame(System.nanoTime())
                delay(10)
            }
            yield()
        }

        suspend fun dispatch(event: KeyEvent?) {
            keyEventState.value = event
        }

        fun dispose() {
            try { composition.dispose() } catch (_: Throwable) {}
            recomposer.cancel()
            job.cancel()
        }
    }

    @Test
    fun twoEqualButDistinctEventsBothFire() = runBlocking(Dispatchers.Default) {
        val h = Harness()
        val fireLog = mutableListOf<KeyEvent>()
        try {
            h.start(this) {
                onKey { fireLog.add(it) }
            }
            h.tick()
            assertEquals(0, fireLog.size)

            // Two separate instances of the same 'j' press.
            val j1 = KeyEvent('j', Key.CHAR)
            val j2 = KeyEvent('j', Key.CHAR)
            val j3 = KeyEvent('j', Key.CHAR)

            h.dispatch(j1)
            h.tick(rounds = 4)
            assertEquals(1, fireLog.size, "first j should fire")

            h.dispatch(j2)
            h.tick(rounds = 4)
            assertEquals(2, fireLog.size, "second j (equal but distinct) should fire")

            h.dispatch(j3)
            h.tick(rounds = 4)
            assertEquals(3, fireLog.size, "third j should fire")
        } finally {
            h.dispose()
        }
    }

    @Test
    fun selectedReachesCapAfterTwoPressesWithSingleFramePerDispatch() = runBlocking(Dispatchers.Default) {
        // Mirrors RunTui's main loop: 1 frame per dispatch, then subsequent
        // idle polls at 16ms that fire additional frames to settle cascades.
        val h = Harness()
        var observed = -1
        try {
            h.start(this) {
                var s by remember { mutableStateOf(0) }
                onKey { ev ->
                    when (ev.char) {
                        'j' -> s = (s + 1).coerceAtMost(2)
                        'k' -> s = (s - 1).coerceAtLeast(0)
                    }
                }
                observed = s
            }
            suspend fun pressAndSettle(ch: Char) {
                h.dispatch(KeyEvent(ch, Key.CHAR))
                // 1 frame for the dispatch, then up to 3 idle frames at 16ms
                // to let cascading recompositions finish (matches RunTui's
                // hasAwaiters-driven polling).
                h.tick(rounds = 1)
                repeat(3) { delay(16); h.tick(rounds = 1) }
            }
            h.tick(rounds = 2)
            assertEquals(0, observed)

            pressAndSettle('j')
            assertEquals(1, observed)

            pressAndSettle('j')
            assertEquals(2, observed, "second j must reach step 3")

            pressAndSettle('j')
            assertEquals(2, observed)

            pressAndSettle('k')
            assertEquals(1, observed)
        } finally {
            h.dispose()
        }
    }

    @Test
    fun selectedReachesCapAfterTwoPresses() = runBlocking(Dispatchers.Default) {
        val h = Harness()
        var observed = -1
        try {
            h.start(this) {
                var s by remember { mutableStateOf(0) }
                onKey { ev ->
                    when (ev.char) {
                        'j' -> s = (s + 1).coerceAtMost(2)
                        'k' -> s = (s - 1).coerceAtLeast(0)
                    }
                }
                observed = s
            }
            h.tick(rounds = 4)
            assertEquals(0, observed)

            h.dispatch(KeyEvent('j', Key.CHAR))
            h.tick(rounds = 4)
            assertEquals(1, observed)

            h.dispatch(KeyEvent('j', Key.CHAR))
            h.tick(rounds = 4)
            assertEquals(2, observed, "second j must advance to step 3 (index 2)")

            h.dispatch(KeyEvent('j', Key.CHAR))
            h.tick(rounds = 4)
            assertEquals(2, observed, "third j stays capped")

            h.dispatch(KeyEvent('k', Key.CHAR))
            h.tick(rounds = 4)
            assertEquals(1, observed)
        } finally {
            h.dispose()
        }
    }
}

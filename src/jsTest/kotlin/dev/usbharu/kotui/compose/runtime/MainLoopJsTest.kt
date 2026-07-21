package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import dev.usbharu.kotui.onInputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MainLoopJsTest {
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun jsMainLoopReportsAsyncAndCompletesThroughCallback() = GlobalScope.promise {
        val completion = CompletableDeferred<Throwable?>()
        var ran = false

        val synchronous = runMainLoop(
            block = { ran = true },
            onComplete = { completion.complete(it) },
        )

        assertFalse(synchronous)
        assertNull(completion.await())
        assertTrue(ran)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun cancellingJsInputLoopRemovesOnlyItsOwnDataListener() = GlobalScope.promise {
        val stdin = js("process.stdin")
        val before = stdin.listenerCount("data") as Int
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            onInputEvent { true }
        }

        assertEquals(before + 1, stdin.listenerCount("data") as Int)
        job.cancelAndJoin()
        assertEquals(before, stdin.listenerCount("data") as Int)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun inputPumpReportsNormalCompletionAndReaderFailure() = GlobalScope.promise {
        val normal = runInputPump(read = { }, isRunning = { true }, emit = {})
        assertTrue(normal.isSuccess)

        val expected = IllegalStateException("read failed")
        val failed = runInputPump(
            read = { throw expected },
            isRunning = { true },
            emit = {},
        )
        assertSame(expected, failed.exceptionOrNull())
    }
}

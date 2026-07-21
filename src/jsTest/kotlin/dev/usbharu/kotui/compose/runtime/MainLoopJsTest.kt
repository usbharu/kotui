package dev.usbharu.kotui.compose.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
}

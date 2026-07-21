package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import dev.usbharu.kotui.compose.applier.TuiApplier
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.gap
import dev.usbharu.kotui.compose.modifier.width
import dev.usbharu.kotui.compose.modifier.Modifier.Element
import dev.usbharu.kotui.compose.node.TuiNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModifierReconciliationTest {
    @Test
    fun unchangedModifierStillWinsWhenWidgetBasePropertiesChange() = runBlocking(Dispatchers.Default) {
        val clock = BroadcastFrameClock()
        val job = Job()
        val scope = CoroutineScope(coroutineContext + clock + job)
        val recomposer = Recomposer(scope.coroutineContext)
        val root = TuiNode("root")
        val composition = Composition(TuiApplier(root), recomposer)
        val label = mutableStateOf("a")
        val gap = mutableStateOf(1)
        val image = mutableStateOf(TerminalImage(ByteArray(4), 1, 1))
        val fixedWidth = Modifier.width(7)
        val fixedGap = Modifier.gap(9)
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent {
                Badge(label.value, fixedWidth)
                Row(modifier = fixedGap, gap = gap.value) {}
                Image(image.value, fixedWidth)
            }
            suspend fun settle() {
                Snapshot.sendApplyNotifications()
                delay(10)
                clock.sendFrame(System.nanoTime())
                delay(20)
            }
            settle()
            assertEquals(7, root.children[0].preferredWidth)
            assertEquals(9, root.children[1].layoutGap)
            assertEquals(7, root.children[2].preferredWidth)

            label.value = "a much wider badge"
            gap.value = 2
            image.value = TerminalImage(ByteArray(8), 2, 1, cellPixelWidth = 1)
            settle()

            assertEquals(7, root.children[0].preferredWidth)
            assertEquals(9, root.children[1].layoutGap)
            assertEquals(7, root.children[2].preferredWidth)
        } finally {
            composition.dispose()
            recomposer.cancel()
            job.cancel()
        }
    }

    @Test
    fun removingModifierDoesNotRestoreOldBaseWhenNewBaseEqualsOldOverride() = runBlocking(Dispatchers.Default) {
        val clock = BroadcastFrameClock()
        val job = Job()
        val scope = CoroutineScope(coroutineContext + clock + job)
        val recomposer = Recomposer(scope.coroutineContext)
        val root = TuiNode("root")
        val composition = Composition(TuiApplier(root), recomposer)
        val label = mutableStateOf("a") // rendered width = 3
        val modifier = mutableStateOf<Modifier>(Modifier.width(7))
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent { Badge(label.value, modifier.value) }
            suspend fun settle() {
                Snapshot.sendApplyNotifications()
                delay(10)
                clock.sendFrame(System.nanoTime())
                delay(20)
            }
            settle()
            assertEquals(7, root.children.single().preferredWidth)

            label.value = "abcde" // rendered width now also equals 7
            modifier.value = Modifier
            settle()

            assertEquals(7, root.children.single().preferredWidth)
        } finally {
            composition.dispose()
            recomposer.cancel()
            job.cancel()
        }
    }

    @Test
    fun removingCustomModifierRestoresEveryNodePropertyItChanged() = runBlocking(Dispatchers.Default) {
        val clock = BroadcastFrameClock()
        val job = Job()
        val scope = CoroutineScope(coroutineContext + clock + job)
        val recomposer = Recomposer(scope.coroutineContext)
        val root = TuiNode("root")
        val composition = Composition(TuiApplier(root), recomposer)
        val custom = object : Element {
            override fun apply(node: TuiNode) {
                node.drawBorder = true
                node.text = "from modifier"
            }
        }
        val modifier = mutableStateOf<Modifier>(custom)
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent { Text("base", modifier.value) }
            suspend fun settle() {
                Snapshot.sendApplyNotifications()
                delay(10)
                clock.sendFrame(System.nanoTime())
                delay(20)
            }
            settle()
            assertTrue(root.children.single().drawBorder)
            assertEquals("from modifier", root.children.single().text)

            modifier.value = Modifier
            settle()

            assertFalse(root.children.single().drawBorder)
            assertEquals("base", root.children.single().text)
        } finally {
            composition.dispose()
            recomposer.cancel()
            job.cancel()
        }
    }
}

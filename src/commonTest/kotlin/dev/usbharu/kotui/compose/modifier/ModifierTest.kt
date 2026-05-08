package dev.usbharu.kotui.compose.modifier

import dev.usbharu.kotui.compose.node.TuiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ModifierTest {
    @Test
    fun thenWithEmptyModifierReturnsSameInstance() {
        val modifier = Modifier.width(3)

        assertSame(modifier, modifier.then(Modifier))
    }

    @Test
    fun emptyModifierThenReturnsOtherModifier() {
        val modifier = Modifier.width(5)

        assertSame(modifier, Modifier.then(modifier))
    }

    @Test
    fun combinedModifierAppliesInOrder() {
        val node = TuiNode("node")

        node.applyModifier(Modifier.width(2).width(4).height(1))

        assertEquals(4, node.preferredWidth)
        assertEquals(1, node.preferredHeight)
    }

    @Test
    fun equivalentModifierChainsAreEqual() {
        assertEquals(
            Modifier.width(10).height(2).focusable(),
            Modifier.width(10).height(2).focusable(),
        )
    }
}

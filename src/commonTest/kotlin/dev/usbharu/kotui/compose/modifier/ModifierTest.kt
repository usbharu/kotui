package dev.usbharu.kotui.compose.modifier

import dev.usbharu.kotui.compose.node.TuiNode
import dev.usbharu.kotui.compose.runtime.KeyEvent
import dev.usbharu.kotui.core.Style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertNull

class ModifierTest {
    @Test
    fun companionIsIdentityForThenAndFolds() {
        val modifier = Modifier.width(4)

        assertSame(modifier, modifier.then(Modifier))
        assertSame(modifier, Modifier.then(modifier))
        assertEquals(0, Modifier.foldIn(0) { acc, _ -> acc + 1 })
        assertEquals(0, Modifier.foldOut(0) { _, acc -> acc + 1 })
        assertEquals("Modifier", Modifier.toString())
    }

    @Test
    fun foldInAndFoldOutPreserveExpectedOrder() {
        val modifier = Modifier.width(1).height(2).gap(3)

        val inOrder = modifier.foldIn(emptyList<String>()) { acc, element ->
            acc + element::class.simpleName.orEmpty()
        }
        val outOrder = modifier.foldOut(emptyList<String>()) { element, acc ->
            acc + element::class.simpleName.orEmpty()
        }

        assertEquals(listOf("WidthMod", "HeightMod", "GapMod"), inOrder)
        assertEquals(listOf("GapMod", "HeightMod", "WidthMod"), outOrder)
    }

    @Test
    fun applyingModifierUpdatesNodeProperties() {
        val node = TuiNode("target")
        val style = Style(bold = true)
        val focused = Style(underline = true)
        var handled = false

        node.applyModifier(
            Modifier
                .size(12, 3)
                .style(style)
                .focusedStyle(focused)
                .focusable()
                .focusScope()
                .zIndex(9)
                .gap(2)
                .weight(1.5f)
                .flexBasis(7)
                .offset(4, 5)
                .onKeyEvent {
                    handled = it.char == 'x'
                    handled
                }
        )

        assertEquals(12, node.preferredWidth)
        assertEquals(3, node.preferredHeight)
        assertEquals(style, node.style)
        assertEquals(focused, node.focusedStyle)
        assertTrue(node.focusable)
        assertTrue(node.focusScope)
        assertEquals(9, node.zIndex)
        assertEquals(2, node.layoutGap)
        assertEquals(1.5f, node.flexGrow)
        assertEquals(7, node.flexBasis)
        assertEquals(4, node.bounds.x)
        assertEquals(5, node.bounds.y)
        assertNotNull(node.onKeyEvent)
        assertTrue(node.onKeyEvent!!.invoke(KeyEvent.fromChar('x')))
        assertTrue(handled)
        assertFalse(node.onKeyEvent!!.invoke(KeyEvent.fromChar('y')))
    }

    @Test
    fun dimensionsAndGapsRejectNegativeValues() {
        assertFailsWith<IllegalArgumentException> { Modifier.width(-1) }
        assertFailsWith<IllegalArgumentException> { Modifier.height(-1) }
        assertFailsWith<IllegalArgumentException> { Modifier.gap(-1) }
        assertFailsWith<IllegalArgumentException> { Modifier.flexBasis(-1) }
    }

    @Test
    fun flexGrowRejectsNegativeAndNonFiniteValues() {
        assertFailsWith<IllegalArgumentException> { Modifier.weight(-1f) }
        assertFailsWith<IllegalArgumentException> { Modifier.flexGrow(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { Modifier.flexGrow(Float.POSITIVE_INFINITY) }
    }

    @Test
    fun removingSizeAndFlexModifiersRestoresWidgetValues() {
        val node = TuiNode("node").apply {
            preferredWidth = 4
            preferredHeight = 2
            layoutGap = 1
            flexGrow = 0.5f
            flexBasis = 3
        }
        node.applyModifier(Modifier.size(10, 8).gap(6).weight(2f).flexBasis(9))

        node.applyModifier(Modifier)

        assertEquals(4, node.preferredWidth)
        assertEquals(2, node.preferredHeight)
        assertEquals(1, node.layoutGap)
        assertEquals(0.5f, node.flexGrow)
        assertEquals(3, node.flexBasis)
    }

    @Test
    fun removingVisualAndFocusModifiersRestoresWidgetValues() {
        val baseStyle = Style(fg = "base")
        val node = TuiNode("node").apply { style = baseStyle }
        node.applyModifier(
            Modifier.style(Style(bg = "overlay"))
                .focusedStyle(Style(bold = true))
                .focusable()
                .focusScope()
                .zIndex(7)
        )

        node.applyModifier(Modifier)

        assertEquals(baseStyle, node.style)
        assertNull(node.focusedStyle)
        assertFalse(node.focusable)
        assertFalse(node.focusScope)
        assertEquals(0, node.zIndex)
    }

    @Test
    fun removingKeyAndOffsetModifiersRestoresExistingBehaviorAndPosition() {
        val originalHandler: (KeyEvent) -> Boolean = { true }
        val node = TuiNode("node").apply {
            bounds = dev.usbharu.kotui.core.Rect(2, 3, 4, 5)
            onKeyEvent = originalHandler
        }
        node.applyModifier(Modifier.offset(8, 9).onKeyEvent { false })

        node.applyModifier(Modifier)

        assertEquals(2, node.bounds.x)
        assertEquals(3, node.bounds.y)
        assertEquals(4, node.bounds.width)
        assertSame(originalHandler, node.onKeyEvent)
    }

    @Test
    fun widgetPropertyChangedBetweenModifierPassesIsNotRolledBack() {
        val node = TuiNode("node").apply { preferredWidth = 4 }
        node.applyModifier(Modifier.width(10))
        node.preferredWidth = 7 // a widget update that ran before the modifier update

        node.applyModifier(Modifier)

        assertEquals(7, node.preferredWidth)
    }

    @Test
    fun replacingModifierUsesOriginalWidgetBaseline() {
        val node = TuiNode("node").apply { preferredWidth = 4 }
        node.applyModifier(Modifier.width(10))

        node.applyModifier(Modifier.width(12))
        assertEquals(12, node.preferredWidth)

        node.applyModifier(Modifier)
        assertEquals(4, node.preferredWidth)
    }

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

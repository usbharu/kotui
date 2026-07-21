package dev.usbharu.kotui.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnsiKeyDecoderTest {

    private fun decode(input: String, flushAtEnd: Boolean = true): List<InputEvent> {
        val decoder = AnsiKeyDecoder()
        val out = mutableListOf<InputEvent>()
        for (c in input) out += decoder.feed(c)
        if (flushAtEnd) out += decoder.flush()
        return out
    }

    @Test
    fun plainAscii() {
        val events = decode("abc")
        assertEquals(3, events.size)
        events.forEachIndexed { i, e ->
            val k = e as KeyEvent
            assertEquals(Key.CHAR, k.key)
            assertEquals("abc"[i], k.char)
            assertEquals(false, k.ctrl)
        }
    }

    @Test
    fun tabEnterBackspace() {
        val events = decode("\t\r\u007F")
        assertEquals(3, events.size)
        assertEquals(Key.TAB, (events[0] as KeyEvent).key)
        assertEquals(Key.ENTER, (events[1] as KeyEvent).key)
        assertEquals(Key.BACKSPACE, (events[2] as KeyEvent).key)
    }

    @Test
    fun ctrlLetter() {
        val events = decode("\u0001\u0003\u0016") // Ctrl+A, Ctrl+C, Ctrl+V
        assertEquals(3, events.size)
        val a = events[0] as KeyEvent
        assertEquals('a', a.char); assertTrue(a.ctrl); assertEquals(Key.CHAR, a.key)
        val c = events[1] as KeyEvent
        assertEquals('c', c.char); assertTrue(c.ctrl)
        val v = events[2] as KeyEvent
        assertEquals('v', v.char); assertTrue(v.ctrl)
    }

    @Test
    fun arrowKeys() {
        val events = decode("\u001B[A\u001B[B\u001B[C\u001B[D")
        assertEquals(4, events.size)
        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.ARROW_DOWN, (events[1] as KeyEvent).key)
        assertEquals(Key.ARROW_RIGHT, (events[2] as KeyEvent).key)
        assertEquals(Key.ARROW_LEFT, (events[3] as KeyEvent).key)
    }

    @Test
    fun shiftArrow() {
        val events = decode("\u001B[1;2C")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals(Key.ARROW_RIGHT, k.key)
        assertTrue(k.shift)
        assertEquals(false, k.ctrl)
        assertEquals(false, k.alt)
    }

    @Test
    fun ctrlArrow() {
        val events = decode("\u001B[1;5D")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals(Key.ARROW_LEFT, k.key)
        assertTrue(k.ctrl)
        assertEquals(false, k.shift)
    }

    @Test
    fun homeEndDelete() {
        val events = decode("\u001B[H\u001B[F\u001B[3~")
        assertEquals(3, events.size)
        assertEquals(Key.HOME, (events[0] as KeyEvent).key)
        assertEquals(Key.END, (events[1] as KeyEvent).key)
        assertEquals(Key.DELETE, (events[2] as KeyEvent).key)
    }

    @Test
    fun homeEndTildeForm() {
        val events = decode("\u001B[1~\u001B[4~")
        assertEquals(2, events.size)
        assertEquals(Key.HOME, (events[0] as KeyEvent).key)
        assertEquals(Key.END, (events[1] as KeyEvent).key)
    }

    @Test
    fun pageUpDown() {
        val events = decode("\u001B[5~\u001B[6~")
        assertEquals(Key.PAGE_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.PAGE_DOWN, (events[1] as KeyEvent).key)
    }

    @Test
    fun ss3ArrowKeys() {
        val events = decode("\u001BOA\u001BOH")
        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.HOME, (events[1] as KeyEvent).key)
    }

    @Test
    fun escapeAloneViaFlush() {
        val events = decode("\u001B")
        assertEquals(1, events.size)
        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
    }

    @Test
    fun altCharViaEsc() {
        val events = decode("\u001Bf")
        assertEquals(1, events.size)
        val k = events[0] as KeyEvent
        assertEquals('f', k.char)
        assertTrue(k.alt)
        assertEquals(Key.CHAR, k.key)
    }

    @Test
    fun bracketedPaste() {
        val events = decode("\u001B[200~hello world\u001B[201~")
        assertEquals(1, events.size)
        val p = events[0] as PasteEvent
        assertEquals("hello world", p.text)
    }

    @Test
    fun bracketedPasteAroundText() {
        val events = decode("a\u001B[200~xy\u001B[201~b")
        assertEquals(3, events.size)
        assertEquals('a', (events[0] as KeyEvent).char)
        assertEquals("xy", (events[1] as PasteEvent).text)
        assertEquals('b', (events[2] as KeyEvent).char)
    }

    @Test
    fun incompletePasteFlushPreservesPossibleTerminatorPrefix() {
        val events = decode("\u001B[200~hello\u001B[20")

        assertEquals("hello\u001B[20", (events.single() as PasteEvent).text)
    }

    @Test
    fun altTabAndAltEnterKeepTheirSemanticKeys() {
        val events = decode("\u001B\t\u001B\r")

        val tab = events[0] as KeyEvent
        assertEquals(Key.TAB, tab.key)
        assertTrue(tab.alt)
        val enter = events[1] as KeyEvent
        assertEquals(Key.ENTER, enter.key)
        assertTrue(enter.alt)
    }

    @Test
    fun omittedCsiParametersUseTerminalDefaults() {
        val event = decode("\u001B[;C").single() as KeyEvent

        assertEquals(Key.ARROW_RIGHT, event.key)
        assertEquals(false, event.ctrl)
        assertEquals(false, event.shift)
        assertEquals(false, event.alt)
    }

    @Test
    fun csiBackTabIsDecodedAsShiftTab() {
        val event = decode("\u001B[Z").single() as KeyEvent

        assertEquals(Key.TAB, event.key)
        assertTrue(event.shift)
    }

    @Test
    fun incompleteCsiFlushPreservesEveryTypedByte() {
        val events = decode("\u001B[12;")

        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
        assertEquals("[12;", events.drop(1).joinToString("") { (it as KeyEvent).char.toString() })
    }

    @Test
    fun incompleteSs3FlushPreservesIntroducer() {
        val events = decode("\u001BO")

        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
        assertEquals('O', (events[1] as KeyEvent).char)
    }

    @Test
    fun csiParameterAndIntermediateBytesDoNotLeakAsText() {
        val events = decode("\u001B[>1;2mX\u001B[1 qY")

        assertEquals(listOf('X', 'Y'), events.map { (it as KeyEvent).char })
    }

    @Test
    fun colonSubparametersDoNotBreakModifierDecoding() {
        val event = decode("\u001B[1;5:1C").single() as KeyEvent

        assertEquals(Key.ARROW_RIGHT, event.key)
        assertTrue(event.ctrl)
    }

    @Test
    fun overlappingPasteTerminatorCandidateStillFindsRealTerminator() {
        val events = decode("\u001B[200~a\u001B\u001B[201~")

        assertEquals("a\u001B", (events.single() as PasteEvent).text)
    }

    @Test
    fun ctrlSpaceAndAsciiSeparatorControlsAreMappedToShortcutCharacters() {
        val events = decode("\u0000\u001C\u001D\u001E\u001F")

        assertEquals(listOf(' ', '\\', ']', '^', '_'), events.map { (it as KeyEvent).char })
        assertTrue(events.all { (it as KeyEvent).ctrl })
    }

    @Test
    fun altControlShortcutsRetainBothModifiers() {
        val events = decode("\u001B\u0000\u001B\u001C")

        assertEquals(listOf(' ', '\\'), events.map { (it as KeyEvent).char })
        assertTrue(events.all { (it as KeyEvent).ctrl && it.alt })
    }

    @Test
    fun invalidControlByteCancelsCsiWithoutLosingInput() {
        val events = decode("\u001B[12\nA")

        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
        assertEquals("[12", events.subList(1, 4).joinToString("") { (it as KeyEvent).char.toString() })
        assertEquals(Key.ENTER, (events[4] as KeyEvent).key)
        assertEquals('A', (events[5] as KeyEvent).char)
    }

    @Test
    fun oversizedCsiCannotGrowDecoderBufferWithoutBound() {
        val decoder = AnsiKeyDecoder()
        val output = mutableListOf<InputEvent>()
        "\u001B[${"1".repeat(65)}".forEach { output += decoder.feed(it) }

        assertTrue(output.isNotEmpty())
        assertEquals(false, decoder.hasPending())
    }

    @Test
    fun overflowingCsiNumberIsReportedAsUnknownInsteadOfDefaultArrow() {
        val event = decode("\u001B[999999999999999999C").single() as KeyEvent

        assertEquals(Key.UNKNOWN, event.key)
    }

    @Test
    fun unsupportedModifierValueIsReportedAsUnknown() {
        val event = decode("\u001B[1;99C").single() as KeyEvent

        assertEquals(Key.UNKNOWN, event.key)
    }

    @Test
    fun unsupportedButCompleteSequencesProduceUnknownEvent() {
        assertEquals(Key.UNKNOWN, (decode("\u001B[2~").single() as KeyEvent).key)
        assertEquals(Key.UNKNOWN, (decode("\u001BOX").first() as KeyEvent).key)
    }

    @Test
    fun eightBitCsiAndSs3IntroducersAreDecoded() {
        val events = decode("\u009BA\u008FB")

        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.ARROW_DOWN, (events[1] as KeyEvent).key)
    }

    @Test
    fun crlfEnterIsSingleEnter() {
        val events = decode("\r\n")
        assertEquals(1, events.size)
        assertEquals(Key.ENTER, (events[0] as KeyEvent).key)
    }

    @Test
    fun pendingStatesAreVisibleUntilSequenceIsCompletedOrFlushed() {
        val decoder = AnsiKeyDecoder()
        assertTrue(decoder.feed('\u001B').isEmpty())
        assertTrue(decoder.hasPending())

        val events = decoder.flush()
        assertEquals(1, events.size)
        assertEquals(Key.ESCAPE, (events[0] as KeyEvent).key)
        assertEquals(false, decoder.hasPending())
    }

    @Test
    fun incompleteCsiAndSs3FlushAsEscape() {
        val csi = decode("\u001B[1;", flushAtEnd = true)
        assertEquals(4, csi.size)
        assertEquals(Key.ESCAPE, (csi[0] as KeyEvent).key)
        assertEquals(listOf('[', '1', ';'), csi.drop(1).map { (it as KeyEvent).char })

        val ss3 = decode("\u001BO", flushAtEnd = true)
        assertEquals(2, ss3.size)
        assertEquals(Key.ESCAPE, (ss3[0] as KeyEvent).key)
        assertEquals('O', (ss3[1] as KeyEvent).char)
    }

    @Test
    fun escapeEscapeEmitsFirstEscapeAndKeepsSecondPending() {
        val decoder = AnsiKeyDecoder()
        assertTrue(decoder.feed('\u001B').isEmpty())
        val first = decoder.feed('\u001B')
        assertEquals(1, first.size)
        assertEquals(Key.ESCAPE, (first[0] as KeyEvent).key)

        val second = decoder.flush()
        assertEquals(1, second.size)
        assertEquals(Key.ESCAPE, (second[0] as KeyEvent).key)
    }

    @Test
    fun ctrlAltLetterAndAltBackspace() {
        val events = decode("\u001B\u0001\u001B\u007F")

        val ctrlAlt = events[0] as KeyEvent
        assertEquals('a', ctrlAlt.char)
        assertTrue(ctrlAlt.ctrl)
        assertTrue(ctrlAlt.alt)

        val altBackspace = events[1] as KeyEvent
        assertEquals(Key.BACKSPACE, altBackspace.key)
        assertTrue(altBackspace.alt)
    }

    @Test
    fun csiModifiersCanSetCtrlAltAndShiftTogether() {
        val events = decode("\u001B[1;8C")

        val key = events[0] as KeyEvent
        assertEquals(Key.ARROW_RIGHT, key.key)
        assertTrue(key.ctrl)
        assertTrue(key.alt)
        assertTrue(key.shift)
    }

    @Test
    fun unknownCsiAndSs3SequencesProduceUnknownEvents() {
        assertEquals(3, decode("\u001B[2~\u001B[99~\u001B[X").count { (it as KeyEvent).key == Key.UNKNOWN })
        assertEquals(Key.UNKNOWN, (decode("\u001BOX").single() as KeyEvent).key)
    }

    @Test
    fun privateCsiQuestionPrefixIsIgnoredWhenParsingParams() {
        val events = decode("\u001B[?1;5D")

        val key = events[0] as KeyEvent
        assertEquals(Key.ARROW_LEFT, key.key)
        assertTrue(key.ctrl)
    }

    @Test
    fun strayPasteEndMarkerProducesNoEvent() {
        assertTrue(decode("\u001B[201~").isEmpty())
    }

    @Test
    fun incompletePasteFlushesCollectedText() {
        val events = decode("\u001B[200~partial")

        assertEquals(1, events.size)
        assertEquals("partial", (events[0] as PasteEvent).text)
    }

    @Test
    fun pasteTerminatorMismatchIsTreatedAsLiteralPasteText() {
        val events = decode("\u001B[200~a\u001B[20Xb\u001B[201~")

        assertEquals(1, events.size)
        assertEquals("a\u001B[20Xb", (events[0] as PasteEvent).text)
    }

    @Test
    fun altControlCharsBackspaceAndRegularUppercaseAreDecoded() {
        val events = decode("\u001B\n\u001B\t\u001BZ")

        val altEnter = events[0] as KeyEvent
        assertEquals(Key.ENTER, altEnter.key)
        assertTrue(altEnter.alt)

        val altTab = events[1] as KeyEvent
        assertEquals(Key.TAB, altTab.key)
        assertTrue(altTab.alt)

        val altUpper = events[2] as KeyEvent
        assertEquals(Key.CHAR, altUpper.key)
        assertEquals('Z', altUpper.char)
        assertTrue(altUpper.alt)
    }

    @Test
    fun csiDefaultsAndAlternateHomeEndFormsAreDecoded() {
        val events = decode("\u001B[A\u001B[7~\u001B[8~")

        assertEquals(Key.ARROW_UP, (events[0] as KeyEvent).key)
        assertEquals(Key.HOME, (events[1] as KeyEvent).key)
        assertEquals(Key.END, (events[2] as KeyEvent).key)
    }

    @Test
    fun unknownCsiFinalProducesUnknownEvent() {
        val unknownFinal = decode("\u001B[X")
        assertEquals(Key.UNKNOWN, (unknownFinal.single() as KeyEvent).key)
    }

    @Test
    fun emptyCsiModifierParamDoesNotEnableAllModifiers() {
        val events = decode("\u001B[1;C")

        val key = events.single() as KeyEvent
        assertEquals(Key.ARROW_RIGHT, key.key)
        assertEquals(false, key.ctrl)
        assertEquals(false, key.alt)
        assertEquals(false, key.shift)
    }

    @Test
    fun pasteEscPrefixOnlyIsFlushedAsLiteralContent() {
        val events = decode("\u001B[200~a\u001Bx\u001B[201~")

        assertEquals(1, events.size)
        assertEquals("a\u001Bx", (events.single() as PasteEvent).text)
    }

    @Test
    fun flushIdleProducesNoEvents() {
        val decoder = AnsiKeyDecoder()

        assertTrue(decoder.flush().isEmpty())
        assertEquals(false, decoder.hasPending())
    }

    @Test
    fun emptyIncompletePasteFlushesNoEvent() {
        val decoder = AnsiKeyDecoder()
        "\u001B[200~".forEach { decoder.feed(it) }

        assertTrue(decoder.hasPending())
        assertTrue(decoder.flush().isEmpty())
    }

    @Test
    fun ss3CoversRightLeftAndEnd() {
        val events = decode("\u001BOC\u001BOD\u001BOF")

        assertEquals(Key.ARROW_RIGHT, (events[0] as KeyEvent).key)
        assertEquals(Key.ARROW_LEFT, (events[1] as KeyEvent).key)
        assertEquals(Key.END, (events[2] as KeyEvent).key)
    }

}

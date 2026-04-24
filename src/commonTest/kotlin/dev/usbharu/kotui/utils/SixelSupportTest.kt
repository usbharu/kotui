package dev.usbharu.kotui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SixelSupportTest {
    private fun env(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.toMap()
        return { map[it] }
    }

    @Test
    fun ghosttyDetectedAsKittyEvenThoughDA1ReportsNoSixel() {
        val probe = parseTerminalResponses("\u001B[?62;22;52c\u001B[6;29;14t")
        assertFalse(probe.sixelSupported, "sanity: Ghostty DA1 does not advertise sixel")
        assertEquals(14, probe.cellPixelWidth)
        assertEquals(29, probe.cellPixelHeight)

        val envCaps = detectCapsFromEnv(env(
            "TERM" to "xterm-ghostty",
            "TERM_PROGRAM" to "ghostty",
            "GHOSTTY_RESOURCES_DIR" to "/Applications/Ghostty.app/...",
        ))
        assertTrue(envCaps.kitty, "Ghostty env implies kitty support")
        assertFalse(envCaps.sixel, "Ghostty does not actually support sixel")

        val merged = mergeCaps(probe, envCaps)
        assertTrue(merged.kittySupported)
        assertFalse(merged.sixelSupported)
        assertEquals(14, merged.cellPixelWidth)
        assertEquals(29, merged.cellPixelHeight)
    }

    @Test
    fun kittyTerminalDetectedViaTerm() {
        val caps = detectCapsFromEnv(env("TERM" to "xterm-kitty"))
        assertTrue(caps.kitty)
        assertFalse(caps.sixel)
    }

    @Test
    fun wezTermSupportsBothProtocols() {
        val caps = detectCapsFromEnv(env("TERM_PROGRAM" to "WezTerm"))
        assertTrue(caps.kitty)
        assertTrue(caps.sixel)
    }

    @Test
    fun footHintSixelOnly() {
        val caps = detectCapsFromEnv(env("TERM" to "foot-extra"))
        assertTrue(caps.sixel)
        assertFalse(caps.kitty)
    }

    @Test
    fun plainXtermNotFlaggedByEnv() {
        val caps = detectCapsFromEnv(env("TERM" to "xterm-256color"))
        assertFalse(caps.sixel)
        assertFalse(caps.kitty)
    }

    @Test
    fun xtermWithSixelStillDetectedByDA1() {
        val caps = parseTerminalResponses("\u001B[?64;1;2;4;6;9;22c")
        assertTrue(caps.sixelSupported, "feature 4 present => sixel")
    }

    @Test
    fun mergeUsesProbeCellSizeAndEnvKitty() {
        val probe = TerminalCaps(sixelSupported = false, cellPixelWidth = 14, cellPixelHeight = 29)
        val merged = mergeCaps(probe, EnvCaps(sixel = false, kitty = true))
        assertTrue(merged.kittySupported)
        assertFalse(merged.sixelSupported)
        assertEquals(14, merged.cellPixelWidth)
        assertEquals(29, merged.cellPixelHeight)
    }

    @Test
    fun mergeWithNullProbeFallsBackToDefaults() {
        val merged = mergeCaps(probe = null, env = EnvCaps(sixel = false, kitty = true))
        assertTrue(merged.kittySupported)
        assertEquals(10, merged.cellPixelWidth)
        assertEquals(20, merged.cellPixelHeight)
    }

    @Test
    fun zellijFlagsInsideMultiplexer() {
        val caps = detectCapsFromEnv(env(
            "TERM" to "xterm-ghostty",
            "TERM_PROGRAM" to "ghostty",
            "ZELLIJ" to "0",
            "ZELLIJ_SESSION_NAME" to "main",
        ))
        assertTrue(caps.insideMultiplexer, "ZELLIJ env implies multiplexer")
        // Raw env hints are preserved; the veto happens in mergeCaps.
        assertTrue(caps.kitty)
    }

    @Test
    fun tmuxFlagsInsideMultiplexer() {
        val caps = detectCapsFromEnv(env(
            "TERM" to "tmux-256color",
            "TMUX" to "/tmp/tmux-501/default,1234,0",
        ))
        assertTrue(caps.insideMultiplexer)
    }

    @Test
    fun multiplexerVetoesProbeSixelSupportInMergedCaps() {
        // Host reports feature 4 (sixel) via DA1, but zellij intercepts the
        // image payload — the merge should return sixel=false.
        val probe = parseTerminalResponses("[?64;1;2;4;6;9;22c")
        val envCaps = detectCapsFromEnv(env(
            "TERM" to "xterm-256color",
            "ZELLIJ" to "0",
        ))
        val merged = mergeCaps(probe, envCaps)
        assertFalse(merged.sixelSupported)
        assertFalse(merged.kittySupported)
    }

    @Test
    fun multiplexerVetoesEnvKittyInMergedCaps() {
        val envCaps = detectCapsFromEnv(env(
            "TERM" to "xterm-ghostty",
            "TERM_PROGRAM" to "ghostty",
            "ZELLIJ" to "0",
        ))
        val merged = mergeCaps(probe = null, env = envCaps)
        assertFalse(merged.kittySupported)
        assertFalse(merged.sixelSupported)
    }

    @Test
    fun forceGraphicsOverrideReenablesInsideMultiplexer() {
        val caps = detectCapsFromEnv(env(
            "TERM" to "xterm-ghostty",
            "ZELLIJ" to "0",
            "KOTUI_FORCE_GRAPHICS" to "1",
        ))
        assertFalse(caps.insideMultiplexer, "override clears multiplexer flag")
        val merged = mergeCaps(probe = null, env = caps)
        assertTrue(merged.kittySupported, "override lets Ghostty kitty env propagate")
    }
}

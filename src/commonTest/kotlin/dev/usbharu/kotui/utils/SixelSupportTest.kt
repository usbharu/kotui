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

    @Test
    fun forceGraphicsTrueStringAlsoOverridesMultiplexer() {
        val caps = detectCapsFromEnv(env(
            "TERM" to "screen-256color",
            "KOTUI_FORCE_GRAPHICS" to "true",
        ))

        assertFalse(caps.insideMultiplexer)
    }

    @Test
    fun screenAndStyAreDetectedAsMultiplexers() {
        val termCaps = detectCapsFromEnv(env("TERM" to "screen-256color"))
        assertTrue(termCaps.insideMultiplexer)

        val styCaps = detectCapsFromEnv(env("STY" to "123.session"))
        assertTrue(styCaps.insideMultiplexer)
    }

    @Test
    fun additionalEnvHintsDetectKittyAndSixelProtocols() {
        assertTrue(detectCapsFromEnv(env("KITTY_WINDOW_ID" to "1")).kitty)
        assertTrue(detectCapsFromEnv(env("WEZTERM_EXECUTABLE" to "/usr/bin/wezterm")).kitty)
        assertTrue(detectCapsFromEnv(env("WEZTERM_EXECUTABLE" to "/usr/bin/wezterm")).sixel)
        assertTrue(detectCapsFromEnv(env("TERM_PROGRAM" to "mintty")).sixel)
        assertTrue(detectCapsFromEnv(env("KONSOLE_VERSION" to "240800")).sixel)
        assertTrue(detectCapsFromEnv(env("TERM" to "mlterm")).sixel)
    }

    @Test
    fun terminalResponseParserKeepsDefaultsForMalformedCellSizes() {
        val caps = parseTerminalResponses("\u001B[?1;2c\u001B[6;bad;0t\u001B[6;24;12t")

        assertFalse(caps.sixelSupported)
        assertEquals(12, caps.cellPixelWidth)
        assertEquals(24, caps.cellPixelHeight)
    }

    @Test
    fun terminalResponseParserIgnoresUnterminatedReplies() {
        val caps = parseTerminalResponses("\u001B[?1;4\u001B[6;24;12")

        assertFalse(caps.sixelSupported)
        assertEquals(10, caps.cellPixelWidth)
        assertEquals(20, caps.cellPixelHeight)
    }

    @Test
    fun terminalResponseParserHandlesMissingDa1AndPartialSizeReplies() {
        val noDa1 = parseTerminalResponses("plain text\u001B[6;18;9t")
        assertFalse(noDa1.sixelSupported)
        assertEquals(9, noDa1.cellPixelWidth)
        assertEquals(18, noDa1.cellPixelHeight)

        val noDaTerminator = parseTerminalResponses("\u001B[?1;4")
        assertFalse(noDaTerminator.sixelSupported)

        val partialSize = parseTerminalResponses("\u001B[6;18t\u001B[6;-1;-2t")
        assertEquals(10, partialSize.cellPixelWidth)
        assertEquals(20, partialSize.cellPixelHeight)
    }

    @Test
    fun remainingEnvironmentHintsAreDetected() {
        assertTrue(detectCapsFromEnv(env("TERM_PROGRAM" to "mlterm")).sixel)
        assertTrue(detectCapsFromEnv(env("TERM" to "wezterm-256color")).kitty)
        assertTrue(detectCapsFromEnv(env("TERM" to "wezterm-256color")).sixel)

        val falseForce = detectCapsFromEnv(env(
            "TERM" to "screen-256color",
            "KOTUI_FORCE_GRAPHICS" to "false",
        ))
        assertTrue(falseForce.insideMultiplexer)
    }

    @Test
    fun mergeUsesEnvSixelWhenProbeIsMissingOrNegative() {
        val envOnly = mergeCaps(probe = null, env = EnvCaps(sixel = true, kitty = false))
        assertTrue(envOnly.sixelSupported)
        assertFalse(envOnly.kittySupported)

        val envOverridesNegativeProbe = mergeCaps(
            probe = TerminalCaps(sixelSupported = false, cellPixelWidth = 7, cellPixelHeight = 8),
            env = EnvCaps(sixel = true, kitty = false),
        )
        assertTrue(envOverridesNegativeProbe.sixelSupported)
        assertEquals(7, envOverridesNegativeProbe.cellPixelWidth)
        assertEquals(8, envOverridesNegativeProbe.cellPixelHeight)
    }
}

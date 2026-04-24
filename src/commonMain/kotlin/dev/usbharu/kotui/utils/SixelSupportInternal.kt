package dev.usbharu.kotui.utils

/**
 * Parses the concatenated replies to `CSI c` and `CSI 16 t`.
 *
 * - DA1 reply: `ESC [ ? <params> c` — the presence of feature code 4 indicates sixel support.
 * - CSI 16 t reply (character cell size in pixels): `ESC [ 6 ; <height> ; <width> t`.
 *
 * Terminals that do not understand the queries stay silent; defaults are used
 * in that case.
 */
internal fun parseTerminalResponses(buffer: String): TerminalCaps {
    var sixel = false
    var cellW = 10
    var cellH = 20

    val daIdx = buffer.indexOf("\u001B[?")
    if (daIdx >= 0) {
        val end = buffer.indexOf('c', startIndex = daIdx)
        if (end > daIdx) {
            val body = buffer.substring(daIdx + 3, end)
            val params = body.split(';')
            if (params.any { it.trim() == "4" }) {
                sixel = true
            }
        }
    }

    var i = 0
    while (i < buffer.length) {
        val prefixIdx = buffer.indexOf("\u001B[6;", startIndex = i)
        if (prefixIdx < 0) break
        val end = buffer.indexOf('t', startIndex = prefixIdx)
        if (end < 0) break
        val body = buffer.substring(prefixIdx + 3, end)
        val parts = body.split(';')
        if (parts.size >= 3) {
            val h = parts[1].toIntOrNull()
            val w = parts[2].toIntOrNull()
            if (h != null && h > 0) cellH = h
            if (w != null && w > 0) cellW = w
        }
        i = end + 1
    }

    return TerminalCaps(sixelSupported = sixel, cellPixelWidth = cellW, cellPixelHeight = cellH)
}

/** Query string to send before reading responses. */
internal const val TERMINAL_PROBE = "\u001B[c\u001B[16t"

/** Number of terminator characters expected in responses (DA1 ends with 'c', 16t with 't'). */
internal const val TERMINAL_PROBE_TERMINATORS = 2

/** Image protocol support inferred from environment variables. */
internal data class EnvCaps(
    val sixel: Boolean,
    val kitty: Boolean,
    /**
     * True when the process is running inside a terminal multiplexer
     * (zellij, tmux, screen, …) that intercepts escape sequences. Even if
     * the host terminal supports Sixel / Kitty, the multiplexer typically
     * drops or mangles the image payload, so we treat it as unsupported
     * unless the user explicitly opts in via `KOTUI_FORCE_GRAPHICS=1`.
     */
    val insideMultiplexer: Boolean = false,
)

/**
 * Classifies the running process environment to figure out which terminal
 * image protocols are almost certainly supported.
 *
 * Terminals do not reliably advertise these protocols through DA1:
 * Ghostty, for example, does not announce sixel (nor support it at all) but
 * does implement kgp. Environment identifiers are the most reliable signal we
 * have without doing protocol-specific async round-trips.
 */
internal fun detectCapsFromEnv(env: (String) -> String?): EnvCaps {
    val term = env("TERM")?.lowercase().orEmpty()
    val termProgram = env("TERM_PROGRAM")?.lowercase().orEmpty()

    val forceGraphics = env("KOTUI_FORCE_GRAPHICS")?.let { it == "1" || it.equals("true", ignoreCase = true) } == true
    val insideMultiplexer = !forceGraphics && when {
        env("ZELLIJ") != null -> true
        env("ZELLIJ_SESSION_NAME") != null -> true
        env("TMUX") != null -> true
        env("STY") != null -> true
        term.startsWith("screen") -> true
        term.startsWith("tmux") -> true
        else -> false
    }

    val kitty = when {
        term == "xterm-kitty" -> true
        term.contains("ghostty") -> true
        term.contains("wezterm") -> true
        termProgram == "ghostty" -> true
        termProgram == "wezterm" -> true
        env("KITTY_WINDOW_ID") != null -> true
        env("GHOSTTY_RESOURCES_DIR") != null -> true
        env("WEZTERM_EXECUTABLE") != null -> true
        else -> false
    }

    val sixel = when {
        termProgram == "wezterm" -> true
        termProgram == "mintty" -> true
        termProgram == "mlterm" -> true
        term.startsWith("foot") -> true
        term.contains("mlterm") -> true
        term.contains("wezterm") -> true
        env("WEZTERM_EXECUTABLE") != null -> true
        env("KONSOLE_VERSION") != null -> true
        else -> false
    }

    return EnvCaps(sixel = sixel, kitty = kitty, insideMultiplexer = insideMultiplexer)
}

/**
 * Merges an optional DA1/CSI-16t probe with the environment hints. The probe
 * is the authoritative source for sixel + cell metrics; the environment is the
 * authoritative source for kgp (which has no standard DA1 feature code).
 */
internal fun mergeCaps(probe: TerminalCaps?, env: EnvCaps): TerminalCaps {
    val sixel = if (env.insideMultiplexer) false else (probe?.sixelSupported == true) || env.sixel
    val kitty = if (env.insideMultiplexer) false else env.kitty
    val w = probe?.cellPixelWidth ?: 10
    val h = probe?.cellPixelHeight ?: 20
    return TerminalCaps(
        sixelSupported = sixel,
        kittySupported = kitty,
        cellPixelWidth = w,
        cellPixelHeight = h,
    )
}

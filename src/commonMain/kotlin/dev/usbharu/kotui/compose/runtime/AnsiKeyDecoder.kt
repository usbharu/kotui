package dev.usbharu.kotui.compose.runtime

/**
 * Incremental decoder that translates a stream of input characters (bytes already
 * decoded to UTF-16 chars) into [InputEvent]s. Handles:
 *  - ASCII control chars (Ctrl+A..Z -> KeyEvent with ctrl=true)
 *  - CSI sequences (ESC [ ...) for arrow keys, Home/End, Delete, PgUp/PgDn with
 *    optional modifier parameters (e.g. ESC[1;5C = Ctrl+Right)
 *  - SS3 sequences (ESC O ...) for Home/End on some terminals
 *  - Alt+key (ESC followed by a char)
 *  - Bracketed paste (ESC[200~ ... ESC[201~) emitted as [PasteEvent]
 *
 * ESC vs Alt+key disambiguation: callers emit buffered ESC via [flush] when no
 * further input arrives within a short timeout.
 */
class AnsiKeyDecoder {
    private enum class State { IDLE, ESC, CSI, SS3, PASTE }

    private var state = State.IDLE
    private val csiBuf = StringBuilder()
    private val pasteBuf = StringBuilder()
    private var previousWasCr = false

    /** True if the decoder is mid-sequence and would produce more output with further input. */
    fun hasPending(): Boolean = state != State.IDLE

    fun feed(ch: Char): List<InputEvent> {
        val out = mutableListOf<InputEvent>()
        feedInto(ch, out)
        return out
    }

    fun flush(): List<InputEvent> {
        val out = mutableListOf<InputEvent>()
        when (state) {
            State.ESC -> out += KeyEvent('\u001B', Key.ESCAPE)
            State.CSI -> {
                // Incomplete CSI: surface as raw ESCAPE and discard buffer
                out += KeyEvent('\u001B', Key.ESCAPE)
                csiBuf.clear()
            }
            State.SS3 -> out += KeyEvent('\u001B', Key.ESCAPE)
            State.PASTE -> {
                // Incomplete paste: emit whatever we have
                if (pasteBuf.isNotEmpty()) out += PasteEvent(pasteBuf.toString())
                pasteBuf.clear()
            }
            State.IDLE -> Unit
        }
        state = State.IDLE
        previousWasCr = false
        return out
    }

    private fun feedInto(ch: Char, out: MutableList<InputEvent>) {
        if (state == State.IDLE && previousWasCr) {
            previousWasCr = false
            if (ch == '\n') return
        }
        when (state) {
            State.IDLE -> handleIdle(ch, out)
            State.ESC -> handleEsc(ch, out)
            State.CSI -> handleCsi(ch, out)
            State.SS3 -> handleSs3(ch, out)
            State.PASTE -> handlePaste(ch, out)
        }
    }

    private fun handleIdle(ch: Char, out: MutableList<InputEvent>) {
        when (ch) {
            '\u001B' -> state = State.ESC
            '\t' -> out += KeyEvent(ch, Key.TAB)
            '\r' -> {
                out += KeyEvent(ch, Key.ENTER)
                previousWasCr = true
            }
            '\n' -> out += KeyEvent(ch, Key.ENTER)
            '\u007F', '\b' -> out += KeyEvent(ch, Key.BACKSPACE)
            else -> {
                val code = ch.code
                if (code in 0x01..0x1A) {
                    // Ctrl+A..Z (excluding \t, \r which are handled above)
                    val letter = ('a' + (code - 1))
                    out += KeyEvent(letter, Key.CHAR, ctrl = true)
                } else {
                    out += KeyEvent(ch, Key.CHAR)
                }
            }
        }
    }

    private fun handleEsc(ch: Char, out: MutableList<InputEvent>) {
        when (ch) {
            '[' -> { state = State.CSI; csiBuf.clear() }
            'O' -> state = State.SS3
            '\u001B' -> {
                // ESC ESC: first ESC is a real ESCAPE, second begins another sequence
                out += KeyEvent('\u001B', Key.ESCAPE)
                state = State.ESC
            }
            else -> {
                // ESC + char = Alt+char. For lowercase letter we set alt=true.
                state = State.IDLE
                val code = ch.code
                if (code in 0x01..0x1A) {
                    val letter = ('a' + (code - 1))
                    out += KeyEvent(letter, Key.CHAR, ctrl = true, alt = true)
                } else if (ch == '\u007F' || ch == '\b') {
                    out += KeyEvent(ch, Key.BACKSPACE, alt = true)
                } else {
                    out += KeyEvent(ch, Key.CHAR, alt = true)
                }
            }
        }
    }

    private fun handleCsi(ch: Char, out: MutableList<InputEvent>) {
        // Intermediate: digits, ';', '?', etc. Final: letter or '~'.
        if (ch in '0'..'9' || ch == ';' || ch == '?') {
            csiBuf.append(ch)
            return
        }
        val params = parseCsiParams(csiBuf.toString())
        csiBuf.clear()
        state = State.IDLE

        // Detect bracketed paste start/end: ESC[200~ / ESC[201~
        if (ch == '~' && params.size == 1) {
            when (params[0]) {
                200 -> { state = State.PASTE; pasteBuf.clear(); return }
                201 -> return // stray end marker
            }
        }

        val (keyParam, modParam) = when {
            params.isEmpty() -> 1 to 1
            params.size == 1 -> params[0] to 1
            else -> params[0] to params[1]
        }
        val normalizedModParam = modParam.takeIf { it > 0 } ?: 1
        val ctrl = (normalizedModParam - 1) and 0b100 != 0
        val alt = (normalizedModParam - 1) and 0b010 != 0
        val shift = (normalizedModParam - 1) and 0b001 != 0

        val key = when (ch) {
            'A' -> Key.ARROW_UP
            'B' -> Key.ARROW_DOWN
            'C' -> Key.ARROW_RIGHT
            'D' -> Key.ARROW_LEFT
            'H' -> Key.HOME
            'F' -> Key.END
            '~' -> when (keyParam) {
                1, 7 -> Key.HOME
                2 -> Key.UNKNOWN // Insert — not modelled
                3 -> Key.DELETE
                4, 8 -> Key.END
                5 -> Key.PAGE_UP
                6 -> Key.PAGE_DOWN
                else -> Key.UNKNOWN
            }
            else -> Key.UNKNOWN
        }
        if (key != Key.UNKNOWN) {
            out += KeyEvent('\u0000', key, ctrl = ctrl, shift = shift, alt = alt)
        }
    }

    private fun handleSs3(ch: Char, out: MutableList<InputEvent>) {
        state = State.IDLE
        val key = when (ch) {
            'A' -> Key.ARROW_UP
            'B' -> Key.ARROW_DOWN
            'C' -> Key.ARROW_RIGHT
            'D' -> Key.ARROW_LEFT
            'H' -> Key.HOME
            'F' -> Key.END
            else -> Key.UNKNOWN
        }
        if (key != Key.UNKNOWN) {
            out += KeyEvent('\u0000', key)
        }
    }

    private fun handlePaste(ch: Char, out: MutableList<InputEvent>) {
        // Look for ESC[201~ terminator. We scan the buffer lazily: once we see ESC,
        // switch into a small paste-escape state via csiBuf reuse.
        if (ch == '\u001B') {
            // Begin watching for terminator. Use csiBuf as scratch starting from "^[".
            csiBuf.clear()
            csiBuf.append('\u001B')
            return
        }
        if (csiBuf.isNotEmpty()) {
            csiBuf.append(ch)
            // Expected shape: ESC [ 2 0 1 ~
            val s = csiBuf.toString()
            if ("\u001B[201~".startsWith(s)) {
                if (s == "\u001B[201~") {
                    // End of paste
                    out += PasteEvent(pasteBuf.toString())
                    pasteBuf.clear()
                    csiBuf.clear()
                    state = State.IDLE
                }
                return
            }
            // Mismatch: treat buffered bytes as literal paste content
            pasteBuf.append(s)
            csiBuf.clear()
            return
        }
        pasteBuf.append(ch)
    }

    private fun parseCsiParams(s: String): IntArray {
        if (s.isEmpty()) return IntArray(0)
        val cleaned = if (s.startsWith('?')) s.substring(1) else s
        if (cleaned.isEmpty()) return IntArray(0)
        return cleaned.split(';').map { it.toIntOrNull() ?: 0 }.toIntArray()
    }
}

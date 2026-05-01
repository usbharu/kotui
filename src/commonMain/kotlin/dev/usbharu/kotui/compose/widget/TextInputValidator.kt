package dev.usbharu.kotui.compose.widget

fun interface TextInputValidator {
    fun isValid(value: String): Boolean

    companion object {
        val Any = TextInputValidator { true }
        val DigitsOnly = TextInputValidator { value ->
            value.all { it in '0'..'9' }
        }
        val AsciiLettersOnly = TextInputValidator { value ->
            value.all { it in 'a'..'z' || it in 'A'..'Z' }
        }
        val AsciiAlphanumericOnly = TextInputValidator { value ->
            value.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        }
    }
}

package dev.usbharu.kotui.compose.widget

fun interface TextInputValidator {
    fun isValid(value: String): Boolean

    companion object {
        val AllowAny = TextInputValidator { true }
        val AsciiDigitsOnly = TextInputValidator { value ->
            value.all { it in '0'..'9' }
        }
        val DigitsOnly = AsciiDigitsOnly
        val AsciiLettersOnly = TextInputValidator { value ->
            value.all { it in 'a'..'z' || it in 'A'..'Z' }
        }
        val AsciiAlphanumericOnly = TextInputValidator { value ->
            value.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        }
    }
}

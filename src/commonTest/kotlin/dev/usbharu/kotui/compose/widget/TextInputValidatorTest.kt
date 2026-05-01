package dev.usbharu.kotui.compose.widget

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextInputValidatorTest {

    @Test
    fun allowAnyAcceptsAllValues() {
        assertTrue(TextInputValidator.AllowAny.isValid(""))
        assertTrue(TextInputValidator.AllowAny.isValid("abc123-日本語"))
    }

    @Test
    fun digitsOnlyAllowsEmptyAndAsciiDigits() {
        assertTrue(TextInputValidator.DigitsOnly.isValid(""))
        assertTrue(TextInputValidator.DigitsOnly.isValid("123"))
        assertFalse(TextInputValidator.DigitsOnly.isValid("12a"))
    }

    @Test
    fun asciiLettersOnlyAllowsOnlyAsciiLetters() {
        assertTrue(TextInputValidator.AsciiLettersOnly.isValid("abcXYZ"))
        assertFalse(TextInputValidator.AsciiLettersOnly.isValid("abc1"))
        assertFalse(TextInputValidator.AsciiLettersOnly.isValid("日本語"))
    }

    @Test
    fun asciiAlphanumericOnlyAllowsAsciiLettersAndDigits() {
        assertTrue(TextInputValidator.AsciiAlphanumericOnly.isValid("abcXYZ123"))
        assertFalse(TextInputValidator.AsciiAlphanumericOnly.isValid("abc-123"))
    }
}

package dev.usbharu.kotui.compose.widget

import androidx.compose.runtime.Composable
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.focusScope
import dev.usbharu.kotui.compose.modifier.zIndex

@Composable
fun Modal(title: String = "", modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Panel(title, modifier.zIndex(10).focusScope(), content)
}

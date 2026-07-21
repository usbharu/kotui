package dev.usbharu.kotui.layout

data class Constraints(
    val maxWidth: Int,
    val maxHeight: Int,
    val minWidth: Int = 0,
    val minHeight: Int = 0
) {
    init {
        require(minWidth >= 0 && minHeight >= 0) { "minimum constraints must be non-negative" }
        require(maxWidth >= 0 && maxHeight >= 0) { "maximum constraints must be non-negative" }
        require(minWidth <= maxWidth && minHeight <= maxHeight) { "minimum constraints must not exceed maximum constraints" }
    }
}

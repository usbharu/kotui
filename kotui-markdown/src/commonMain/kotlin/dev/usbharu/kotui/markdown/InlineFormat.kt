package dev.usbharu.kotui.markdown

import dev.usbharu.kotui.core.Style
import dev.usbharu.markdown.AstNode

internal data class StyledSegment(val text: String, val style: Style)

internal fun mergeStyle(parent: Style, override: Style): Style = Style(
    fg = override.fg ?: parent.fg,
    bg = override.bg ?: parent.bg,
    bold = parent.bold || override.bold,
    underline = parent.underline || override.underline,
    reverse = parent.reverse || override.reverse,
)

internal fun flattenInline(
    nodes: List<AstNode>,
    base: Style,
    styles: MarkdownStyles,
): List<List<StyledSegment>> {
    val lines: MutableList<MutableList<StyledSegment>> = mutableListOf(mutableListOf())

    fun push(text: String, style: Style) {
        if (text.isEmpty()) return
        lines.last().add(StyledSegment(text, style))
    }

    fun walk(node: AstNode, style: Style) {
        when (node) {
            is AstNode.PlainText -> push(node.text, style)
            is AstNode.BoldNode -> node.nodes.forEach { walk(it, mergeStyle(style, styles.bold)) }
            is AstNode.ItalicNode -> node.nodes.forEach { walk(it, mergeStyle(style, styles.italic)) }
            is AstNode.StrikeThroughNode -> node.nodes.forEach { walk(it, mergeStyle(style, styles.strike)) }
            is AstNode.InlineCodeNode -> push(node.code, mergeStyle(style, styles.inlineCode))
            is AstNode.UrlNode -> {
                // Show the link label (what users actually read). If there's
                // no label — e.g. a bare autolink — fall back to the href.
                val label = node.urlNameNode.name.ifBlank { node.url.url }
                push(label, mergeStyle(style, styles.link))
            }
            is AstNode.SimpleUrlNode -> push(node.url, mergeStyle(style, styles.link))
            is AstNode.ImageNode -> push(
                "[image: ${node.urlUrlNode.urlNameNode.name.ifBlank { node.urlUrlNode.url.url }}]",
                mergeStyle(style, styles.imageFallback),
            )
            is AstNode.BreakNode -> lines.add(mutableListOf())
            is AstNode.InlineNodes -> node.nodes.forEach { walk(it, style) }
            else -> push("[?${node::class.simpleName}]", styles.errorStyle)
        }
    }

    nodes.forEach { walk(it, base) }
    return lines.map { it.toList() }
}

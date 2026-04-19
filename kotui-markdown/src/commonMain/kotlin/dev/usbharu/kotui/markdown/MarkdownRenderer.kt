package dev.usbharu.kotui.markdown

import androidx.compose.runtime.Composable
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.widget.Column
import dev.usbharu.kotui.compose.widget.Divider
import dev.usbharu.kotui.compose.widget.Text
import dev.usbharu.kotui.core.Style
import dev.usbharu.markdown.AstNode

@Composable
internal fun RenderRoot(root: AstNode.RootNode, styles: MarkdownStyles) {
    val body = root.node
    if (body is AstNode.BodyNode) {
        RenderBody(body, styles)
    } else {
        RenderBlockGroup(body, styles, quoteDepth = 0, listIndent = 0)
    }
}

@Composable
internal fun RenderBody(body: AstNode.BodyNode, styles: MarkdownStyles) {
    // gap=1 at the body level inserts a blank line between top-level blocks.
    Column(gap = 1) {
        body.body.forEach { child ->
            RenderBlockGroup(child, styles, quoteDepth = 0, listIndent = 0)
        }
    }
}

/** Wraps one block so its internal lines stay together without the body-level gap between them. */
@Composable
private fun RenderBlockGroup(
    node: AstNode,
    styles: MarkdownStyles,
    quoteDepth: Int,
    listIndent: Int,
) {
    Column {
        RenderBlock(node, styles, quoteDepth, listIndent)
    }
}

@Composable
private fun RenderBlock(
    node: AstNode,
    styles: MarkdownStyles,
    quoteDepth: Int,
    listIndent: Int,
) {
    when (node) {
        is AstNode.HeaderNode -> RenderHeader(node, styles)
        is AstNode.ParagraphNode -> RenderParagraph(node, styles)
        is AstNode.SeparatorNode -> Divider()
        is AstNode.DiscListNode -> RenderDiscList(node, styles, listIndent)
        is AstNode.DecimalListNode -> RenderDecimalList(node, styles, listIndent)
        is AstNode.QuoteNode -> RenderQuote(node, styles, quoteDepth + 1)
        is AstNode.BodyNode -> RenderBody(node, styles)
        is AstNode.ListItemNode -> RenderListItemBody(node, styles, listIndent)
        is AstNode.InlineNode -> RenderInlineFlow(listOf(node), styles.paragraph, styles)
        else -> Text(
            "[unsupported: ${node::class.simpleName}]",
            Modifier.style(styles.errorStyle),
        )
    }
}

@Composable
private fun RenderHeader(h: AstNode.HeaderNode, styles: MarkdownStyles) {
    val text = h.headerTextNode?.text ?: ""
    val level = h.header.coerceIn(1, 6)
    val style = styles.headerStyle(level)
    // Prefix with the "#" markers so users can visually tell h1–h6 apart.
    val hashes = "#".repeat(level) + " "
    StyledLine(
        segments = listOf(StyledSegment(text, style)),
        base = style,
        prefix = hashes,
        prefixStyle = style,
    )
}

@Composable
private fun RenderParagraph(p: AstNode.ParagraphNode, styles: MarkdownStyles) {
    RenderInlineFlow(p.nodes, styles.paragraph, styles)
}

@Composable
private fun RenderInlineFlow(
    nodes: List<AstNode>,
    base: Style,
    styles: MarkdownStyles,
    prefix: String = "",
    prefixStyle: Style = base,
) {
    val lines = flattenInline(nodes, base, styles)
    val indent = " ".repeat(prefix.length)
    lines.forEachIndexed { i, line ->
        StyledLine(
            segments = line,
            base = base,
            prefix = if (i == 0) prefix else indent,
            prefixStyle = prefixStyle,
        )
    }
}

@Composable
private fun RenderDiscList(
    list: AstNode.DiscListNode,
    styles: MarkdownStyles,
    indent: Int,
) {
    list.itemNode.forEach { item ->
        RenderListItem(item, styles, indent, bullet = "•", bulletStyle = styles.listBullet)
    }
}

@Composable
private fun RenderDecimalList(
    list: AstNode.DecimalListNode,
    styles: MarkdownStyles,
    indent: Int,
) {
    list.itemNode.forEachIndexed { i, item ->
        RenderListItem(item, styles, indent, bullet = "${i + 1}.", bulletStyle = styles.listNumber)
    }
}

@Composable
private fun RenderListItem(
    item: AstNode.ListItemNode,
    styles: MarkdownStyles,
    indent: Int,
    bullet: String,
    bulletStyle: Style,
) {
    val leading = " ".repeat(indent * 2)
    val prefix = "$leading$bullet "
    RenderListItemBody(item, styles, indent, prefix, bulletStyle)
}

@Composable
private fun RenderListItemBody(
    item: AstNode.ListItemNode,
    styles: MarkdownStyles,
    indent: Int,
    firstLinePrefix: String = " ".repeat(indent * 2),
    prefixStyle: Style = styles.paragraph,
) {
    val inlineBuf = mutableListOf<AstNode>()
    var emittedFirst = false
    val followIndent = " ".repeat(firstLinePrefix.length)

    @Composable
    fun flushInline() {
        if (inlineBuf.isEmpty()) return
        RenderInlineFlow(
            nodes = inlineBuf.toList(),
            base = styles.paragraph,
            styles = styles,
            prefix = if (!emittedFirst) firstLinePrefix else followIndent,
            prefixStyle = if (!emittedFirst) prefixStyle else styles.paragraph,
        )
        emittedFirst = true
        inlineBuf.clear()
    }

    item.nodes.forEach { node ->
        when (node) {
            is AstNode.DiscListNode -> {
                flushInline()
                RenderDiscList(node, styles, indent + 1)
            }
            is AstNode.DecimalListNode -> {
                flushInline()
                RenderDecimalList(node, styles, indent + 1)
            }
            is AstNode.InlineNode -> inlineBuf.add(node)
            else -> {
                flushInline()
                RenderBlock(node as AstNode, styles, quoteDepth = 0, listIndent = indent)
            }
        }
    }
    flushInline()
}

@Composable
private fun RenderQuote(
    q: AstNode.QuoteNode,
    styles: MarkdownStyles,
    depth: Int,
) {
    val prefix = "> ".repeat(depth)
    val inlineBuf = mutableListOf<AstNode>()

    @Composable
    fun flushInline() {
        if (inlineBuf.isEmpty()) return
        RenderInlineFlow(
            nodes = inlineBuf.toList(),
            base = styles.quoteText,
            styles = styles,
            prefix = prefix,
            prefixStyle = styles.quoteBar,
        )
        inlineBuf.clear()
    }

    q.nodes.forEach { child ->
        when (child) {
            is AstNode.QuoteNode -> {
                flushInline()
                RenderQuote(child, styles, depth + 1)
            }
            is AstNode.InlineNode -> inlineBuf.add(child)
            else -> flushInline()
        }
    }
    flushInline()
}

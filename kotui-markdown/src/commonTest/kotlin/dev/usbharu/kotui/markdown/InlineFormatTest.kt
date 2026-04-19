package dev.usbharu.kotui.markdown

import dev.usbharu.kotui.core.Style
import dev.usbharu.markdown.AstNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineFormatTest {
    private val styles = MarkdownStyles()

    @Test
    fun plainText_singleLine_singleSegment() {
        val lines = flattenInline(
            nodes = listOf(AstNode.PlainText("hello")),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].size)
        assertEquals("hello", lines[0][0].text)
        assertEquals(styles.paragraph, lines[0][0].style)
    }

    @Test
    fun boldWrapsBase_segmentStyleIsBold() {
        val lines = flattenInline(
            nodes = listOf(
                AstNode.BoldNode(mutableListOf(AstNode.PlainText("hi"))),
            ),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].size)
        assertTrue(lines[0][0].style.bold, "bold should be true")
    }

    @Test
    fun italicInsideBold_bothApplied() {
        val lines = flattenInline(
            nodes = listOf(
                AstNode.BoldNode(
                    mutableListOf(
                        AstNode.ItalicNode(mutableListOf(AstNode.PlainText("hi"))),
                    ),
                ),
            ),
            base = styles.paragraph,
            styles = styles,
        )
        val seg = lines[0][0]
        assertTrue(seg.style.bold, "bold carries through")
        assertTrue(seg.style.underline, "italic style uses underline")
    }

    @Test
    fun breakNode_splitsLines() {
        val lines = flattenInline(
            nodes = listOf(
                AstNode.PlainText("a"),
                AstNode.BreakNode,
                AstNode.PlainText("b"),
            ),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals(2, lines.size)
        assertEquals("a", lines[0][0].text)
        assertEquals("b", lines[1][0].text)
    }

    @Test
    fun inlineCode_usesInlineCodeStyle() {
        val lines = flattenInline(
            nodes = listOf(AstNode.InlineCodeNode("x")),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals(styles.inlineCode, lines[0][0].style)
        assertEquals("x", lines[0][0].text)
    }

    @Test
    fun urlNode_rendersLabel() {
        val lines = flattenInline(
            nodes = listOf(
                AstNode.UrlNode(
                    url = AstNode.UrlUrlNode("https://example.com"),
                    urlNameNode = AstNode.UrlNameNode("site"),
                    urlTitleNode = null,
                ),
            ),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals("site", lines[0][0].text)
        assertEquals(styles.link, lines[0][0].style)
    }

    @Test
    fun imageNode_rendersFallbackText() {
        val lines = flattenInline(
            nodes = listOf(
                AstNode.ImageNode(
                    AstNode.UrlNode(
                        url = AstNode.UrlUrlNode("https://example.com/cat.png"),
                        urlNameNode = AstNode.UrlNameNode("cat"),
                        urlTitleNode = null,
                    ),
                ),
            ),
            base = styles.paragraph,
            styles = styles,
        )
        assertEquals("[image: cat]", lines[0][0].text)
        assertEquals(styles.imageFallback, lines[0][0].style)
    }

    @Test
    fun strikeThroughNode_usesStrikeStyle() {
        val custom = styles.copy(strike = Style(fg = "X", reverse = true))
        val lines = flattenInline(
            nodes = listOf(
                AstNode.StrikeThroughNode(listOf(AstNode.PlainText("dead"))),
            ),
            base = styles.paragraph,
            styles = custom,
        )
        assertTrue(lines[0][0].style.reverse, "strike degrades to reverse")
        assertEquals("X", lines[0][0].style.fg)
    }
}

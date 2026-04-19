package dev.usbharu.kotui.markdown

import dev.usbharu.markdown.AstNode
import dev.usbharu.markdown.Lexer
import dev.usbharu.markdown.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownRenderSmokeTest {

    private fun parse(source: String): AstNode.BodyNode {
        val tokens = Lexer().lex(source)
        val root = Parser().parse(tokens) as AstNode.RootNode
        return root.node as AstNode.BodyNode
    }

    @Test
    fun headers_areParsedAsHeaderNodes() {
        val body = parse(
            """
            # H1
            ## H2
            """.trimIndent(),
        )
        val headers = body.body.filterIsInstance<AstNode.HeaderNode>()
        assertEquals(2, headers.size)
        assertEquals(1, headers[0].header)
        assertEquals(2, headers[1].header)
    }

    @Test
    fun separator_isSeparatorNode() {
        val body = parse("---")
        assertTrue(body.body.any { it is AstNode.SeparatorNode })
    }

    @Test
    fun discList_isDiscListNode() {
        val body = parse(
            """
            - one
            - two
            """.trimIndent(),
        )
        val list = body.body.filterIsInstance<AstNode.DiscListNode>().firstOrNull()
        assertTrue(list != null, "expected a DiscListNode in body")
        assertEquals(2, list.itemNode.size)
    }

    @Test
    fun paragraph_withBold_containsBoldNode() {
        val body = parse("hello **world**")
        val para = body.body.filterIsInstance<AstNode.ParagraphNode>().firstOrNull()
        assertTrue(para != null)
        // Bold text somewhere under the paragraph's inline tree.
        val containsBold = para.nodes.any { containsBoldRec(it) }
        assertTrue(containsBold, "paragraph should contain a BoldNode somewhere")
    }

    private fun containsBoldRec(node: AstNode): Boolean {
        if (node is AstNode.BoldNode) return true
        return when (node) {
            is AstNode.InlineNodes -> node.nodes.any { containsBoldRec(it) }
            is AstNode.ItalicNode -> node.nodes.any { containsBoldRec(it) }
            is AstNode.StrikeThroughNode -> node.nodes.any { containsBoldRec(it) }
            else -> false
        }
    }
}

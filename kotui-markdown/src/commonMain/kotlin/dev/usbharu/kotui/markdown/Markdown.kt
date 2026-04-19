package dev.usbharu.kotui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.usbharu.kotui.compose.modifier.Modifier
import dev.usbharu.kotui.compose.modifier.style
import dev.usbharu.kotui.compose.widget.Column
import dev.usbharu.kotui.compose.widget.Text
import dev.usbharu.markdown.AstNode
import dev.usbharu.markdown.Lexer
import dev.usbharu.markdown.Parser

internal data class ParseResult(val root: AstNode?, val error: String?)

@Composable
fun Markdown(
    source: String,
    modifier: Modifier = Modifier,
    styles: MarkdownStyles = MarkdownStyles(),
) {
    val result = remember(source) {
        try {
            ParseResult(root = Parser().parse(Lexer().lex(source)), error = null)
        } catch (t: Throwable) {
            ParseResult(root = null, error = t.message ?: t::class.simpleName)
        }
    }

    Column(modifier = modifier) {
        val parsed = result.root
        if (parsed == null) {
            Text(
                "[markdown parse error: ${result.error}]",
                Modifier.style(styles.errorStyle),
            )
            return@Column
        }
        when (parsed) {
            is AstNode.RootNode -> RenderRoot(parsed, styles)
            is AstNode.BodyNode -> RenderBody(parsed, styles)
            else -> Text(
                "[unexpected root: ${parsed::class.simpleName}]",
                Modifier.style(styles.errorStyle),
            )
        }
    }
}

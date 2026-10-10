package com.sabin.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color

/** Text with http/https URLs as underlined, tappable links (opened via the platform URI handler, ACTION_VIEW). */
internal fun linkified(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    val style = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    LinkDetector.find(text).forEach {
        addLink(LinkAnnotation.Url(text.substring(it.first, it.last + 1), style), it.first, it.last + 1)
    }
}

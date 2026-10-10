package com.sabin.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Decorative illustrations drawn on a Canvas; the surrounding text carries the meaning. */
@Composable
fun EmptyIllustrationView(kind: EmptyIllustration, blob: Color, ink: Color, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(144.dp)) {
        val w = size.width
        val line = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        drawCircle(blob, radius = w * 0.5f)
        when (kind) {
            EmptyIllustration.NOTES -> {
                drawRoundRect(ink, Offset(w * 0.28f, w * 0.2f), Size(w * 0.44f, w * 0.58f), CornerRadius(w * 0.06f), style = line)
                for (i in 0..2) drawLine(ink, Offset(w * 0.37f, w * (0.36f + i * 0.12f)), Offset(w * 0.63f, w * (0.36f + i * 0.12f)), line.width, StrokeCap.Round)
                drawCircle(accent, w * 0.1f, Offset(w * 0.72f, w * 0.74f))
                drawLine(blob, Offset(w * 0.72f, w * 0.69f), Offset(w * 0.72f, w * 0.79f), line.width, StrokeCap.Round)
                drawLine(blob, Offset(w * 0.67f, w * 0.74f), Offset(w * 0.77f, w * 0.74f), line.width, StrokeCap.Round)
            }
            EmptyIllustration.SEARCH -> {
                drawCircle(ink, w * 0.2f, Offset(w * 0.45f, w * 0.45f), style = line)
                drawLine(accent, Offset(w * 0.6f, w * 0.6f), Offset(w * 0.76f, w * 0.76f), line.width * 1.6f, StrokeCap.Round)
            }
            EmptyIllustration.ARCHIVE -> {
                drawRoundRect(ink, Offset(w * 0.24f, w * 0.28f), Size(w * 0.52f, w * 0.14f), CornerRadius(w * 0.03f), style = line)
                drawRoundRect(ink, Offset(w * 0.28f, w * 0.42f), Size(w * 0.44f, w * 0.3f), CornerRadius(w * 0.03f), style = line)
                drawLine(accent, Offset(w * 0.43f, w * 0.52f), Offset(w * 0.57f, w * 0.52f), line.width * 1.4f, StrokeCap.Round)
            }
            EmptyIllustration.TRASH -> {
                drawRoundRect(ink, Offset(w * 0.32f, w * 0.34f), Size(w * 0.36f, w * 0.42f), CornerRadius(w * 0.05f), style = line)
                drawLine(ink, Offset(w * 0.27f, w * 0.3f), Offset(w * 0.73f, w * 0.3f), line.width, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.45f, w * 0.24f), Offset(w * 0.55f, w * 0.24f), line.width * 1.4f, StrokeCap.Round)
            }
        }
    }
}

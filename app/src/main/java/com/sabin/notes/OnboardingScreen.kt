package com.sabin.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val illustrations = listOf(R.drawable.ic_onboarding_capture, R.drawable.ic_onboarding_search, R.drawable.ic_onboarding_pin)

/** Three short screens; [onDone] fires on Skip or on the last screen's button (which opens the first note). */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val page = Onboarding.pages[index]
    val last = Onboarding.isLast(index)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.skip)) }
            }
            Spacer(Modifier.weight(1f))
            val backdrop = MaterialTheme.colorScheme.secondaryContainer
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    drawRoundRect(backdrop, size = size, cornerRadius = CornerRadius(BrandColors.BACKDROP_CORNER_DP.dp.toPx()))
                }
                Image(painterResource(illustrations[index]), contentDescription = null, modifier = Modifier.size(220.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(page.benefit, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.page_of, index + 1, Onboarding.pages.size), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            // Decorative: the text above carries the position for TalkBack. Current page is wide, others round (shape, not just colour).
            Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Onboarding.pages.indices.forEach { i ->
                    val on = i == index
                    Box(
                        Modifier.height(8.dp).width(if (on) 24.dp else 8.dp).clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { if (last) onDone() else index++ }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (last) stringResource(R.string.write_first_note) else stringResource(R.string.next))
            }
        }
    }
}

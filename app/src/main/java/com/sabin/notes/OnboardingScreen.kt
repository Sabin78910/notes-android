package com.sabin.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text("Skip") }
            }
            Spacer(Modifier.weight(1f))
            Image(painterResource(illustrations[index]), contentDescription = null, modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(24.dp))
            Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(page.benefit, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("${index + 1} of ${Onboarding.pages.size}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { if (last) onDone() else index++ }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (last) "Write your first note" else "Next")
            }
        }
    }
}

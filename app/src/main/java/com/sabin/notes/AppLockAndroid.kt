package com.sabin.notes

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.runtime.mutableStateOf

internal const val KEY_APP_LOCK = "app_lock_enabled"

internal object AppLock {
    fun available(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true

    fun intent(context: Context): Intent? =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.createConfirmDeviceCredentialIntent(
            context.getString(R.string.app_lock_prompt_title), context.getString(R.string.app_lock_prompt_body)
        )
}

/** Shown instead of any note content until the device credential is confirmed. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var prompted by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) onUnlocked()
    }
    fun prompt() {
        val intent = AppLock.intent(context)
        if (intent == null) onUnlocked() else launcher.launch(intent)
    }
    LaunchedEffect(Unit) { if (!prompted) { prompted = true; prompt() } }
    val locked = stringResource(R.string.app_lock_locked)
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp).semantics { contentDescription = locked },
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(locked, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Button(onClick = { prompt() }, modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.app_lock_unlock))
            }
        }
    }
}

package com.sabin.notes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) { NotesScreen() }
        }
    }
}

private const val PREFS = "notes"
private const val KEY = "data"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val store = remember { NoteStore.deserialize(prefs.getString(KEY, "") ?: "") }
    var version by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var newestFirst by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notes = remember(version, query, newestFirst) { store.visible(query, newestFirst) }

    fun save() { prefs.edit().putString(KEY, store.serialize()).apply(); version++ }

    editing?.let { n ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit note") },
            text = { OutlinedTextField(editText, { editText = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    enabled = editText.isNotBlank(),
                    onClick = { store.edit(n.id, editText); editing = null; save() }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    fun deleteWithUndo(n: Note) {
        store.delete(n.id); save()
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar("Note deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) { store.restore(n); save() }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notes") }) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (newestFirst) "Sort: Newest first" else "Sort: Oldest first")
            }
            OutlinedTextField(draft, { draft = it }, label = { Text("New note") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { if (draft.isNotBlank()) { store.add(draft); draft = ""; save() } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) { Text("Add note") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { n ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            PinPresentation.badge(n.pinned)?.let {
                                Text("📌 $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(n.text)
                            if (n.createdAt > 0) {
                                Text(
                                    "Created " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(n.createdAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                TextButton(onClick = { editText = n.text; editing = n }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.editDescription(n.text) }) { Text("Edit") }
                                TextButton(onClick = { store.togglePin(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.pinDescription(n.pinned, n.text) }) { Text(PinPresentation.buttonLabel(n.pinned)) }
                                TextButton(onClick = { deleteWithUndo(n) }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.deleteDescription(n.text) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

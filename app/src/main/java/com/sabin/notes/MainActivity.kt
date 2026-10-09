package com.sabin.notes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { NotesScreen() } }
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
    var editing by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notes = remember(version, query) { store.visible(query) }

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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(draft, { draft = it }, label = { Text("New note") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { if (draft.isNotBlank()) { store.add(draft); draft = ""; save() } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) { Text("Add note") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { n ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text((if (n.pinned) "📌 " else "") + n.text)
                            Row {
                                TextButton(onClick = { editText = n.text; editing = n }) { Text("Edit") }
                                TextButton(onClick = { store.togglePin(n.id); save() }) { Text(if (n.pinned) "Unpin" else "Pin") }
                                TextButton(onClick = { deleteWithUndo(n) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

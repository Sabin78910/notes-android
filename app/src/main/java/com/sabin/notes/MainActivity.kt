package com.sabin.notes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
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
            NotesTheme { NotesScreen() }
        }
    }
}

private const val PREFS = "notes"
private const val KEY = "data"
private const val KEY_LAYOUT = "layout"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val store = remember { NoteStore.deserialize(prefs.getString(KEY, "") ?: "") .also { it.purgeExpired() } }
    var view by remember { mutableStateOf("Notes") }
    var version by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf(LayoutMode.fromName(prefs.getString(KEY_LAYOUT, null))) }
    var newestFirst by remember { mutableStateOf(true) }
    var colorFilter by remember { mutableStateOf<NoteColor?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var tagging by remember { mutableStateOf<Note?>(null) }
    var tagText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }
    val resolver = LocalContext.current.contentResolver
    val animationsEnabled = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notes = remember(version, query, newestFirst, colorFilter, tagFilter, view) {
        when (view) {
            "Archive" -> store.archived()
            "Trash" -> store.trashed()
            else -> store.visible(query, newestFirst, colorFilter, tagFilter)
        }
    }
    val dark = isSystemInDarkTheme()

    fun save() { prefs.edit().putString(KEY, store.serialize()).apply(); version++ }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching { resolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.export(store.all()).toByteArray()) } }.isSuccess
            scope.launch { snackbarHost.showSnackbar(if (ok) "Backup saved" else "Could not save backup") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { resolver.openInputStream(uri)!!.use { String(it.readBytes()) } }.getOrNull()
            val msg = when (val r = text?.let { Backup.import(store, it) }) {
                is Backup.Result.Imported -> { save(); "Restored ${r.added} new notes" }
                is Backup.Result.Error -> r.message
                null -> "Could not read file"
            }
            scope.launch { snackbarHost.showSnackbar(msg) }
        }
    }


    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New note") },
            text = { OutlinedTextField(draft, { draft = it }, label = { Text("New note") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { store.add(draft); draft = ""; adding = false; save() }
                ) { Text("Add note") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }
        )
    }

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

    tagging?.let { n ->
        AlertDialog(
            onDismissRequest = { tagging = null },
            title = { Text("Add tags") },
            text = { OutlinedTextField(tagText, { tagText = it }, label = { Text("e.g. #work #ideas") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    enabled = Tags.parse(tagText).isNotEmpty(),
                    onClick = { store.addTags(n.id, tagText); tagging = null; save() }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { tagging = null }) { Text("Cancel") } }
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Notes") }, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        LazyVerticalStaggeredGrid(
            StaggeredGridCells.Fixed(layout.columns),
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
          item(span = StaggeredGridItemSpan.FullLine) { Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Notes", "Archive", "Trash").forEach { v ->
                    FilterChip(view == v, { view = v }, label = { Text(v) })
                }
            }
            if (view == "Trash") {
                TextButton(onClick = { store.emptyTrash(); save() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Empty trash") }
                Text("Notes in Trash are deleted after 30 days.", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { exportLauncher.launch("notes-backup.json") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back up") }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Restore") }
            }
            OutlinedTextField(query, { query = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (newestFirst) "Sort: Newest first" else "Sort: Oldest first")
                }
                TextButton(
                    onClick = { layout = layout.toggled(); prefs.edit().putString(KEY_LAYOUT, layout.name).apply() },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = layout.toggleDescription() }
                ) { Text(if (layout == LayoutMode.GRID) "Grid view" else "List view") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(colorFilter == null, { colorFilter = null }, label = { Text("All") })
                NoteColor.entries.forEach { c ->
                    FilterChip(
                        colorFilter == c, { colorFilter = if (colorFilter == c) null else c },
                        label = { Text(if (colorFilter == c) "✓ ${c.label}" else c.label, color = Color(c.text(dark))) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(c.background(dark)), selectedContainerColor = Color(c.background(dark)))
                    )
                }
            }
            val allTags = remember(version) { store.allTags() }
            if (tagFilter != null && tagFilter !in allTags) tagFilter = null
            if (allTags.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.forEach { t ->
                        FilterChip(tagFilter == t, { tagFilter = if (tagFilter == t) null else t }, label = { Text(if (tagFilter == t) "✓ #$t" else "#$t") })
                    }
                }
            }
          } }
                if (notes.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        val filtered = query.isNotBlank() || colorFilter != null || tagFilter != null
                        Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (view == "Trash") "🗑️" else if (view == "Archive") "📦" else if (filtered) "🔍" else "📝", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(12.dp))
                            Text(EmptyState.message(view, filtered), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
                items(notes, key = { it.id }) { n ->
                    val fg = n.color?.let { Color(it.text(dark)) } ?: Color.Unspecified
                    Card(
                        Modifier.fillMaxWidth().animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        colors = n.color?.let { CardDefaults.cardColors(containerColor = Color(it.background(dark)), contentColor = fg) } ?: CardDefaults.cardColors()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            PinPresentation.badge(n.pinned)?.let {
                                Text("📌 $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (n.checklist) {
                                val p = Checklist.progress(n)
                                Text(p.label, style = MaterialTheme.typography.labelMedium)
                                Checklist.items(n.text).take(NotePreview.MAX_ITEMS).forEachIndexed { idx, item ->
                                    val ticked = idx in n.checked
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            .clickable { store.toggleItem(n.id, idx); save() },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(ticked, onCheckedChange = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(item, textDecoration = if (ticked) TextDecoration.LineThrough else null)
                                    }
                                }
                                if (NotePreview.hiddenItems(n) > 0) Text("+${NotePreview.hiddenItems(n)} more", style = MaterialTheme.typography.labelMedium)
                                if (Checklist.shouldCelebrate(p, animationsEnabled)) {
                                    Text("🎉 All done!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            } else Text(NotePreview.text(n), maxLines = NotePreview.MAX_LINES, overflow = TextOverflow.Ellipsis)
                            if (n.createdAt > 0) {
                                Text(
                                    "Created " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(n.createdAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (n.tags.isNotEmpty()) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    n.tags.forEach { t ->
                                        AssistChip(
                                            onClick = { store.removeTag(n.id, t); save() },
                                            label = { Text("#$t ✕") },
                                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Remove tag $t" }
                                        )
                                    }
                                }
                            }
                            Row {
                                if (view == "Trash") {
                                    TextButton(onClick = { store.restoreFromTrash(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text("Restore") }
                                } else {
                                TextButton(onClick = { editText = n.text; editing = n }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.editDescription(n.text) }) { Text("Edit") }
                                TextButton(onClick = { store.togglePin(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.pinDescription(n.pinned, n.text) }) { Text(PinPresentation.buttonLabel(n.pinned)) }
                                TextButton(onClick = { store.setChecklist(n.id, !n.checklist); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(if (n.checklist) "Plain note" else "Checklist") }
                                TextButton(onClick = { store.setColor(n.id, NoteColor.entries.let { e -> if (n.color == null) e.first() else e.getOrNull(n.color.ordinal + 1) }); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(n.color?.let { "Colour: ${it.label}" } ?: "Colour") }
                                TextButton(onClick = { tagText = ""; tagging = n }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text("Tag") }
                                TextButton(onClick = { deleteWithUndo(n) }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.deleteDescription(n.text) }) { Text("Delete") }
                                TextButton(onClick = { if (n.archived) store.unarchive(n.id) else store.archive(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(if (n.archived) "Unarchive" else "Archive") }
                                }
                            }
                        }
                    }
                }
            }
    }
}

package com.sabin.notes

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var launchAction by mutableStateOf<LaunchAction?>(null)
    private var openNoteId by mutableStateOf<Long?>(null)
    private var sharedText by mutableStateOf<String?>(null)

    private fun readShared(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        sharedText = ShareText.incoming(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchAction = LaunchAction.fromName(intent.getStringExtra(LaunchAction.EXTRA))
        openNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it >= 0 }
        readShared(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        launchAction = LaunchAction.fromName(intent?.getStringExtra(LaunchAction.EXTRA))
        openNoteId = intent?.getLongExtra(EXTRA_NOTE_ID, -1L)?.takeIf { it >= 0 }
        if (savedInstanceState == null) readShared(intent)
        setContent {
            NotesTheme { NotesScreen(launchAction, onLaunchActionHandled = { launchAction = null }, openNoteId, onNoteOpened = { openNoteId = null }, sharedText, onSharedHandled = { sharedText = null }) }
        }
    }
}

internal const val PREFS = "notes"
internal const val KEY = "data"
private const val KEY_LAYOUT = "layout"
private const val KEY_ONBOARDED = "onboarded"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(launchAction: LaunchAction? = null, onLaunchActionHandled: () -> Unit = {}, openNoteId: Long? = null, onNoteOpened: () -> Unit = {}, sharedText: String? = null, onSharedHandled: () -> Unit = {}) {
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val store = remember { NoteStore.deserialize(prefs.getString(KEY, "") ?: "") .also { it.purgeExpired() } }
    val activity = androidx.activity.compose.LocalActivity.current
    val reviewPrompt = remember {
        activity?.let { ReviewPrompt(PrefsReviewState(prefs), PlayReviewGateway(it)).also { r -> r.onAppLaunch(System.currentTimeMillis()) } }
    }
    var view by remember { mutableStateOf("Notes") }
    var version by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var addAsChecklist by remember { mutableStateOf(false) }
    val onboardingFlag = remember {
        object : Onboarding.Flag {
            override fun isSeen() = prefs.getBoolean(KEY_ONBOARDED, false)
            override fun markSeen() { prefs.edit().putBoolean(KEY_ONBOARDED, true).apply() }
        }
    }
    var onboarding by remember { mutableStateOf(Onboarding.shouldShow(onboardingFlag)) }
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

    val appContext = LocalContext.current.applicationContext
    val scheduler = remember { ReminderScheduler(AndroidAlarms(appContext)) }
    var remindersOn by remember { mutableStateOf(prefs.getBoolean(KEY_REMINDERS, false) && ReminderPermission.granted(appContext)) }
    var explainReminders by remember { mutableStateOf(false) }
    var reminderFor by remember { mutableStateOf<Note?>(null) }
    var pickingFor by remember { mutableStateOf<Note?>(null) }
    fun setRemindersEnabled(on: Boolean) {
        remindersOn = on; prefs.edit().putBoolean(KEY_REMINDERS, on).apply()
        if (on) scheduler.rescheduleAll(store.all()) else scheduler.cancelAll(store.all())
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setRemindersEnabled(granted)
        if (granted) pickingFor = reminderFor else scope.launch { snackbarHost.showSnackbar(appContext.getString(R.string.msg_need_permission)) }
        reminderFor = null
    }
    fun save() {
        prefs.edit().putString(KEY, store.serialize()).apply(); version++
        scope.launch { NotesWidget.refresh(appContext) }
        if (remindersOn) scheduler.rescheduleAll(store.all()) else scheduler.cancelAll(store.all())
    }

    LaunchedEffect(openNoteId) {
        if (openNoteId != null) {
            store.all().firstOrNull { it.id == openNoteId && it.trashedAt == null }?.let { editText = it.text; editing = it }
            onNoteOpened()
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            draft = sharedText
            addAsChecklist = false
            adding = true
            onSharedHandled()
        }
    }

    LaunchedEffect(launchAction) {
        if (launchAction != null) {
            addAsChecklist = launchAction == LaunchAction.NEW_CHECKLIST
            adding = true
            onLaunchActionHandled()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching { resolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.export(store.all()).toByteArray()) } }.isSuccess
            if (!ok) reviewPrompt?.onError()
            scope.launch { snackbarHost.showSnackbar(appContext.getString(if (ok) R.string.msg_backup_saved else R.string.msg_backup_failed)) }
        }
    }
    var exportingNote by remember { mutableStateOf<Note?>(null) }
    val noteExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val n = exportingNote
        exportingNote = null
        if (uri != null && n != null) {
            val ok = runCatching { resolver.openOutputStream(uri, "wt")!!.use { it.write(NoteExport.toMarkdown(n).toByteArray()) } }.isSuccess
            if (!ok) reviewPrompt?.onError()
            scope.launch { snackbarHost.showSnackbar(appContext.getString(if (ok) R.string.msg_note_exported else R.string.msg_note_export_failed)) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { resolver.openInputStream(uri)!!.use { String(it.readBytes()) } }.getOrNull()
            val msg = when (val r = text?.let { Backup.import(store, it) }) {
                is Backup.Result.Imported -> { save(); appContext.getString(R.string.msg_restored, r.added) }
                is Backup.Result.Error -> { reviewPrompt?.onError(); r.message }
                null -> { reviewPrompt?.onError(); appContext.getString(R.string.msg_read_failed) }
            }
            scope.launch { snackbarHost.showSnackbar(msg) }
        }
    }


    if (onboarding) {
        OnboardingScreen(onDone = { Onboarding.finish(onboardingFlag); onboarding = false; adding = true })
        return
    }

    if (adding) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        AlertDialog(
            onDismissRequest = { adding = false; addAsChecklist = false },
            title = { Text(if (addAsChecklist) stringResource(R.string.new_checklist) else stringResource(R.string.new_note)) },
            text = { OutlinedTextField(draft, { draft = it }, label = { Text(if (addAsChecklist) stringResource(R.string.one_item_per_line) else stringResource(R.string.new_note)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus)) },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { val n = store.add(draft); if (addAsChecklist) store.setChecklist(n.id, true); draft = ""; adding = false; addAsChecklist = false; save(); reviewPrompt?.onNoteAdded(store.all().count { it.trashedAt == null }, System.currentTimeMillis()) }
                ) { Text(stringResource(R.string.add_note_button)) }
            },
            dismissButton = { TextButton(onClick = { adding = false; addAsChecklist = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    editing?.let { n ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_note_title)) },
            text = {
                Column {
                    OutlinedTextField(editText, { editText = it }, modifier = Modifier.fillMaxWidth())
                    val res = LocalContext.current.resources
                    val summary = if (n.checklist) {
                        val p = WordCount.checklistProgress(editText, n.checked)
                        stringResource(R.string.checklist_done_count, p.done, p.total)
                    } else {
                        val c = WordCount.of(editText)
                        stringResource(R.string.count_summary, res.getQuantityString(R.plurals.word_count, c.words, c.words), res.getQuantityString(R.plurals.character_count, c.characters, c.characters))
                    }
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp).semantics { contentDescription = summary; liveRegion = LiveRegionMode.Polite })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editText.isNotBlank(),
                    onClick = { store.edit(n.id, editText); editing = null; save() }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ShareText.outgoing(n.copy(text = editText)))
                            appContext.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        modifier = Modifier.semantics { contentDescription = appContext.getString(R.string.share_note_description) }
                    ) { Text(stringResource(R.string.share)) }
                    TextButton(onClick = {
                        val current = n.copy(text = editText)
                        exportingNote = current
                        noteExportLauncher.launch(NoteExport.fileName(current.text))
                    }) { Text(stringResource(R.string.export_note)) }
                    TextButton(
                        onClick = {
                            store.duplicate(n.id)
                            editing = null
                            save()
                            scope.launch { snackbarHost.showSnackbar(appContext.getString(R.string.msg_note_duplicated)) }
                        },
                        modifier = Modifier.semantics { contentDescription = appContext.getString(R.string.duplicate_note_description) }
                    ) { Text(stringResource(R.string.duplicate_note)) }
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) }
                }
            }
        )
    }

    tagging?.let { n ->
        AlertDialog(
            onDismissRequest = { tagging = null },
            title = { Text(stringResource(R.string.add_tags_title)) },
            text = { OutlinedTextField(tagText, { tagText = it }, label = { Text(stringResource(R.string.tags_hint)) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    enabled = Tags.parse(tagText).isNotEmpty(),
                    onClick = { store.addTags(n.id, tagText); tagging = null; save() }
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { tagging = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (explainReminders) {
        AlertDialog(
            onDismissRequest = { explainReminders = false; reminderFor = null },
            title = { Text(stringResource(R.string.allow_reminders_title)) },
            text = { Text(stringResource(R.string.allow_reminders_body)) },
            confirmButton = {
                TextButton(onClick = {
                    explainReminders = false
                    if (ReminderPermission.granted(appContext)) { setRemindersEnabled(true); pickingFor = reminderFor; reminderFor = null }
                    else permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.continue_label)) }
            },
            dismissButton = { TextButton(onClick = { explainReminders = false; reminderFor = null }) { Text(stringResource(R.string.not_now)) } }
        )
    }

    pickingFor?.let { n ->
        var date by remember { mutableStateOf(true) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val timeState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { pickingFor = null },
            title = { Text(if (date) stringResource(R.string.reminder_date) else stringResource(R.string.reminder_time)) },
            text = { if (date) DatePicker(dateState, showModeToggle = false) else TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    if (date) date = false else {
                        val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis() }
                        val cal = java.util.Calendar.getInstance().apply {
                            set(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH), utc.get(java.util.Calendar.DAY_OF_MONTH), timeState.hour, timeState.minute, 0)
                        }
                        if (cal.timeInMillis <= System.currentTimeMillis()) scope.launch { snackbarHost.showSnackbar(appContext.getString(R.string.msg_future_time)) }
                        else { store.setReminder(n.id, cal.timeInMillis); pickingFor = null; save() }
                    }
                }) { Text(if (date) stringResource(R.string.next) else stringResource(R.string.set)) }
            },
            dismissButton = { TextButton(onClick = { pickingFor = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    fun deleteWithUndo(n: Note) {
        store.delete(n.id); save()
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(appContext.getString(R.string.msg_note_deleted), actionLabel = appContext.getString(R.string.undo), duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) { store.restore(n); save() }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.title_notes)) },
                actions = {
                    IconButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.semantics { contentDescription = SortOrder.description(newestFirst) }) {
                        Text(SortOrder.glyph(newestFirst), style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(
                        onClick = { layout = layout.toggled(); prefs.edit().putString(KEY_LAYOUT, layout.name).apply() },
                        modifier = Modifier.semantics { contentDescription = layout.toggleDescription() }
                    ) {
                        if (layout == LayoutMode.GRID) Icon(Icons.Filled.Menu, contentDescription = null)
                        else Text("▦", style = MaterialTheme.typography.titleLarge)
                    }
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options)) }
                        DropdownMenu(menuOpen, { menuOpen = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.back_up)) }, onClick = { menuOpen = false; exportLauncher.launch("notes-backup.json") })
                            DropdownMenuItem(text = { Text(stringResource(R.string.restore)) }, onClick = { menuOpen = false; importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addAsChecklist = false; adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_note)) }
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
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val views = listOf("Notes", "Archive", "Trash")
                val viewLabels = listOf(R.string.title_notes, R.string.archive, R.string.trash)
                views.forEachIndexed { i, v ->
                    SegmentedButton(
                        selected = view == v, onClick = { view = v },
                        shape = SegmentedButtonDefaults.itemShape(i, views.size),
                        label = { Text(stringResource(viewLabels[i])) }
                    )
                }
            }
            if (view == "Trash") {
                TextButton(onClick = { store.emptyTrash(); save() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.empty_trash)) }
                Text(stringResource(R.string.trash_note), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reminders))
                Switch(remindersOn, { on -> if (on) explainReminders = true else setRemindersEnabled(false) })
            }
            TextField(
                query, { query = it },
                placeholder = { Text(stringResource(R.string.search_notes)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Row(Modifier.edgeFade().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(colorFilter == null, { colorFilter = null }, label = { Text(stringResource(R.string.all)) })
                NoteColor.entries.forEach { c ->
                    FilterChip(
                        colorFilter == c, { colorFilter = if (colorFilter == c) null else c },
                        label = { Text(if (colorFilter == c) "✓ ${c.label}" else c.label, color = Color(c.text(dark))) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(c.background(dark)), selectedContainerColor = Color(c.background(dark)))
                    )
                }
                Spacer(Modifier.width(24.dp))
            }
            val allTags = remember(version) { store.allTags() }
            if (tagFilter != null && tagFilter !in allTags) tagFilter = null
            if (allTags.isNotEmpty()) {
                Row(Modifier.edgeFade().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.forEach { t ->
                        FilterChip(tagFilter == t, { tagFilter = if (tagFilter == t) null else t }, label = { Text(if (tagFilter == t) "✓ #$t" else "#$t") })
                    }
                    Spacer(Modifier.width(24.dp))
                }
            }
          } }
                if (notes.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        val filtered = query.isNotBlank() || colorFilter != null || tagFilter != null
                        Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = RoundedCornerShape(40.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(144.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(EmptyState.icon(view, filtered), style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(EmptyState.message(view, filtered), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            Text(EmptyState.helper(view, filtered), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            if (EmptyState.showNewNoteAction(view, filtered)) {
                                Spacer(Modifier.height(16.dp))
                                FilledTonalButton(onClick = { addAsChecklist = false; adding = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.new_note)) }
                            }
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
                                            .clickable {
                                                store.toggleItem(n.id, idx); save()
                                                store.all().firstOrNull { it.id == n.id }?.let { if (Checklist.progress(it).complete) reviewPrompt?.onChecklistCompleted(System.currentTimeMillis()) }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(ticked, onCheckedChange = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(item, textDecoration = if (ticked) TextDecoration.LineThrough else null)
                                    }
                                }
                                if (NotePreview.hiddenItems(n) > 0) Text(stringResource(R.string.more_items, NotePreview.hiddenItems(n)), style = MaterialTheme.typography.labelMedium)
                                if (Checklist.shouldCelebrate(p, animationsEnabled)) {
                                    Text(stringResource(R.string.all_done), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            } else Text(linkified(NotePreview.text(n), MaterialTheme.colorScheme.primary), maxLines = NotePreview.MAX_LINES, overflow = TextOverflow.Ellipsis)
                            if (n.createdAt > 0) {
                                Text(
                                    stringResource(R.string.created_at, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(n.createdAt))),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            n.remindAt?.let {
                                Text("⏰ " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)), style = MaterialTheme.typography.labelMedium)
                            }
                            if (n.tags.isNotEmpty()) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    n.tags.forEach { t ->
                                        val removeTagDesc = stringResource(R.string.remove_tag, t)
                                        AssistChip(
                                            onClick = { store.removeTag(n.id, t); save() },
                                            label = { Text("#$t ✕") },
                                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = removeTagDesc }
                                        )
                                    }
                                }
                            }
                            Row {
                                if (view == "Trash") {
                                    TextButton(onClick = { store.restoreFromTrash(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(stringResource(R.string.restore)) }
                                } else {
                                TextButton(onClick = { editText = n.text; editing = n }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.editDescription(n.text) }) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { store.togglePin(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.pinDescription(n.pinned, n.text) }) { Text(PinPresentation.buttonLabel(n.pinned)) }
                                TextButton(onClick = { store.setChecklist(n.id, !n.checklist); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(if (n.checklist) stringResource(R.string.plain_note) else stringResource(R.string.checklist)) }
                                TextButton(onClick = { store.setColor(n.id, NoteColor.entries.let { e -> if (n.color == null) e.first() else e.getOrNull(n.color.ordinal + 1) }); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(n.color?.let { stringResource(R.string.colour_named, it.label) } ?: stringResource(R.string.colour)) }
                                TextButton(onClick = { tagText = ""; tagging = n }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(stringResource(R.string.tag)) }
                                TextButton(
                                    onClick = { if (n.remindAt != null) { store.clearReminder(n.id); save() } else if (remindersOn) pickingFor = n else { reminderFor = n; explainReminders = true } },
                                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                                ) { Text(if (n.remindAt != null) stringResource(R.string.clear_reminder) else stringResource(R.string.remind)) }
                                TextButton(onClick = { deleteWithUndo(n) }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = PinPresentation.deleteDescription(n.text) }) { Text(stringResource(R.string.delete)) }
                                TextButton(onClick = { if (n.archived) store.unarchive(n.id) else store.archive(n.id); save() }, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) { Text(if (n.archived) stringResource(R.string.unarchive) else stringResource(R.string.archive)) }
                                }
                            }
                        }
                    }
                }
            }
    }
}

/** Fades the trailing edge of a horizontally scrolling row so clipped chips read as scrollable. */
private fun Modifier.edgeFade(width: Float = 32f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.dp.toPx().coerceAtMost(size.width)
        drawRect(
            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - w, endX = size.width),
            topLeft = androidx.compose.ui.geometry.Offset(size.width - w, 0f),
            size = androidx.compose.ui.geometry.Size(w, size.height),
            blendMode = BlendMode.DstIn
        )
    }

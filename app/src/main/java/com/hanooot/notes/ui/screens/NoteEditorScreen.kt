package com.hanooot.notes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hanooot.notes.NotesViewModel
import com.hanooot.notes.audio.TaskExtractor
import com.hanooot.notes.calendar.CalendarExport
import com.hanooot.notes.data.*
import com.hanooot.notes.ui.components.ChecklistRow
import com.hanooot.notes.ui.components.DateRow
import com.hanooot.notes.ui.components.MemoRow
import com.hanooot.notes.ui.components.RecordingPanel
import com.hanooot.notes.ui.components.ReminderMenu
import com.hanooot.notes.ui.theme.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: NotesViewModel,
    initial: Note,
    categories: List<Category>,
    onClose: () -> Unit,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current

    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var content by remember(initial.id) { mutableStateOf(initial.content) }
    var checklist by remember(initial.id) { mutableStateOf(initial.checklist) }
    var memos by remember(initial.id) { mutableStateOf(initial.memos) }
    var categoryKey by remember(initial.id) { mutableStateOf(initial.categoryKey) }
    var pinned by remember(initial.id) { mutableStateOf(initial.pinned) }
    var dueAt by remember(initial.id) { mutableStateOf(initial.dueAt) }
    var remindMins by remember(initial.id) { mutableStateOf(initial.remindMinutesBefore) }

    var categoryMenuOpen by remember { mutableStateOf(false) }

    // ---- Recording state ----
    var recording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var level by remember { mutableStateOf(0f) }
    var liveFinal by remember { mutableStateOf("") }
    var livePartial by remember { mutableStateOf("") }

    fun currentNote(): Note = initial.copy(
        title = title.ifBlank {
            content.take(30).ifBlank {
                checklist.firstOrNull()?.text?.take(30)
                    ?: if (memos.isNotEmpty()) "Voice memo" else ""
            }
        },
        content = content,
        checklist = checklist,
        memos = memos,
        categoryKey = categoryKey,
        pinned = pinned,
        dueAt = dueAt,
        remindMinutesBefore = if (dueAt == null) null else remindMins
    )

    // ---- Autosave ----
    // Saving only on back meant anything typed was lost if Android killed the
    // app first (low memory, swiped from recents, restart). Edits are now
    // debounced and written as you go.
    var savedId by remember(initial.id) { mutableStateOf(initial.id) }

    fun persist(onDone: (Note) -> Unit = {}) {
        val note = currentNote().copy(id = savedId)
        val empty = note.title.isBlank() && note.content.isBlank() &&
                note.checklist.isEmpty() && note.memos.isEmpty()
        if (empty) return
        vm.save(note) { saved ->
            savedId = saved.id
            // The repository may have moved the note into or out of Done;
            // mirror that so the next save doesn't fight it.
            categoryKey = saved.categoryKey
            onDone(saved)
        }
    }

    LaunchedEffect(title, content, checklist, memos, categoryKey, pinned, dueAt, remindMins) {
        delay(700)          // don't write on every keystroke
        persist()
    }

    // Catches the case the debounce can't: the app going to the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) persist()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun saveAndClose() {
        if (recording) {
            recorder_stop(vm) { file, dur, text ->
                memos = memos + Memo(
                    id = "memo_${System.currentTimeMillis()}",
                    path = file.absolutePath,
                    durationMs = dur,
                    createdAt = System.currentTimeMillis(),
                    transcript = text
                )
            }
            recording = false
        }
        persist()
        onClose()
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording(vm) { recording = true }
        } else {
            vm.post("Microphone blocked", "Enable it in Settings to record memos")
        }
    }

    // Drive the timer and level meter while recording
    LaunchedEffect(recording) {
        while (recording) {
            elapsedMs = vm.recorder.elapsedMs()
            level = vm.recorder.amplitude()
            delay(80)
        }
    }
    DisposableEffect(Unit) {
        vm.transcriber.onUpdate = { f, p -> liveFinal = f; livePartial = p }
        onDispose { vm.transcriber.onUpdate = null }
    }

    val category = categories.firstOrNull { it.key == categoryKey }
    val accent = categoryColor(category)

    Column(modifier.fillMaxSize().background(Bg)) {

        // ---- Header ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { saveAndClose() }) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.weight(1f))

            IconButton(onClick = { pinned = !pinned }) {
                Icon(
                    if (pinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    "Pin",
                    tint = if (pinned) theme.accent else TextSecondary
                )
            }
            IconButton(onClick = { onDelete(currentNote()) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = TextSecondary)
            }

            Box {
                Row(
                    Modifier.clickable { categoryMenuOpen = true }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (category?.label ?: "").uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Icon(Icons.Filled.ArrowDropDown, null, tint = accent)
                }
                DropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false },
                    modifier = Modifier.background(Panel)
                ) {
                    categories.filter { !it.system }.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.label, color = TextPrimary) },
                            leadingIcon = {
                                Icon(iconFor(c.iconName), null, tint = categoryColor(c))
                            },
                            onClick = { categoryKey = c.key; categoryMenuOpen = false }
                        )
                    }
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ---- Title ----
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(theme.accent),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("Title", color = TextMuted,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    inner()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // ---- Checklist ----
            checklist.forEachIndexed { index, item ->
                ChecklistRow(
                    item = item,
                    accent = theme.accent,
                    onToggle = {
                        checklist = checklist.toMutableList().also {
                            it[index] = item.copy(done = !item.done)
                        }
                    },
                    onTextChange = { newText ->
                        checklist = checklist.toMutableList().also {
                            it[index] = item.copy(text = newText)
                        }
                    },
                    onRemove = {
                        checklist = checklist.toMutableList().also { it.removeAt(index) }
                    }
                )
            }
            TextButton(onClick = {
                checklist = checklist + ChecklistItem("")
            }) {
                Text("+ Add item", color = TextSecondary, fontSize = 14.sp)
            }

            // ---- Body ----
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(color = TextSecondary, fontSize = 16.sp),
                cursorBrush = SolidColor(theme.accent),
                decorationBox = { inner ->
                    if (content.isEmpty()) Text("Write more…", color = TextMuted, fontSize = 16.sp)
                    inner()
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 90.dp)
                    .padding(vertical = 8.dp)
            )

            // ---- Date & reminder ----
            DateRow(
                dueAt = dueAt,
                onPick = { dueAt = it; if (remindMins == null) remindMins = 15 },
                onClear = { dueAt = null; remindMins = null }
            )
            if (dueAt != null) {
                ReminderMenu(
                    selected = remindMins,
                    onSelect = { remindMins = it }
                )
                OutlinedButton(
                    onClick = { CalendarExport.addToCalendar(context, currentNote()) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    border = BorderStroke(1.dp, CardStroke)
                ) {
                    Icon(Icons.Filled.Event, null, tint = TextSecondary,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add to calendar", color = TextSecondary, fontSize = 13.sp)
                }
                TextButton(
                    onClick = { CalendarExport.shareIcs(context, currentNote()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share as .ics", color = TextMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Voice memos ----
            if (recording) {
                RecordingPanel(
                    elapsedMs = elapsedMs,
                    level = level,
                    transcript = (liveFinal + livePartial).trim(),
                    onStop = {
                        recorder_stop(vm) { file, dur, text ->
                            memos = memos + Memo(
                                id = "memo_${System.currentTimeMillis()}",
                                path = file.absolutePath,
                                durationMs = dur,
                                createdAt = System.currentTimeMillis(),
                                transcript = text
                            )
                        }
                        recording = false
                        liveFinal = ""; livePartial = ""
                    }
                )
            } else {
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) startRecording(vm) { recording = true }
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CardStroke)
                ) {
                    Icon(Icons.Filled.Mic, null, tint = TextSecondary,
                        modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Record voice memo", color = TextSecondary, fontSize = 14.sp)
                }
            }

            memos.forEach { memo ->
                MemoRow(
                    memo = memo,
                    accent = theme.accent,
                    onDelete = {
                        vm.deleteMemoFile(memo)
                        memos = memos.filterNot { it.id == memo.id }
                    },
                    onTranscriptChange = { fixed ->
                        memos = memos.map {
                            if (it.id == memo.id) it.copy(transcript = fixed) else it
                        }
                    },
                    onAddTasks = {
                        val found = TaskExtractor.extract(memo.transcript)
                        val existing = checklist.map { it.text.trim().lowercase() }.toSet()
                        val fresh = found.filter { it.trim().lowercase() !in existing }
                        if (fresh.isEmpty()) {
                            vm.post("Already added", "Those tasks are in the list")
                        } else {
                            checklist = checklist.filter { it.text.isNotBlank() } +
                                    fresh.map { ChecklistItem(it) }
                            vm.post(
                                "${fresh.size} task${if (fresh.size == 1) "" else "s"} added",
                                "From your voice memo"
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun startRecording(vm: NotesViewModel, onStarted: () -> Unit) {
    runCatching {
        vm.recorder.start(vm.memoDir())
        if (vm.transcriber.isAvailable()) vm.transcriber.start()
        onStarted()
    }.onFailure {
        vm.post("Couldn't start recording", it.message ?: "")
    }
}

private fun recorder_stop(
    vm: NotesViewModel,
    onSaved: (java.io.File, Long, String) -> Unit
) {
    val text = vm.transcriber.stop()
    val result = vm.recorder.stop()
    if (result != null) onSaved(result.first, result.second, text)
}

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
import com.hanooot.notes.ui.components.MemoRow
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

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    accent: Color,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    // Only the checkbox animates; the row itself stays put so ticking an item
    // never looks like the whole list reloaded.
    val scale by animateFloatAsState(
        targetValue = if (item.done) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (item.done) DoneGreen else Color.Transparent)
                .border(
                    2.dp,
                    if (item.done) DoneGreen else TextSecondary,
                    CircleShape
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (item.done) {
                Icon(
                    Icons.Filled.Check, null, tint = Color.White,
                    modifier = Modifier.size(14.dp * scale)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                color = if (item.done) TextMuted else TextPrimary,
                fontSize = 16.sp,
                textDecoration = if (item.done) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                if (item.text.isEmpty()) Text("List item", color = TextMuted, fontSize = 16.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, "Remove", tint = TextMuted,
                modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun DateRow(
    dueAt: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
            .clickable {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dueAt ?: System.currentTimeMillis()
                    if (dueAt == null) { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
                }
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        cal.set(y, m, d)
                        android.app.TimePickerDialog(
                            context,
                            { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h)
                                cal.set(Calendar.MINUTE, min)
                                cal.set(Calendar.SECOND, 0)
                                onPick(cal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
                        ).show()
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Event, null, tint = TextSecondary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            dueAt?.let { fmt.format(Date(it)) } ?: "Add date & reminder",
            color = if (dueAt == null) TextSecondary else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (dueAt != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.Close, "Remove date", tint = TextMuted,
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun RecordingPanel(
    elapsedMs: Long,
    level: Float,
    transcript: String,
    onStop: () -> Unit
) {
    val red = Color(0xFFFF3B30)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, red.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .background(red.copy(alpha = 0.07f))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
                initialValue = 1f, targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "dot"
            )
            Box(Modifier.size(10.dp).clip(CircleShape)
                .background(red.copy(alpha = pulse)))
            Spacer(Modifier.width(12.dp))
            Text(
                formatElapsed(elapsedMs),
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(12.dp))

            // Simple live level meter driven by the recorder's amplitude
            Row(
                Modifier.weight(1f).height(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(20) { i ->
                    val h = (0.15f + level * (0.4f + 0.6f * ((i % 5) / 5f))).coerceIn(0.12f, 1f)
                    Box(
                        Modifier.weight(1f).padding(horizontal = 1.dp)
                            .fillMaxHeight(h)
                            .background(LocalAppTheme.current.accent)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(red)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(11.dp).background(Color.White))
            }
        }

        if (transcript.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Text("TRANSCRIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(transcript, fontSize = 14.sp, color = TextPrimary)
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

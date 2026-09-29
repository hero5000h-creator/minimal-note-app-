package com.hanooot.notes

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import com.hanooot.notes.data.Category
import com.hanooot.notes.data.Note
import com.hanooot.notes.reminder.ReminderNotifications
import com.hanooot.notes.ui.components.NewCategoryDialog
import com.hanooot.notes.ui.components.ThemeSheet
import com.hanooot.notes.ui.screens.HomeScreen
import com.hanooot.notes.ui.screens.NoteEditorScreen
import com.hanooot.notes.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
    }

    private val vm: NotesViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ReminderNotifications.ensureChannel(this)

        val openNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L) ?: -1L

        setContent {
            val theme by vm.theme.collectAsState()

            NotesTheme(theme) {
                val notes by vm.notes.collectAsState()
                val categories by vm.categories.collectAsState()
                val message by vm.messages.collectAsState()

                var editing by remember { mutableStateOf<Note?>(null) }
                var showCalendar by remember { mutableStateOf(false) }
                var showThemes by remember { mutableStateOf(false) }
                var showNewCategory by remember { mutableStateOf(false) }

                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Ask for notification permission once, on Android 13+
                val notifPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Opened from a reminder notification
                LaunchedEffect(openNoteId, notes) {
                    if (openNoteId > 0 && editing == null) {
                        vm.noteById(openNoteId)?.let { editing = it }
                    }
                }

                LaunchedEffect(message) {
                    message?.let {
                        snackbar.showSnackbar(
                            if (it.detail.isBlank()) it.text else "${it.text} · ${it.detail}"
                        )
                        vm.clearMessage()
                    }
                }

                val widthClass = calculateWindowSizeClass(this).widthSizeClass
                // Expanded means a tablet or an unfolded foldable: show the note
                // beside the list instead of covering it.
                val twoPane = widthClass == WindowWidthSizeClass.Expanded

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    containerColor = Bg
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (twoPane) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(0.42f)) {
                                    HomeScreen(
                                        notes = notes,
                                        categories = categories,
                                        onOpenNote = { editing = it },
                                        onNewNote = { editing = Note() },
                                        onAddCategory = { showNewCategory = true },
                                        onDeleteCategory = { vm.deleteCategory(it) },
                                        onOpenThemes = { showThemes = true },
                                        showCalendar = showCalendar,
                                        onToggleCalendar = { showCalendar = it }
                                    )
                                }
                                VerticalDivider(color = CardStroke)
                                Box(Modifier.weight(0.58f)) {
                                    val current = editing
                                    if (current == null) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                "Select a note, or tap + to write a new one",
                                                color = TextMuted, fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(24.dp)
                                            )
                                        }
                                    } else {
                                        NoteEditorScreen(
                                            vm = vm,
                                            initial = current,
                                            categories = categories,
                                            onClose = { editing = null },
                                            onDelete = { vm.delete(it); editing = null }
                                        )
                                    }
                                }
                            }
                        } else {
                            HomeScreen(
                                notes = notes,
                                categories = categories,
                                onOpenNote = { editing = it },
                                onNewNote = { editing = Note() },
                                onAddCategory = { showNewCategory = true },
                                onDeleteCategory = { vm.deleteCategory(it) },
                                onOpenThemes = { showThemes = true },
                                showCalendar = showCalendar,
                                onToggleCalendar = { showCalendar = it }
                            )
                            // Full-screen editor sits over the list on phones.
                            // Surface consumes touches so taps can't fall
                            // through to the list underneath.
                            editing?.let { current ->
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Bg
                                ) {
                                    NoteEditorScreen(
                                        vm = vm,
                                        initial = current,
                                        categories = categories,
                                        onClose = { editing = null },
                                        onDelete = { vm.delete(it); editing = null }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showThemes) {
                    ThemeSheet(
                        current = theme,
                        onSelect = { vm.setTheme(it.key); showThemes = false },
                        onDismiss = { showThemes = false }
                    )
                }
                if (showNewCategory) {
                    NewCategoryDialog(
                        existing = categories,
                        onCreate = { label, color, icon ->
                            vm.addCategory(label, color, icon)
                            showNewCategory = false
                        },
                        onDismiss = { showNewCategory = false }
                    )
                }
            }
        }
    }
}

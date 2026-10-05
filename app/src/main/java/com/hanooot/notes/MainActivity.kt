package com.hanooot.notes

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.hanooot.notes.cloud.BackupManager
import com.hanooot.notes.cloud.SigningInfo
import com.hanooot.notes.ui.components.BackupDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat
import com.hanooot.notes.data.Category
import com.hanooot.notes.data.Note
import com.hanooot.notes.reminder.ReminderNotifications
import com.hanooot.notes.ui.components.NewCategoryDialog
import com.hanooot.notes.ui.components.ThemeSheet
import com.hanooot.notes.ui.screens.HomeScreen
import com.hanooot.notes.ui.screens.NoteEditorScreen
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.i18n.stringsFor
import com.hanooot.notes.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
    }

    private val vm: NotesViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ReminderNotifications.ensureChannel(this)

        val openNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L) ?: -1L

        setContent {
            val theme by vm.theme.collectAsState()
            val lang by vm.lang.collectAsState()
            val strings = remember(lang) { stringsFor(lang) }

            // Arabic flips the whole layout: every Row, every start/end
            // padding and every alignment follows the layout direction, so
            // the interface mirrors without a second set of layouts.
            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLayoutDirection provides
                        if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
            NotesTheme(theme) {
                val s = strings
                val notes by vm.notes.collectAsState()
                val categories by vm.categories.collectAsState()
                val message by vm.messages.collectAsState()

                var editing by remember { mutableStateOf<Note?>(null) }
                var showCalendar by remember { mutableStateOf(false) }
                var showThemes by remember { mutableStateOf(false) }
                var showBackup by remember { mutableStateOf(false) }
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

                val account by vm.account.collectAsState()
                val backupBusy by vm.backupBusy.collectAsState()
                val lastBackupAt by vm.lastBackupAt.collectAsState()

                // Google's sheet returns through an activity result; the
                // account itself stays with Play Services, not with the app.
                val signInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        vm.onSignedIn(task.getResult(ApiException::class.java)?.email)
                    } catch (e: ApiException) {
                        // Code 12501 is the user backing out, which is not an error.
                        if (e.statusCode != 12501) {
                            // 10 is DEVELOPER_ERROR: the package name and
                            // fingerprint do not match a registered client.
                            val detail = if (e.statusCode == 10) s.developerError
                                         else "code ${e.statusCode}"
                            vm.post(s.signInFailed, detail)
                        }
                    }
                }

                // Granting Drive access is a second consent after sign-in, so
                // the prompt Play Services hands back is shown rather than
                // dropped — otherwise it loops back to a sign-in that worked.
                val pendingConsent by vm.pendingConsent.collectAsState()
                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    vm.consentHandled()
                    if (result.resultCode == RESULT_OK) vm.backUpNow()
                }
                LaunchedEffect(pendingConsent) {
                    pendingConsent?.let { consentLauncher.launch(it) }
                }

                // Leaving the app is the natural moment to back up: the note
                // just written is finished, and nothing is waiting on it.
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) vm.backUpIfDue()
                    }
                    owner.lifecycle.addObserver(observer)
                    onDispose { owner.lifecycle.removeObserver(observer) }
                }

                // The notification channel carries a user-visible name, so it
                // is rewritten whenever the language changes.
                LaunchedEffect(lang) { ReminderNotifications.ensureChannel(this@MainActivity) }

                // Opened from a reminder notification
                LaunchedEffect(openNoteId, notes) {
                    if (openNoteId > 0 && editing == null) {
                        vm.noteById(openNoteId)?.let { editing = it }
                    }
                }

                LaunchedEffect(message) {
                    message?.let { msg ->
                        val result = snackbar.showSnackbar(
                            message = if (msg.detail.isBlank()) msg.text
                                      else "${msg.text} · ${msg.detail}",
                            actionLabel = msg.actionLabel,
                            withDismissAction = msg.actionLabel == null,
                            duration = if (msg.actionLabel != null) SnackbarDuration.Long
                                       else SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
                        vm.clearMessage()
                    }
                }

                // Android's back button must close what is open, not the
                // app. Without this the system default finishes the activity.
                BackHandler(enabled = showBackup) { showBackup = false }
                BackHandler(enabled = showThemes && !showBackup) { showThemes = false }
                BackHandler(enabled = showNewCategory && !showThemes && !showBackup) {
                    showNewCategory = false
                }
                BackHandler(
                    enabled = editing != null && !showThemes && !showNewCategory && !showBackup
                ) { editing = null }
                BackHandler(
                    enabled = showCalendar && editing == null &&
                            !showThemes && !showNewCategory && !showBackup
                ) { showCalendar = false }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    containerColor = Bg
                ) { padding ->
                    // BoxWithConstraints measures the real window width, so the
                    // layout adapts without the experimental WindowSizeClass API
                    // or its extra dependency.
                    BoxWithConstraints(Modifier.padding(padding)) {
                        // Wide enough for the note to sit beside the list:
                        // a tablet, or an unfolded foldable.
                        val twoPane = maxWidth >= 840.dp
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
                                        onOpenBackup = { showBackup = true },
                                        backupActive = account != null,
                                        showCalendar = showCalendar,
                                        onToggleCalendar = { showCalendar = it }
                                    )
                                }
                                Box(
                                    Modifier.fillMaxHeight().width(1.dp)
                                        .background(CardStroke)
                                )
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
                                onOpenBackup = { showBackup = true },
                                backupActive = account != null,
                                showCalendar = showCalendar,
                                onToggleCalendar = { showCalendar = it }
                            )
                            // Full-screen editor slides in over the list on
                            // phones. Surface consumes touches so taps can't
                            // fall through to the list underneath.
                            // Offsets are raw pixels, not direction-aware, so
                            // the sign is flipped by hand for Arabic.
                            val slide = if (lang.rtl) -1 else 1
                            AnimatedVisibility(
                                visible = editing != null,
                                enter = slideInHorizontally(
                                    initialOffsetX = { it * slide },
                                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(200)),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { it * slide },
                                    animationSpec = tween(260, easing = FastOutLinearInEasing)
                                ) + fadeOut(tween(180))
                            ) {
                                // Hold the last note while the exit animation
                                // plays, otherwise the panel empties mid-slide.
                                val shown = remember { mutableStateOf(editing) }
                                editing?.let { shown.value = it }

                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Bg
                                ) {
                                    shown.value?.let { current ->
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
                }

                if (showThemes) {
                    ThemeSheet(
                        current = theme,
                        currentLang = lang,
                        onSelect = { vm.setTheme(it.key); showThemes = false },
                        onSelectLang = { vm.setLanguage(it) },
                        onDismiss = { showThemes = false }
                    )
                }
                if (showBackup) {
                    val lastLabel = remember(lastBackupAt, lang) {
                        if (lastBackupAt == 0L) strings.neverBackedUp
                        else strings.lastBackup(
                            java.text.SimpleDateFormat(
                                strings.duePattern, strings.locale
                            ).format(java.util.Date(lastBackupAt))
                        )
                    }
                    BackupDialog(
                        email = account,
                        lastBackupLabel = lastLabel,
                        busy = backupBusy,
                        packageName = SigningInfo.packageName(this@MainActivity),
                        fingerprint = SigningInfo.sha1(this@MainActivity),
                        onSignIn = {
                            signInLauncher.launch(
                                BackupManager.client(this@MainActivity).signInIntent
                            )
                        },
                        onSignOut = {
                            BackupManager.client(this@MainActivity).signOut()
                            vm.onSignedOut()
                            showBackup = false
                        },
                        onBackUp = { vm.backUpNow() },
                        onRestore = { vm.restoreFromDrive() },
                        onDismiss = { showBackup = false }
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
}

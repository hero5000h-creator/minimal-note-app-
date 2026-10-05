package com.hanooot.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hanooot.notes.audio.AudioRecorder
import com.hanooot.notes.audio.Transcriber
import com.hanooot.notes.cloud.BackupManager
import com.hanooot.notes.data.*
import com.hanooot.notes.reminder.ReminderScheduler
import com.hanooot.notes.ui.i18n.AppStrings
import com.hanooot.notes.ui.i18n.Lang
import com.hanooot.notes.ui.i18n.labelOf
import com.hanooot.notes.ui.i18n.stringsFor
import com.hanooot.notes.ui.theme.themeByKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/** A transient message shown in a snackbar, optionally with one action. */
data class UiMessage(
    val id: Long,
    val text: String,
    val detail: String = "",
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NotesRepository(app)
    private val settings = Settings(app)
    private val backups = BackupManager(app, repo)

    val recorder = AudioRecorder(app)
    val transcriber = Transcriber(app)

    val notes: StateFlow<List<Note>> = repo.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theme = settings.theme
        .map { themeByKey(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, themeByKey("red"))

    val lang = settings.language
        .map { Lang.of(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Lang.EN)

    private val _messages = MutableStateFlow<UiMessage?>(null)
    val messages: StateFlow<UiMessage?> = _messages

    // ---- Backup ----

    /** The signed-in address, or null. Play Services is the source of truth. */
    private val _account = MutableStateFlow(BackupManager.account(app)?.email)
    val account: StateFlow<String?> = _account

    val lastBackupAt = settings.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Non-null while a backup or restore is running, holding its label. */
    private val _backupBusy = MutableStateFlow<String?>(null)
    val backupBusy: StateFlow<String?> = _backupBusy

    /**
     * A prompt Play Services wants shown before Drive will answer — granting
     * the Drive scope is a separate consent from signing in. The activity
     * launches it and clears it.
     */
    private val _pendingConsent = MutableStateFlow<android.content.Intent?>(null)
    val pendingConsent: StateFlow<android.content.Intent?> = _pendingConsent

    fun consentHandled() { _pendingConsent.value = null }

    init {
        viewModelScope.launch {
            repo.seedDefaults()
            // Clear audio left behind by deletes that were never undone.
            repo.purgeOrphanMemos()
            // Alarms may have been dropped while the app was uninstalled from
            // memory; make sure everything pending is scheduled.
            ReminderScheduler.rescheduleAll(getApplication(), repo.allNotes())
        }
        // Notification text is built outside composition, so the scheduler and
        // the receiver read the table from here.
        viewModelScope.launch {
            settings.language.collect { AppStrings.current = stringsFor(Lang.of(it)) }
        }
    }

    fun setTheme(key: String) = viewModelScope.launch { settings.setTheme(key) }

    fun setLanguage(lang: Lang) = viewModelScope.launch { settings.setLanguage(lang.key) }

    suspend fun noteById(id: Long): Note? = repo.noteById(id)

    fun save(note: Note, onSaved: (Note) -> Unit = {}) {
        viewModelScope.launch {
            val result = repo.save(note)
            val ctx = getApplication<Application>()

            // Reminder state may have changed in either direction
            ReminderScheduler.cancel(ctx, result.note)
            ReminderScheduler.schedule(ctx, result.note)

            val s = AppStrings.current
            when (result.move) {
                DoneMove.MOVED_TO_DONE ->
                    post(result.note.title, s.movedToDone)
                DoneMove.MOVED_BACK -> {
                    val category = categories.value
                        .firstOrNull { it.key == result.note.categoryKey }
                    val label = if (category == null) s.itsCategory else s.labelOf(category)
                    post(result.note.title, s.reopenedInto(label))
                }
                DoneMove.NONE -> Unit
            }
            onSaved(result.note)
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), note)
            repo.delete(note)
            // Audio stays on disk until the undo window has passed, so the
            // note can come back whole.
            val s = AppStrings.current
            _messages.value = UiMessage(
                id = System.currentTimeMillis(),
                text = s.deletedNote(note.title.ifBlank { s.untitled }),
                actionLabel = s.undo,
                onAction = { restore(note) }
            )
        }
    }

    private fun restore(note: Note) {
        viewModelScope.launch {
            repo.restore(note)
            ReminderScheduler.schedule(getApplication(), note)
        }
    }

    fun addCategory(label: String, color: Int, icon: String, onDone: (Category) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.addCategory(label, color, icon)) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repo.deleteCategory(category) }
    }

    fun memoDir(): File = repo.memoDir()

    fun deleteMemoFile(memo: Memo) {
        viewModelScope.launch { repo.deleteMemoFile(memo) }
    }

    /** Called after the sign-in sheet closes, with the address or null. */
    fun onSignedIn(email: String?) {
        _account.value = email
        if (email != null) backUpNow()
    }

    fun onSignedOut() {
        _account.value = null
    }

    fun backUpNow() {
        if (_backupBusy.value != null) return
        val s = AppStrings.current
        _backupBusy.value = s.backingUp
        viewModelScope.launch {
            val result = backups.backUp()
            _backupBusy.value = null
            when (result) {
                is BackupManager.Result.Ok -> {
                    settings.setLastBackupAt(System.currentTimeMillis())
                    post(s.backupDone(result.notes), s.audioUploaded(result.memos))
                }
                is BackupManager.Result.NeedsSignIn -> onNeedsConsent(result.intent)
                is BackupManager.Result.Failed -> post(s.backupFailed, result.reason)
                BackupManager.Result.NoBackup -> post(s.noBackupFound)
            }
        }
    }

    /**
     * Runs when the app goes to the background, so a day's notes are never
     * more than one backup behind. Throttled: leaving and re-entering the app
     * repeatedly should not mean an upload each time.
     */
    fun backUpIfDue() {
        if (_account.value == null || _backupBusy.value != null) return
        val age = System.currentTimeMillis() - lastBackupAt.value
        if (age < 15 * 60_000L) return
        backUpNow()
    }

    /**
     * Consent can be recoverable or not. With a prompt to show, the account
     * stays — it is still signed in, it just has not granted Drive yet.
     */
    private fun onNeedsConsent(intent: android.content.Intent?) {
        val s = AppStrings.current
        if (intent != null) {
            _pendingConsent.value = intent
            post(s.grantDriveAccess)
        } else {
            _account.value = null
            post(s.signInNeeded)
        }
    }

    fun restoreFromDrive() {
        if (_backupBusy.value != null) return
        val s = AppStrings.current
        _backupBusy.value = s.restoring
        viewModelScope.launch {
            val result = backups.restore()
            _backupBusy.value = null
            when (result) {
                is BackupManager.Result.Ok -> {
                    // Reminders were rebuilt from scratch, so the alarms that
                    // belong to the restored notes have to be re-armed.
                    ReminderScheduler.rescheduleAll(getApplication(), repo.allNotes())
                    post(s.restoreDone(result.notes))
                }
                BackupManager.Result.NoBackup -> post(s.noBackupFound)
                is BackupManager.Result.NeedsSignIn -> onNeedsConsent(result.intent)
                is BackupManager.Result.Failed -> post(s.backupFailed, result.reason)
            }
        }
    }

    fun post(text: String, detail: String = "") {
        _messages.value = UiMessage(System.currentTimeMillis(), text, detail)
    }

    fun clearMessage() { _messages.value = null }

    override fun onCleared() {
        super.onCleared()
        recorder.cancel()
        transcriber.stop()
    }
}

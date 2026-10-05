package com.hanooot.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hanooot.notes.audio.AudioRecorder
import com.hanooot.notes.audio.Transcriber
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

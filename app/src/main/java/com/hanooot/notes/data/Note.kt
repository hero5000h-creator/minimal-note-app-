package com.hanooot.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One item in a note's checklist. */
data class ChecklistItem(
    val text: String,
    val done: Boolean = false
)

/**
 * A voice memo. [path] points at a file inside the app's own storage, so the
 * recording survives restarts — unlike the browser prototype, where audio only
 * lived in memory.
 */
data class Memo(
    val id: String,
    val path: String,
    val durationMs: Long,
    val createdAt: Long,
    val transcript: String = ""
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    val memos: List<Memo> = emptyList(),
    val categoryKey: String = Category.WORK_KEY,
    /** Where the note came from before it was auto-filed into Done. */
    val previousCategoryKey: String? = null,
    val pinned: Boolean = false,
    /** Scheduled date/time in epoch millis, or null if unscheduled. */
    val dueAt: Long? = null,
    /** Minutes before [dueAt] to fire a reminder; null means no reminder. */
    val remindMinutesBefore: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = checklist.isNotEmpty() && checklist.all { it.done }

    /** Search looks at the title, the body and every checklist item. */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return title.contains(q, ignoreCase = true) ||
                content.contains(q, ignoreCase = true) ||
                checklist.any { it.text.contains(q, ignoreCase = true) } ||
                memos.any { it.transcript.contains(q, ignoreCase = true) }
    }

    val reminderAt: Long?
        get() {
            val due = dueAt ?: return null
            val mins = remindMinutesBefore ?: return null
            return due - mins * 60_000L
        }
}

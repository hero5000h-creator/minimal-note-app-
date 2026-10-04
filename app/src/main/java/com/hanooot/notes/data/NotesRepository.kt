package com.hanooot.notes.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Outcome of an automatic move into or out of the Done category. */
enum class DoneMove { NONE, MOVED_TO_DONE, MOVED_BACK }

data class SaveResult(val note: Note, val move: DoneMove)

class NotesRepository(context: Context) {

    private val db = NotesDatabase.get(context)
    private val notes = db.noteDao()
    private val categories = db.categoryDao()
    private val appContext = context.applicationContext

    fun observeNotes(): Flow<List<Note>> = notes.observeAll()
    fun observeCategories(): Flow<List<Category>> = categories.observeAll()

    suspend fun seedDefaults() {
        categories.insertAll(Category.defaults())
    }

    suspend fun noteById(id: Long): Note? = notes.byId(id)
    suspend fun allNotes(): List<Note> = notes.all()

    /**
     * Applies the Done rule, then persists.
     *
     * A note whose checklist is fully ticked moves to Done, remembering where
     * it came from. Unticking anything sends it back to that original category
     * rather than a default, so completing a note is never a one-way trip.
     */
    suspend fun save(note: Note): SaveResult {
        var resolved = note
        var move = DoneMove.NONE

        val complete = note.isComplete
        if (complete && note.categoryKey != Category.DONE_KEY) {
            resolved = note.copy(
                previousCategoryKey = note.categoryKey,
                categoryKey = Category.DONE_KEY
            )
            move = DoneMove.MOVED_TO_DONE
        } else if (!complete && note.categoryKey == Category.DONE_KEY) {
            val back = note.previousCategoryKey
                ?: categories.all().firstOrNull { !it.system }?.key
                ?: Category.WORK_KEY
            resolved = note.copy(categoryKey = back, previousCategoryKey = null)
            move = DoneMove.MOVED_BACK
        }

        resolved = resolved.copy(updatedAt = System.currentTimeMillis())

        val saved = if (resolved.id == 0L) {
            val newId = notes.insert(resolved)
            resolved.copy(id = newId)
        } else {
            notes.update(resolved)
            resolved
        }
        return SaveResult(saved, move)
    }

    /**
     * Removes the row but leaves the audio on disk, so a delete can be undone.
     * Files that end up with no note pointing at them are cleared by
     * [purgeOrphanMemos] on the next launch.
     */
    suspend fun delete(note: Note) {
        notes.delete(note)
    }

    /** Puts a deleted note back, keeping its original id. */
    suspend fun restore(note: Note) {
        notes.insert(note)
    }

    /** Deletes memo files no surviving note references. */
    suspend fun purgeOrphanMemos() {
        val referenced = notes.all().flatMap { it.memos }.map { it.path }.toHashSet()
        runCatching {
            memoDir().listFiles()?.forEach { file ->
                if (file.absolutePath !in referenced) file.delete()
            }
        }
    }

    suspend fun deleteMemoFile(memo: Memo) {
        runCatching { File(memo.path).takeIf { it.exists() }?.delete() }
    }

    suspend fun addCategory(label: String, color: Int, iconName: String): Category {
        val key = "CUSTOM_" + System.currentTimeMillis()
        val cat = Category(
            key = key,
            label = label,
            color = color,
            iconName = iconName,
            // Keep Done last in the tab strip
            sortOrder = categories.maxSortOrder() + 1,
            builtIn = false
        )
        categories.insert(cat)
        return cat
    }

    suspend fun deleteCategory(category: Category) {
        if (category.builtIn || category.system) return
        val fallback = categories.all().firstOrNull { !it.system && it.key != category.key }?.key
            ?: Category.WORK_KEY
        notes.reassignCategory(category.key, fallback)
        categories.delete(category)
    }

    /** Where new recordings are written; private to the app. */
    fun memoDir(): File = File(appContext.filesDir, "memos").apply { mkdirs() }
}

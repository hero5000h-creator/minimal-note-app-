package com.hanooot.notes.cloud

import com.hanooot.notes.data.Category
import com.hanooot.notes.data.ChecklistItem
import com.hanooot.notes.data.Memo
import com.hanooot.notes.data.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * The backup file's shape.
 *
 * Written by hand with org.json for the same reason the Room converters are:
 * no serialization plugin, and an explicit format that can be read and fixed
 * by eye if a restore ever goes wrong.
 *
 * Audio lives in separate files beside this one — a recording is megabytes,
 * and inlining it would make the backup unreadable and slow to write. The
 * memo's `file` field names the audio file to look for.
 */
object BackupPayload {

    const val FILE_NAME = "notes-backup.json"
    const val MIME = "application/json"

    /** Bumped only if the format changes in a way a reader must notice. */
    private const val VERSION = 1

    fun encode(notes: List<Note>, categories: List<Category>): ByteArray {
        val root = JSONObject()
            .put("version", VERSION)
            .put("savedAt", System.currentTimeMillis())
            .put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(
                        JSONObject()
                            .put("key", c.key)
                            .put("label", c.label)
                            .put("color", c.color)
                            .put("iconName", c.iconName)
                            .put("sortOrder", c.sortOrder)
                            .put("builtIn", c.builtIn)
                            .put("system", c.system)
                    )
                }
            })
            .put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(
                        JSONObject()
                            .put("id", n.id)
                            .put("title", n.title)
                            .put("content", n.content)
                            .put("categoryKey", n.categoryKey)
                            .put("previousCategoryKey", n.previousCategoryKey ?: JSONObject.NULL)
                            .put("pinned", n.pinned)
                            .put("dueAt", n.dueAt ?: JSONObject.NULL)
                            .put("remindMinutesBefore", n.remindMinutesBefore ?: JSONObject.NULL)
                            .put("updatedAt", n.updatedAt)
                            .put("checklist", JSONArray().apply {
                                n.checklist.forEach { item ->
                                    put(
                                        JSONObject()
                                            .put("text", item.text)
                                            .put("done", item.done)
                                    )
                                }
                            })
                            .put("memos", JSONArray().apply {
                                n.memos.forEach { m ->
                                    put(
                                        JSONObject()
                                            .put("id", m.id)
                                            // The name, not the path: the path
                                            // contains this install's own
                                            // directory, which is different on
                                            // the phone being restored to.
                                            .put("file", m.path.substringAfterLast('/'))
                                            .put("durationMs", m.durationMs)
                                            .put("createdAt", m.createdAt)
                                            .put("transcript", m.transcript)
                                    )
                                }
                            })
                    )
                }
            })
        return root.toString().toByteArray()
    }

    data class Decoded(
        val savedAt: Long,
        val notes: List<Note>,
        val categories: List<Category>,
        /** Audio file names the notes refer to, to be fetched from Drive. */
        val memoFiles: List<String>
    )

    /**
     * @param memoDir where restored audio will live, so each memo's path points
     *        at this install rather than the one that made the backup.
     */
    fun decode(bytes: ByteArray, memoDir: String): Decoded {
        val root = JSONObject(String(bytes))

        val categories = root.optJSONArray("categories").orEmpty().map { o ->
            Category(
                key = o.getString("key"),
                label = o.getString("label"),
                color = o.getInt("color"),
                iconName = o.optString("iconName", "bookmark"),
                sortOrder = o.optInt("sortOrder"),
                builtIn = o.optBoolean("builtIn"),
                system = o.optBoolean("system")
            )
        }

        val memoFiles = mutableListOf<String>()
        val notes = root.optJSONArray("notes").orEmpty().map { o ->
            val memos = o.optJSONArray("memos").orEmpty().map { m ->
                val name = m.optString("file")
                if (name.isNotBlank()) memoFiles += name
                Memo(
                    id = m.optString("id", name),
                    path = "$memoDir/$name",
                    durationMs = m.optLong("durationMs"),
                    createdAt = m.optLong("createdAt"),
                    transcript = m.optString("transcript")
                )
            }
            Note(
                id = o.optLong("id"),
                title = o.optString("title"),
                content = o.optString("content"),
                checklist = o.optJSONArray("checklist").orEmpty().map { c ->
                    ChecklistItem(c.optString("text"), c.optBoolean("done"))
                },
                memos = memos,
                categoryKey = o.optString("categoryKey", Category.WORK_KEY),
                previousCategoryKey = o.optStringOrNull("previousCategoryKey"),
                pinned = o.optBoolean("pinned"),
                dueAt = o.optLongOrNull("dueAt"),
                remindMinutesBefore = o.optIntOrNull("remindMinutesBefore"),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        return Decoded(
            savedAt = root.optLong("savedAt"),
            notes = notes,
            categories = categories,
            memoFiles = memoFiles.distinct()
        )
    }
}

// ---- org.json helpers ----
// org.json turns a missing value into a zero or an empty string, which would
// quietly bring a note back with dueAt = 0 (January 1970) instead of no date.

private fun JSONArray?.orEmpty(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key).takeIf { it != 0L }

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (isNull(key)) null else if (has(key)) optInt(key) else null

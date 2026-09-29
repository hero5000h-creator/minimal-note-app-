package com.hanooot.notes.data

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the list fields as JSON strings. Uses org.json (part of Android) so
 * the project doesn't need an extra serialization compiler plugin.
 */
class Converters {

    @TypeConverter
    fun checklistToJson(items: List<ChecklistItem>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("text", it.text).put("done", it.done))
        }
        return arr.toString()
    }

    @TypeConverter
    fun jsonToChecklist(json: String?): List<ChecklistItem> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChecklistItem(o.optString("text"), o.optBoolean("done"))
            }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun memosToJson(memos: List<Memo>): String {
        val arr = JSONArray()
        memos.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("path", it.path)
                    .put("durationMs", it.durationMs)
                    .put("createdAt", it.createdAt)
                    .put("transcript", it.transcript)
            )
        }
        return arr.toString()
    }

    @TypeConverter
    fun jsonToMemos(json: String?): List<Memo> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Memo(
                    id = o.optString("id"),
                    path = o.optString("path"),
                    durationMs = o.optLong("durationMs"),
                    createdAt = o.optLong("createdAt"),
                    transcript = o.optString("transcript")
                )
            }
        }.getOrDefault(emptyList())
    }
}

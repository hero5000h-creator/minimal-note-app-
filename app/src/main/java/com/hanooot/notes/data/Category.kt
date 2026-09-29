package com.hanooot.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categories are rows rather than an enum so the user can add their own.
 * The four built-ins plus Done ship with the app and can't be deleted.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val key: String,
    val label: String,
    /** ARGB colour, used only when the Multicolour theme is active. */
    val color: Int,
    val iconName: String,
    val sortOrder: Int,
    val builtIn: Boolean = false,
    /** Done is managed by the app: notes move in and out of it automatically. */
    val system: Boolean = false
) {
    companion object {
        const val WORK_KEY = "WORK"
        const val PERSONAL_KEY = "PERSONAL"
        const val WIFE_KEY = "WIFE"
        const val OTHER_KEY = "OTHER"
        const val DONE_KEY = "DONE"

        fun defaults(): List<Category> = listOf(
            Category(WORK_KEY,     "Work",     0xFFFFA726.toInt(), "work",     0, builtIn = true),
            Category(PERSONAL_KEY, "Personal", 0xFFFF5252.toInt(), "home",     1, builtIn = true),
            Category(WIFE_KEY,     "Wife",     0xFFEC407A.toInt(), "heart",    2, builtIn = true),
            Category(OTHER_KEY,    "Other",    0xFF26C6DA.toInt(), "bookmark", 3, builtIn = true),
            Category(DONE_KEY,     "Done",     0xFF2FBF71.toInt(), "check",    4, builtIn = true, system = true)
        )
    }
}

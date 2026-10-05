package com.hanooot.notes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// One DataStore for the whole app. Declaring a second delegate over the same
// file name would throw at runtime, so every preference lives here.
private val Context.settingsStore by preferencesDataStore("settings")

private val THEME_KEY = stringPreferencesKey("theme")
private val LANG_KEY = stringPreferencesKey("lang")

/** Accent colour and interface language, persisted across launches. */
class Settings(private val context: Context) {

    val theme: Flow<String> = context.settingsStore.data.map { it[THEME_KEY] ?: "red" }

    val language: Flow<String> = context.settingsStore.data.map { it[LANG_KEY] ?: "en" }

    suspend fun setTheme(key: String) {
        context.settingsStore.edit { it[THEME_KEY] = key }
    }

    suspend fun setLanguage(key: String) {
        context.settingsStore.edit { it[LANG_KEY] = key }
    }

    /** For the boot receiver, which has no view model to read from. */
    suspend fun languageOnce(): String = language.first()
}

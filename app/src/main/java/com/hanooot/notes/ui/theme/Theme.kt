package com.hanooot.notes.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Surfaces stay dark across every theme; only the accent changes.
val Bg = Color(0xFF0B0B0D)
val Panel = Color(0xFF141416)
val CardStroke = Color(0xFF2A2A2E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8A8A8E)
val TextMuted = Color(0xFF5A5A5E)
val DoneGreen = Color(0xFF2FBF71)

/**
 * @param multicolour when true each category keeps its own colour; otherwise
 *        every category icon and label is drawn in the accent colour.
 */
data class AppTheme(
    val key: String,
    val label: String,
    val accent: Color,
    val multicolour: Boolean = false
)

val AppThemes = listOf(
    AppTheme("red",    "Dark & Red",    Color(0xFFFF3B30)),
    AppTheme("orange", "Dark & Orange", Color(0xFFFF5A36)),
    AppTheme("amber",  "Dark & Amber",  Color(0xFFFFAB00)),
    AppTheme("green",  "Dark & Green",  Color(0xFF2FBF71)),
    AppTheme("blue",   "Dark & Blue",   Color(0xFF3D8BFD)),
    AppTheme("purple", "Dark & Purple", Color(0xFFA855F7)),
    AppTheme("multi",  "Multicolour",   Color(0xFFFF3B30), multicolour = true)
)

fun themeByKey(key: String): AppTheme =
    AppThemes.firstOrNull { it.key == key } ?: AppThemes.first()

// static: the theme changes rarely, so readers need no recomposition tracking
val LocalAppTheme = staticCompositionLocalOf { AppThemes.first() }

private val Context.themeStore by preferencesDataStore("settings")
private val THEME_KEY = stringPreferencesKey("theme")

class ThemePreference(private val context: Context) {
    val flow: Flow<String> = context.themeStore.data.map { it[THEME_KEY] ?: "red" }
    suspend fun set(key: String) {
        context.themeStore.edit { it[THEME_KEY] = key }
    }
}

@Composable
fun NotesTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = theme.accent,
        onPrimary = Color.White,
        secondary = theme.accent,
        background = Bg,
        onBackground = TextPrimary,
        surface = Bg,
        onSurface = TextPrimary,
        surfaceVariant = Panel,
        onSurfaceVariant = TextSecondary,
        outline = CardStroke,
        error = Color(0xFFFF5252)
    )
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

package com.hanooot.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Surfaces stay dark across every theme; only the accent changes.
val Bg = Color(0xFF0B0B0D)
val Panel = Color(0xFF141416)
val CardStroke = Color(0xFF2A2A2E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8A8A8E)
val TextMuted = Color(0xFF5A5A5E)
val DoneGreen = Color(0xFF2FBF71)

/**
 * The name is looked up by key in the string table, so a theme reads correctly
 * in whichever language is active.
 *
 * @param multicolour when true each category keeps its own colour; otherwise
 *        every category icon and label is drawn in the accent colour.
 */
data class AppTheme(
    val key: String,
    val accent: Color,
    val multicolour: Boolean = false
)

val AppThemes = listOf(
    AppTheme("red",    Color(0xFFFF3B30)),
    AppTheme("orange", Color(0xFFFF5A36)),
    AppTheme("amber",  Color(0xFFFFAB00)),
    AppTheme("green",  Color(0xFF2FBF71)),
    AppTheme("blue",   Color(0xFF3D8BFD)),
    AppTheme("purple", Color(0xFFA855F7)),
    AppTheme("multi",  Color(0xFFFF3B30), multicolour = true)
)

fun themeByKey(key: String): AppTheme =
    AppThemes.firstOrNull { it.key == key } ?: AppThemes.first()

// static: the theme changes rarely, so readers need no recomposition tracking
val LocalAppTheme = staticCompositionLocalOf { AppThemes.first() }

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

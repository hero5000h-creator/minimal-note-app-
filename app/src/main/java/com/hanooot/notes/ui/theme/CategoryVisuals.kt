package com.hanooot.notes.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hanooot.notes.data.Category

/** Icon choices offered when creating a category. */
val CategoryIcons: List<Pair<String, ImageVector>> = listOf(
    "work" to Icons.Filled.Work,
    "home" to Icons.Filled.Home,
    "heart" to Icons.Filled.Favorite,
    "bookmark" to Icons.Filled.Bookmark,
    "check" to Icons.Filled.CheckCircle,
    "star" to Icons.Filled.Star,
    "idea" to Icons.Filled.Lightbulb,
    "calendar" to Icons.Filled.Event,
    "mail" to Icons.Filled.Email,
    "shield" to Icons.Filled.Shield,
    "cart" to Icons.Filled.ShoppingCart,
    "flight" to Icons.Filled.Flight
)

fun iconFor(name: String): ImageVector =
    CategoryIcons.firstOrNull { it.first == name }?.second ?: Icons.Filled.Bookmark

/** Colour palette offered when creating a category. */
val CategoryColors: List<Color> = listOf(
    Color(0xFFFFA726), Color(0xFFFF5252), Color(0xFFEC407A), Color(0xFF26C6DA),
    Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFFCA28),
    Color(0xFF8D6E63), Color(0xFF78909C)
)

/**
 * The colour a category should render in right now: its own under the
 * Multicolour theme, otherwise the single accent colour.
 */
@Composable
fun categoryColor(category: Category?): Color {
    val theme = LocalAppTheme.current
    return when {
        category == null -> theme.accent
        theme.multicolour -> Color(category.color)
        else -> theme.accent
    }
}

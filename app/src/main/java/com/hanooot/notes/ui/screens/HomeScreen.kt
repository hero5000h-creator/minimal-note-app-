package com.hanooot.notes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.data.Category
import com.hanooot.notes.data.Note
import com.hanooot.notes.ui.components.NoteCard
import com.hanooot.notes.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    categories: List<Category>,
    onOpenNote: (Note) -> Unit,
    onNewNote: () -> Unit,
    onAddCategory: () -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onOpenThemes: () -> Unit,
    showCalendar: Boolean,
    onToggleCalendar: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current

    // "All" first, then every category in order
    val tabs: List<Category?> = remember(categories) { listOf(null) + categories }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    // Confirm before removing a category the user made
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Panel,
            title = { Text("Delete \"${target.label}\"?", color = TextPrimary) },
            text = {
                val held = notes.count { it.categoryKey == target.key }
                Text(
                    if (held > 0)
                        "Its $held note${if (held == 1) "" else "s"} will move to another category, not be deleted."
                    else "This category has no notes.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(target); pendingDelete = null
                }) { Text("Delete", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier.fillMaxSize().background(Bg)) {

        // ---- Header ----
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${notes.size} ${if (notes.size == 1) "note" else "notes"}",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))

            // List / calendar toggle
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Panel)
                    .padding(2.dp)
            ) {
                ViewToggleButton(Icons.Filled.ViewList, !showCalendar) { onToggleCalendar(false) }
                ViewToggleButton(Icons.Filled.CalendarMonth, showCalendar) { onToggleCalendar(true) }
            }

            IconButton(onClick = onOpenThemes) {
                Icon(Icons.Filled.Palette, "Theme", tint = TextSecondary)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(theme.accent)
                    .clickable { onNewNote() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, "New note", tint = Color.White)
            }
        }

        // ---- Category tabs ----
        // A plain scrolling row rather than ScrollableTabRow: the tabs need
        // long-press handling, which Tab() doesn't expose.
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                tabs.forEachIndexed { index, category ->
                    val selected = pagerState.currentPage == index
                    val deletable = category != null && !category.builtIn && !category.system
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .combinedClickable(
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                // Long-press removes a category the user created.
                                // Built-ins and Done are protected.
                                onLongClick = { if (deletable) pendingDelete = category }
                            )
                    ) {
                        // Colour and underline animate, so swiping between
                        // categories reads as one continuous motion.
                        val tabColor by animateColorAsState(
                            targetValue = if (selected) theme.accent else TextSecondary,
                            animationSpec = tween(220),
                            label = "tabColor"
                        )
                        val underline by animateDpAsState(
                            targetValue = if (selected) 26.dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "underline"
                        )
                        Text(
                            (category?.label ?: "All").uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = tabColor,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            Modifier
                                .height(2.dp)
                                .width(underline)
                                .background(theme.accent, RoundedCornerShape(2.dp))
                        )
                    }
                }
                // Trailing "+" to create a category
                Text(
                    "+",
                    fontSize = 18.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { onAddCategory() }
                        .padding(vertical = 12.dp)
                )
            }
            Box(
                Modifier.fillMaxWidth().height(1.dp).background(CardStroke)
            )
        }

        if (showCalendar) {
            CalendarScreen(
                notes = notes,
                categories = categories,
                activeCategory = tabs.getOrNull(pagerState.currentPage),
                onOpenNote = onOpenNote,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Swiping between categories is the pager's own gesture — smooth,
            // interruptible and correct by construction.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 0.dp
            ) { page ->
                val category = tabs[page]
                val visible = remember(notes, category) {
                    if (category == null) notes
                    else notes.filter { it.categoryKey == category.key }
                }
                NoteList(
                    notes = visible,
                    categories = categories,
                    onOpenNote = onOpenNote
                )
            }
        }
    }
}

@Composable
private fun ViewToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        Modifier
            .size(width = 34.dp, height = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) theme.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) theme.accent else TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteList(
    notes: List<Note>,
    categories: List<Category>,
    onOpenNote: (Note) -> Unit
) {
    if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No notes in this category yet",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                category = categories.firstOrNull { it.key == note.categoryKey },
                onClick = { onOpenNote(note) },
                // Pinning a note, or completing one so it moves to Done,
                // slides the row instead of making the list jump.
                modifier = Modifier.animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            )
        }
    }
}

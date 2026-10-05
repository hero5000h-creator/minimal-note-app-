package com.hanooot.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.data.Category
import com.hanooot.notes.data.Note
import com.hanooot.notes.ui.components.NoteCard
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.i18n.Strings
import com.hanooot.notes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private data class Day(val millis: Long, val inMonth: Boolean, val dayOfMonth: Int)

@Composable
fun CalendarScreen(
    notes: List<Note>,
    categories: List<Category>,
    activeCategory: Category?,
    onOpenNote: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val s = LocalStrings.current
    var cursor by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedKey by remember { mutableStateOf<String?>(null) }

    val monthFmt = remember(s.lang) { SimpleDateFormat(s.monthYearPattern, s.locale) }

    // Notes shown honour the category tab you're on
    val scoped = remember(notes, activeCategory) {
        if (activeCategory == null) notes else notes.filter { it.categoryKey == activeCategory.key }
    }
    val byDay = remember(scoped) {
        scoped.filter { it.dueAt != null }.groupBy { dayKey(it.dueAt!!) }
    }

    val days = remember(cursor.timeInMillis) { buildMonthGrid(cursor) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                monthFmt.format(cursor.time),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                cursor = (cursor.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) {
                // In Arabic the arrows swap: "back a month" points right.
                Icon(
                    if (s.lang.rtl) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft,
                    s.prevMonth, tint = TextSecondary
                )
            }
            TextButton(onClick = {
                cursor = Calendar.getInstance()
                selectedKey = dayKey(System.currentTimeMillis())
            }) { Text(s.todayButton, color = TextSecondary, fontSize = 12.sp) }
            IconButton(onClick = {
                cursor = (cursor.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) {
                Icon(
                    if (s.lang.rtl) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                    s.nextMonth, tint = TextSecondary
                )
            }
        }

        Row(Modifier.fillMaxWidth()) {
            s.weekdayInitials.forEach {
                Text(
                    it, Modifier.weight(1f),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(days) { day ->
                val key = dayKey(day.millis)
                val dayNotes = byDay[key].orEmpty()
                val isToday = key == dayKey(System.currentTimeMillis())
                val isSelected = key == selectedKey

                Box(
                    Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isSelected) theme.accent else Color.Transparent)
                        .then(
                            if (isToday && !isSelected)
                                Modifier.border(1.dp, theme.accent, RoundedCornerShape(11.dp))
                            else Modifier
                        )
                        .clickable { selectedKey = if (isSelected) null else key },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            day.dayOfMonth.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                !day.inMonth -> TextMuted.copy(alpha = 0.5f)
                                else -> TextPrimary
                            }
                        )
                        Spacer(Modifier.height(3.dp))
                        Row {
                            dayNotes.take(3).forEach { n ->
                                val cat = categories.firstOrNull { it.key == n.categoryKey }
                                Box(
                                    Modifier.size(5.dp).padding(horizontal = 0.5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White else categoryColor(cat)
                                        )
                                )
                                Spacer(Modifier.width(2.dp))
                            }
                        }
                    }
                }
            }
        }

        selectedKey?.let { key ->
            val dayNotes = byDay[key].orEmpty().sortedBy { it.dueAt }
            Spacer(Modifier.height(16.dp))
            Text(
                headingFor(key, s),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            if (dayNotes.isEmpty()) {
                Text(s.nothingScheduled, color = TextMuted, fontSize = 14.sp)
            } else {
                LazyColumn {
                    items(dayNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            category = categories.firstOrNull { it.key == note.categoryKey },
                            onClick = { onOpenNote(note) }
                        )
                    }
                }
            }
        }
    }
}

private fun dayKey(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
}

private fun headingFor(key: String, s: Strings): String {
    val (y, m, d) = key.split("-").map { it.toInt() }
    val c = Calendar.getInstance().apply { set(y, m, d) }
    return SimpleDateFormat(s.dayHeadingPattern, s.locale)
        .format(c.time).uppercase(s.locale)
}

/** Six weeks starting from the Sunday on or before the 1st — always covers the month. */
private fun buildMonthGrid(cursor: Calendar): List<Day> {
    val month = cursor.get(Calendar.MONTH)
    val start = (cursor.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.DAY_OF_YEAR, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return (0 until 42).map { i ->
        val c = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
        Day(c.timeInMillis, c.get(Calendar.MONTH) == month, c.get(Calendar.DAY_OF_MONTH))
    }
}

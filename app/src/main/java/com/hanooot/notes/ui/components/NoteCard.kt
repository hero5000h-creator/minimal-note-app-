package com.hanooot.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.i18n.Strings
import com.hanooot.notes.ui.i18n.labelOf
import com.hanooot.notes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteCard(
    note: Note,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val accent = categoryColor(category)
    val complete = note.categoryKey == Category.DONE_KEY

    // A plain Box rather than Card(onClick = …): the clickable Card overload is
    // an experimental Material3 API, and this needs no opt-in.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Bg)
            .border(1.dp, CardStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconFor(category?.iconName ?: "bookmark"),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    note.title.ifBlank { s.untitled },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (complete) TextSecondary else TextPrimary,
                    textDecoration = if (complete) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (note.memos.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Mic, null, tint = accent,
                        modifier = Modifier.size(14.dp).padding(start = 0.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (note.remindMinutesBefore != null && note.dueAt != null) {
                    Icon(
                        Icons.Filled.Notifications, null, tint = accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                note.dueAt?.let { due ->
                    val soon = due < System.currentTimeMillis() + 86_400_000L
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (soon) accent.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.07f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            formatDue(due, s),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (soon) accent else TextSecondary
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    s.labelOf(category).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1
                )
            }

            val preview = previewFor(note, s)
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    preview,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }
        }
    }
}

private fun previewFor(note: Note, s: Strings): String {
    if (note.memos.isNotEmpty() && note.content.isBlank() && note.checklist.isEmpty()) {
        return s.recordingsCount(note.memos.size)
    }
    if (note.checklist.isNotEmpty()) {
        val left = note.checklist.count { !it.done }
        return if (left == 0) s.allItemsDone else s.itemsLeft(left)
    }
    return note.content
}

private fun formatDue(millis: Long, s: Strings): String {
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = millis }
    // lowercase() with the active locale: "PM" becomes "pm" in English and is
    // left alone in Arabic, where case does not exist.
    val time = SimpleDateFormat(s.timePattern, s.locale)
        .format(Date(millis)).lowercase(s.locale)

    val sameDay = now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "${s.today} $time"

    now.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (tomorrow) return "${s.tomorrow} $time"

    return SimpleDateFormat(s.monthDayPattern, s.locale).format(Date(millis))
}

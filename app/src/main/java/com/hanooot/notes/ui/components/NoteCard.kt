package com.hanooot.notes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.hanooot.notes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = categoryColor(category)
    val complete = note.categoryKey == Category.DONE_KEY

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Bg),
        border = BorderStroke(1.dp, CardStroke)
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
                    note.title.ifBlank { "Untitled" },
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
                            formatDue(due),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (soon) accent else TextSecondary
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    (category?.label ?: "").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1
                )
            }

            val preview = previewFor(note)
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

private fun previewFor(note: Note): String {
    if (note.memos.isNotEmpty() && note.content.isBlank() && note.checklist.isEmpty()) {
        val n = note.memos.size
        return "$n recording${if (n == 1) "" else "s"}"
    }
    if (note.checklist.isNotEmpty()) {
        val left = note.checklist.count { !it.done }
        return if (left == 0) "All items done"
        else "$left item${if (left == 1) "" else "s"} left"
    }
    return note.content
}

private fun formatDue(millis: Long): String {
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = millis }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis)).lowercase()

    val sameDay = now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Today $time"

    now.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (tomorrow) return "Tomorrow $time"

    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
}

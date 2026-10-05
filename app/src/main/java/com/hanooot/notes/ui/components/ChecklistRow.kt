package com.hanooot.notes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.data.ChecklistItem
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.theme.*

// One checklist row. Only the checkbox animates — re-rendering the whole
// list on a tick made a single tap look like the list had reloaded.
@Composable
fun ChecklistRow(
    item: ChecklistItem,
    accent: Color,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val s = LocalStrings.current
    // Only the checkbox animates; the row itself stays put so ticking an item
    // never looks like the whole list reloaded.
    val scale by animateFloatAsState(
        targetValue = if (item.done) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (item.done) DoneGreen else Color.Transparent)
                .border(
                    2.dp,
                    if (item.done) DoneGreen else TextSecondary,
                    CircleShape
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (item.done) {
                Icon(
                    Icons.Filled.Check, null, tint = Color.White,
                    modifier = Modifier.size(14.dp * scale)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                color = if (item.done) TextMuted else TextPrimary,
                fontSize = 16.sp,
                textDecoration = if (item.done) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                if (item.text.isEmpty()) Text(s.listItemHint, color = TextMuted, fontSize = 16.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, s.remove, tint = TextMuted,
                modifier = Modifier.size(15.dp))
        }
    }
}

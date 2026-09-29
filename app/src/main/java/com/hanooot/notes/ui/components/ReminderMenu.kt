package com.hanooot.notes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.ui.theme.*

/** Reminder offsets, in minutes before the due time. null means no reminder. */
val ReminderOptions: List<Pair<String, Int?>> = listOf(
    "Off" to null,
    "At time" to 0,
    "5 min before" to 5,
    "15 min before" to 15,
    "1 hour before" to 60,
    "1 day before" to 1440
)

@Composable
fun ReminderMenu(
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    val theme = LocalAppTheme.current
    var open by remember { mutableStateOf(false) }

    val caret by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "caret"
    )
    val currentLabel = ReminderOptions.firstOrNull { it.second == selected }?.first ?: "Off"

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (open) theme.accent.copy(alpha = 0.07f) else Color.Transparent)
                .clickable { open = !open }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Notifications, null,
                tint = if (selected != null) theme.accent else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Remind me", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                currentLabel,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (selected != null) theme.accent else TextMuted
            )
            Icon(
                Icons.Filled.ExpandMore, null, tint = TextMuted,
                modifier = Modifier.size(18.dp).rotate(caret)
            )
        }

        // expandVertically measures the real content height, so open and close
        // both start moving immediately.
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(animationSpec = tween(320)) +
                    fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220)) +
                    fadeOut(animationSpec = tween(140))
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                ReminderOptions.forEach { (label, mins) ->
                    val active = mins == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (active) theme.accent.copy(alpha = 0.10f) else Color.Transparent
                            )
                            .clickable { onSelect(mins); open = false }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label, fontSize = 14.sp,
                            color = if (active) theme.accent else TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        if (active) {
                            Icon(Icons.Filled.Check, null, tint = theme.accent,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

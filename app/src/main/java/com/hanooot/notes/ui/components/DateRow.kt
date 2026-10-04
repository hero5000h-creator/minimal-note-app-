package com.hanooot.notes.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// The note's scheduled date, picked through the platform date/time dialogs.
@Composable
fun DateRow(
    dueAt: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
            .clickable {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dueAt ?: System.currentTimeMillis()
                    if (dueAt == null) { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
                }
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        cal.set(y, m, d)
                        android.app.TimePickerDialog(
                            context,
                            { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h)
                                cal.set(Calendar.MINUTE, min)
                                cal.set(Calendar.SECOND, 0)
                                onPick(cal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
                        ).show()
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Event, null, tint = TextSecondary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            dueAt?.let { fmt.format(Date(it)) } ?: "Add date & reminder",
            color = if (dueAt == null) TextSecondary else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (dueAt != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.Close, "Remove date", tint = TextMuted,
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

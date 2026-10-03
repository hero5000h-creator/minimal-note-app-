package com.hanooot.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.data.Category
import com.hanooot.notes.ui.theme.*

// A dialog rather than a bottom sheet: ModalBottomSheet is an experimental
// Material3 API, and this needs no opt-in.
@Composable
fun ThemeSheet(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                "ACCENT COLOUR",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        },
        text = {
        Column {
            AppThemes.forEach { t ->
                val active = t.key == current.key
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(t) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Multicolour gets a rainbow swatch so it reads as "many colours"
                    val swatch = if (t.multicolour) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF3B30), Color(0xFFFFAB00), Color(0xFF2FBF71),
                                Color(0xFF3D8BFD), Color(0xFFA855F7), Color(0xFFFF3B30)
                            )
                        )
                    } else SolidColor(t.accent)

                    Box(Modifier.size(20.dp).clip(CircleShape).background(swatch))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        t.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = if (active) TextPrimary else TextSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    if (active) Icon(Icons.Filled.Check, null, tint = t.accent)
                }
            }
        }
        }
    )
}

@Composable
fun NewCategoryDialog(
    existing: List<Category>,
    onCreate: (String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(CategoryColors[existing.size % CategoryColors.size]) }
    var icon by remember { mutableStateOf(CategoryIcons.first().first) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("New category", color = TextPrimary) },
        text = {
            Column {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Bg)
                        .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it; error = "" },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(color),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("e.g. Ideas", color = TextMuted, fontSize = 15.sp)
                            inner()
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("COLOUR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColors.take(5).forEach { c -> Swatch(c, c == color) { color = c } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColors.drop(5).forEach { c -> Swatch(c, c == color) { color = c } }
                }
                Text(
                    "Shown when the Multicolour theme is active.",
                    fontSize = 11.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 7.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("ICON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Column {
                    CategoryIcons.chunked(6).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (key, vector) ->
                                Box(
                                    Modifier.size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (key == icon) color.copy(alpha = 0.16f) else Bg
                                        )
                                        .border(
                                            1.dp,
                                            if (key == icon) color else CardStroke,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { icon = key },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(vector, null, tint = color,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF5252), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                when {
                    trimmed.isEmpty() -> error = "Please enter a name."
                    existing.any { it.label.equals(trimmed, ignoreCase = true) } ->
                        error = "That category already exists."
                    else -> onCreate(trimmed, color.toArgb(), icon)
                }
            }) { Text("Add", color = color, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(color)
            .border(
                2.dp,
                if (selected) Color.White else Color.Transparent,
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

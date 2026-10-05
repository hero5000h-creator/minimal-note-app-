package com.hanooot.notes.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.theme.*

// Live recording state: pulsing dot, elapsed time, input meter, stop button,
// and the transcript as it is recognised.
@Composable
fun RecordingPanel(
    elapsedMs: Long,
    level: Float,
    transcript: String,
    onStop: () -> Unit
) {
    val s = LocalStrings.current
    val red = Color(0xFFFF3B30)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, red.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .background(red.copy(alpha = 0.07f))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
                initialValue = 1f, targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "dot"
            )
            Box(Modifier.size(10.dp).clip(CircleShape)
                .background(red.copy(alpha = pulse)))
            Spacer(Modifier.width(12.dp))
            Text(
                formatElapsed(elapsedMs),
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(12.dp))

            // Simple live level meter driven by the recorder's amplitude
            Row(
                Modifier.weight(1f).height(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(20) { i ->
                    val h = (0.15f + level * (0.4f + 0.6f * ((i % 5) / 5f))).coerceIn(0.12f, 1f)
                    Box(
                        Modifier.weight(1f).padding(horizontal = 1.dp)
                            .fillMaxHeight(h)
                            .background(LocalAppTheme.current.accent)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(red)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(11.dp).background(Color.White))
            }
        }

        if (transcript.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Text(s.transcriptHeading, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(transcript, fontSize = 14.sp, color = TextPrimary)
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

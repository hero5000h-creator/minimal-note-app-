package com.hanooot.notes.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.audio.TaskExtractor
import com.hanooot.notes.data.Memo
import com.hanooot.notes.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MemoRow(
    memo: Memo,
    accent: Color,
    onDelete: () -> Unit,
    onAddTasks: () -> Unit
) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Always release the player when the row leaves the screen, otherwise the
    // audio keeps running in the background.
    DisposableEffect(memo.id) {
        onDispose { player?.release(); player = null }
    }

    LaunchedEffect(playing) {
        while (playing) {
            player?.let {
                if (it.duration > 0) progress = it.currentPosition.toFloat() / it.duration
            }
            delay(80)
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(accent)
                    .clickable {
                        if (playing) {
                            player?.pause(); playing = false
                        } else {
                            if (player == null) {
                                player = MediaPlayer().apply {
                                    runCatching {
                                        setDataSource(memo.path); prepare()
                                        setOnCompletionListener {
                                            playing = false; progress = 0f
                                            seekTo(0)
                                        }
                                    }
                                }
                            }
                            runCatching { player?.start(); playing = true }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accent,
                    trackColor = CardStroke
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    formatDuration(memo.durationMs),
                    fontSize = 11.sp, color = TextMuted
                )
            }
            IconButton(onClick = {
                player?.release(); player = null; playing = false
                onDelete()
            }) {
                Icon(Icons.Filled.Delete, "Delete recording", tint = TextMuted,
                    modifier = Modifier.size(16.dp))
            }
        }

        if (memo.transcript.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Text("TRANSCRIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(memo.transcript, fontSize = 14.sp, color = TextPrimary)
            }

            val taskCount = remember(memo.transcript) {
                TaskExtractor.extract(memo.transcript).size
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAddTasks,
                enabled = taskCount > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                    disabledContainerColor = Panel,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.PlaylistAddCheck, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (taskCount > 0)
                        "Add $taskCount task${if (taskCount == 1) "" else "s"} to checklist"
                    else "No tasks found",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

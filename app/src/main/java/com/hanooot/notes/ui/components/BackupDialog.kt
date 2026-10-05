package com.hanooot.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanooot.notes.ui.i18n.LocalStrings
import com.hanooot.notes.ui.theme.*

/**
 * Account and backup state, in one place.
 *
 * @param email the signed-in address, or null when nobody is signed in.
 * @param busy a label while a backup or restore is running, or null.
 */
@Composable
fun BackupDialog(
    email: String?,
    lastBackupLabel: String,
    busy: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val theme = LocalAppTheme.current
    var confirmRestore by remember { mutableStateOf(false) }

    // Restoring throws away whatever is newer than the backup, so it asks.
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = Panel,
            title = { Text(s.restoreWarningTitle, color = TextPrimary) },
            text = { Text(s.restoreWarningBody, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    onRestore()
                }) { Text(s.restoreConfirm, color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(s.cancel, color = TextSecondary)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                s.backupHeading,
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.close, color = TextSecondary) }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(
                                if (email != null) theme.accent.copy(alpha = 0.16f)
                                else Bg
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (email != null) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (email != null) theme.accent else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (email != null) {
                            Text(
                                s.signedInAs, fontSize = 11.sp, color = TextMuted
                            )
                            Text(
                                email,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                s.neverBackedUp,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (email == null) {
                    Text(
                        s.signInExplainer,
                        fontSize = 13.sp, color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            s.signInWithGoogle,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(lastBackupLabel, fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))

                    if (busy != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = theme.accent
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(busy, fontSize = 13.sp, color = TextSecondary)
                        }
                    } else {
                        ActionRow(
                            icon = Icons.Filled.CloudUpload,
                            label = s.backUpNow,
                            tint = theme.accent,
                            onClick = onBackUp
                        )
                        Spacer(Modifier.height(8.dp))
                        ActionRow(
                            icon = Icons.Filled.Download,
                            label = s.restore,
                            tint = TextSecondary,
                            onClick = { confirmRestore = true }
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(s.autoBackupNote, fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(s.driveFolderNote, fontSize = 11.sp, color = TextMuted)

                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(s.signOut, color = TextMuted, fontSize = 13.sp) }
                }
            }
        }
    )
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(11.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

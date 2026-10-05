package com.hanooot.notes.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hanooot.notes.MainActivity
import com.hanooot.notes.R
import com.hanooot.notes.data.NotesRepository
import com.hanooot.notes.data.Settings
import com.hanooot.notes.ui.i18n.AppStrings
import com.hanooot.notes.ui.i18n.Lang
import com.hanooot.notes.ui.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "note_reminders"

object ReminderNotifications {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        // Not skipped when the channel exists: re-creating it with the same id
        // is how Android renames one, which is what a language change needs.
        val s = AppStrings.current
        val channel = NotificationChannel(
            CHANNEL_ID,
            s.channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = s.channelDescription
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
            ?: AppStrings.current.reminderFallbackTitle
        val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY) ?: ""

        ReminderNotifications.ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, noteId)
        }
        val pi = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
        }
    }
}

/**
 * Alarms are cleared when the device restarts, so they have to be rebuilt.
 * Without this every pending reminder would be silently lost on reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Nothing has run since the restart, so the language has to be
                // loaded before any reminder text is built.
                val saved = runCatching { Settings(appContext).languageOnce() }.getOrNull()
                AppStrings.current = stringsFor(Lang.of(saved))

                val repo = NotesRepository(appContext)
                ReminderScheduler.rescheduleAll(appContext, repo.allNotes())
            } finally {
                pending.finish()
            }
        }
    }
}

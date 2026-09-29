package com.hanooot.notes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hanooot.notes.data.Note

/**
 * Schedules reminders through AlarmManager, so they fire even when the app is
 * closed or the device is dozing — the thing a browser simply cannot do.
 */
object ReminderScheduler {

    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "note_title"
    const val EXTRA_BODY = "note_body"

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context, note: Note): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, note.id)
            putExtra(EXTRA_TITLE, note.title.ifBlank { "Reminder" })
            putExtra(EXTRA_BODY, reminderBody(note))
        }
        return PendingIntent.getBroadcast(
            context,
            note.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderBody(note: Note): String {
        val remaining = note.checklist.count { !it.done }
        return when {
            note.content.isNotBlank() -> note.content.take(80)
            remaining > 0 -> "$remaining item${if (remaining == 1) "" else "s"} left"
            else -> "Tap to open"
        }
    }

    /** True when the OS will let us schedule an exact alarm right now. */
    fun canScheduleExact(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager(context).canScheduleExactAlarms()
        } else true

    fun schedule(context: Context, note: Note) {
        cancel(context, note)
        val at = note.reminderAt ?: return
        if (at <= System.currentTimeMillis()) return

        val am = alarmManager(context)
        val pi = pendingIntent(context, note)

        // Fall back to an inexact alarm if the user hasn't granted the exact
        // alarm permission — a late reminder beats no reminder at all.
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, note: Note) {
        runCatching { alarmManager(context).cancel(pendingIntent(context, note)) }
    }

    fun rescheduleAll(context: Context, notes: List<Note>) {
        notes.forEach { schedule(context, it) }
    }
}

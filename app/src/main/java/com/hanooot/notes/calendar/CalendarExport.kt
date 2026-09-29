package com.hanooot.notes.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.hanooot.notes.data.Note
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CalendarExport {

    private const val DEFAULT_DURATION_MS = 30 * 60 * 1000L

    /**
     * Hands the note to the system calendar app as an event the user confirms.
     * This is the friendliest route: no calendar permissions needed.
     */
    fun addToCalendar(context: Context, note: Note) {
        val start = note.dueAt ?: return
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, note.title.ifBlank { "Note" })
            putExtra(CalendarContract.Events.DESCRIPTION, describe(note))
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + DEFAULT_DURATION_MS)
        }
        runCatching { context.startActivity(intent) }
    }

    /** Writes a standard .ics file and offers it to any app that accepts one. */
    fun shareIcs(context: Context, note: Note) {
        val start = note.dueAt ?: return
        val dir = File(context.cacheDir, "ics").apply { mkdirs() }
        val safeName = note.title.replace(Regex("""[^\w\s-]"""), "").trim().take(40)
        val file = File(dir, (if (safeName.isBlank()) "note" else safeName) + ".ics")
        file.writeText(buildIcs(note, start))

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(share, "Add to calendar"))
        }
    }

    private fun describe(note: Note): String = buildString {
        if (note.content.isNotBlank()) append(note.content)
        if (note.checklist.isNotEmpty()) {
            if (isNotEmpty()) append("\n\n")
            note.checklist.forEach {
                append(if (it.done) "[x] " else "[ ] ").append(it.text).append('\n')
            }
        }
    }.trim()

    private fun stamp(millis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis))
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")

    private fun buildIcs(note: Note, start: Long): String {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Hanooot Notes//EN",
            "CALSCALE:GREGORIAN",
            "BEGIN:VEVENT",
            "UID:note-${note.id}-$start@hanooot.notes",
            "DTSTAMP:${stamp(System.currentTimeMillis())}",
            "DTSTART:${stamp(start)}",
            "DTEND:${stamp(start + DEFAULT_DURATION_MS)}",
            "SUMMARY:${escape(note.title.ifBlank { "Note" })}",
            "DESCRIPTION:${escape(describe(note))}"
        )
        note.remindMinutesBefore?.let { mins ->
            lines += listOf(
                "BEGIN:VALARM",
                "TRIGGER:-PT${mins}M",
                "ACTION:DISPLAY",
                "DESCRIPTION:${escape(note.title.ifBlank { "Reminder" })}",
                "END:VALARM"
            )
        }
        lines += listOf("END:VEVENT", "END:VCALENDAR")
        return lines.joinToString("\r\n")
    }
}

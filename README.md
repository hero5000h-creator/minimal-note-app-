# Notes — Android app

A dark-themed notes app with categories, checklists, voice memos with
transcription, a built-in calendar, and reminders that fire when the app is
closed.

## Getting an APK

### Option A — GitHub builds it for you (no Android Studio needed)

Push this project to GitHub. The included workflow
(`.github/workflows/build-apk.yml`) builds an installable APK on every push and
attaches it to the run. Download it from the **Actions** tab.

The APK is a *debug* build, signed with Android's standard debug key — it
installs on your own phone with no keystore setup. For publishing to others
you would add a release keystore later.

### Option B — Android Studio

1. Open this folder in **Android Studio** (Ladybug or newer).
2. Let Gradle sync — it downloads everything on first run.
3. Press **Run**. Minimum Android 8.0 (API 26).

> This project was written without an Android SDK available, so it has not been
> compiled. Structure has been checked (imports, references, resources, brace
> balance), but expect a small number of compile errors on the first build. The
> GitHub build log names the exact file and line for each one.

## What's in it

**Categories**
- Built-ins: Work, Personal, Wife, Other, plus an automatic **Done**.
- Add your own with a name, colour and icon; long-press a custom category tab
  to delete it. Its notes move to another category rather than being deleted.
- Built-ins and Done can't be removed.

**Notes**
- Title, free text, and a checklist.
- Tick every checklist item and the note moves itself to **Done**. Untick one
  and it goes back to the category it came from — the move is never one-way.
- Pin notes to keep them at the top.

**Swiping**
- Swipe left/right between categories. This uses Compose's `HorizontalPager`,
  so the gesture is native: it tracks your finger, can be interrupted, and
  handles fling velocity without any custom physics.

**Voice memos**
- Records to `.m4a` inside app storage, so recordings survive restarts.
- Live level meter and timer while recording.
- **Live transcription** runs alongside the recording, so one pass gives you
  both audio and text.
- **Add tasks to checklist** turns the transcript into separate checklist
  items (see the limitation note below).

**Calendar**
- Month view with coloured dots for days that have scheduled notes.
- Tap a day to see what's on it. Respects the category tab you're on.
- Add a date/time to any note.
- Push a note to your system calendar, or share it as a standard `.ics` file.

**Reminders**
- Off, at time, 5 min, 15 min, 1 hour, or 1 day before.
- Uses `AlarmManager` with `setExactAndAllowWhileIdle`, so reminders arrive
  **even when the app is closed or the phone is dozing**.
- Rescheduled automatically after a reboot, which would otherwise clear them.

**Themes**
- Dark & Red (default), Orange, Amber, Green, Blue, Purple.
- **Multicolour** keeps each category's own colour; every other theme paints
  all category icons and labels in the single accent colour.

**Screen sizes**
- Phones: full-screen editor over the list.
- Unfolded foldables and tablets: two panes side by side, driven by
  `WindowSizeClass` rather than hardcoded dimensions.

## Permissions and why

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice memos. Requested when you first record. |
| `POST_NOTIFICATIONS` | Reminder notifications on Android 13+. |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Reminders that fire at the right minute. |
| `RECEIVE_BOOT_COMPLETED` | Restore pending reminders after a restart. |
| `INTERNET` | Some speech recognisers need it. |

## Two honest limitations

**1. Transcription quality.** This uses Android's built-in `SpeechRecognizer`.
It's fast, free and works offline on many devices — but it is not a large
speech model, so it will struggle with long, noisy, or heavily accented audio
compared to a server-side model like Whisper.

**2. Task extraction is not AI.** `TaskExtractor` is a rule-based parser. It
splits on the connectors people actually speak ("and then", "after that",
"ثم", "بعدين") and strips spoken filler ("okay so I need to", "لازم"). It works
well and runs entirely on-device, but it doesn't understand context — it can't
tell "call Ahmed" (a task) from "Ahmed called yesterday" (not one).

Using a real language model would mean sending the transcript to a server you
control, which holds the API key. **Never put an API key in the app itself** —
anyone can extract it from an APK and spend your quota. If you want that, the
shape is: app → your backend → model API.

## Project layout

```
data/       Room entities, DAOs, converters, repository (incl. the Done rule)
audio/      Recorder, live transcriber, task extractor
reminder/   Alarm scheduling, notification receiver, boot restore
calendar/   System-calendar insert and .ics export
ui/         Compose screens, components, theme
```

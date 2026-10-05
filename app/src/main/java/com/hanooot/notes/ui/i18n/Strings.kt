package com.hanooot.notes.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.hanooot.notes.data.Category
import java.util.Locale

/**
 * The two languages the app ships with.
 *
 * @param locale used for date and number formatting. Arabic is pinned to
 *        Latin digits (`nu-latn`): Arabic month and weekday names with
 *        Western numerals is the everyday style in Iraq, and it keeps times
 *        readable next to Latin text.
 * @param speechTag passed to the speech recogniser, which wants a plain tag.
 */
enum class Lang(
    val key: String,
    val nativeName: String,
    val locale: Locale,
    val speechTag: String,
    val rtl: Boolean
) {
    EN("en", "English", Locale.ENGLISH, "en-US", rtl = false),
    AR("ar", "العربية", Locale.forLanguageTag("ar-IQ-u-nu-latn"), "ar-IQ", rtl = true);

    companion object {
        fun of(key: String?): Lang = values().firstOrNull { it.key == key } ?: EN
    }
}

/**
 * Every piece of user-visible text, as data rather than resources.
 *
 * A plain table beats `strings.xml` here because the language is a setting
 * inside the app, not the system locale: switching it recomposes immediately,
 * with no activity restart and no per-app locale API.
 *
 * Counted strings are lambdas so each language can apply its own plural rule —
 * Arabic has four forms where English has two.
 */
data class Strings(
    val lang: Lang,
    val locale: Locale,

    // ---- Home ----
    val appTitle: String,
    val notesCount: (Int) -> String,
    val all: String,
    val search: String,
    val searchHint: String,
    val clear: String,
    val theme: String,
    val newNote: String,
    val emptyCategory: String,
    val noMatches: (String) -> String,
    val selectNoteHint: String,

    // ---- Shared actions ----
    val add: String,
    val cancel: String,
    val close: String,
    val save: String,
    val delete: String,
    val remove: String,
    val undo: String,
    val back: String,
    val pin: String,

    // ---- Category management ----
    val deleteCategoryTitle: (String) -> String,
    val categoryHoldsNotes: (Int) -> String,
    val categoryEmpty: String,
    val newCategory: String,
    val categoryNameHint: String,
    val colourHeading: String,
    val multicolourNote: String,
    val iconHeading: String,
    val enterName: String,
    val categoryExists: String,

    // ---- Appearance dialog ----
    val appearance: String,
    val languageHeading: String,
    val accentHeading: String,
    val themeName: (String) -> String,

    // ---- Built-in categories ----
    val catWork: String,
    val catPersonal: String,
    val catWife: String,
    val catOther: String,
    val catDone: String,

    // ---- Editor ----
    val titleHint: String,
    val bodyHint: String,
    val addItem: String,
    val listItemHint: String,
    val addToCalendar: String,
    val shareIcs: String,
    val recordMemo: String,
    val voiceMemoTitle: String,
    val micBlocked: String,
    val micBlockedDetail: String,
    val cantRecord: String,
    val alreadyAdded: String,
    val alreadyAddedDetail: String,
    val tasksAdded: (Int) -> String,
    val fromVoiceMemo: String,

    // ---- Date & reminder ----
    val addDateReminder: String,
    val removeDate: String,
    val remindMe: String,
    val reminderOptions: List<Pair<String, Int?>>,

    // ---- Voice memo row ----
    val play: String,
    val pause: String,
    val deleteRecording: String,
    val transcriptHeading: String,
    val editTranscript: String,
    val addTasksToList: (Int) -> String,
    val noTasksFound: String,

    // ---- Note card ----
    val untitled: String,
    val recordingsCount: (Int) -> String,
    val allItemsDone: String,
    val itemsLeft: (Int) -> String,
    val today: String,
    val tomorrow: String,

    // ---- Calendar ----
    val prevMonth: String,
    val nextMonth: String,
    val todayButton: String,
    val nothingScheduled: String,
    val weekdayInitials: List<String>,

    // ---- Snackbar messages ----
    val movedToDone: String,
    val reopenedInto: (String) -> String,
    val itsCategory: String,
    val deletedNote: (String) -> String,

    // ---- Notifications ----
    val channelName: String,
    val channelDescription: String,
    val reminderFallbackTitle: String,
    val tapToOpen: String,

    // ---- Backup ----
    val backup: String,
    val backupHeading: String,
    val signInWithGoogle: String,
    val signInExplainer: String,
    val signedInAs: String,
    val signOut: String,
    val backUpNow: String,
    val backingUp: String,
    val restore: String,
    val restoring: String,
    val lastBackup: (String) -> String,
    val neverBackedUp: String,
    val backupDone: (Int) -> String,
    val audioUploaded: (Int) -> String,
    val restoreDone: (Int) -> String,
    val noBackupFound: String,
    val backupFailed: String,
    val signInFailed: String,
    val signInNeeded: String,
    val restoreWarningTitle: String,
    val restoreWarningBody: String,
    val restoreConfirm: String,
    val autoBackupNote: String,
    val driveFolderNote: String,

    // ---- Date patterns ----
    val duePattern: String,
    val timePattern: String,
    val monthDayPattern: String,
    val monthYearPattern: String,
    val dayHeadingPattern: String
)

/**
 * Arabic plural selection. `one` and `two` are written out; `few` (3–10) and
 * `many` (11+) take the number in place of `#`.
 */
private fun arPlural(n: Int, one: String, two: String, few: String, many: String): String = when {
    n == 1 -> one
    n == 2 -> two
    n % 100 in 3..10 -> few.replace("#", n.toString())
    else -> many.replace("#", n.toString())
}

val EnglishStrings = Strings(
    lang = Lang.EN,
    locale = Lang.EN.locale,

    appTitle = "Notes",
    notesCount = { n -> "$n ${if (n == 1) "note" else "notes"}" },
    all = "All",
    search = "Search",
    searchHint = "Search notes…",
    clear = "Clear",
    theme = "Theme",
    newNote = "New note",
    emptyCategory = "No notes in this category yet",
    noMatches = { q -> "Nothing matches \"$q\"" },
    selectNoteHint = "Select a note, or tap + to write a new one",

    add = "Add",
    cancel = "Cancel",
    close = "Close",
    save = "Save",
    delete = "Delete",
    remove = "Remove",
    undo = "Undo",
    back = "Back",
    pin = "Pin",

    deleteCategoryTitle = { label -> "Delete \"$label\"?" },
    categoryHoldsNotes = { n ->
        "Its $n note${if (n == 1) "" else "s"} will move to another category, not be deleted."
    },
    categoryEmpty = "This category has no notes.",
    newCategory = "New category",
    categoryNameHint = "e.g. Ideas",
    colourHeading = "COLOUR",
    multicolourNote = "Shown when the Multicolour theme is active.",
    iconHeading = "ICON",
    enterName = "Please enter a name.",
    categoryExists = "That category already exists.",

    appearance = "APPEARANCE",
    languageHeading = "LANGUAGE",
    accentHeading = "ACCENT COLOUR",
    themeName = { key ->
        when (key) {
            "red" -> "Dark & Red"
            "orange" -> "Dark & Orange"
            "amber" -> "Dark & Amber"
            "green" -> "Dark & Green"
            "blue" -> "Dark & Blue"
            "purple" -> "Dark & Purple"
            else -> "Multicolour"
        }
    },

    catWork = "Work",
    catPersonal = "Personal",
    catWife = "Wife",
    catOther = "Other",
    catDone = "Done",

    titleHint = "Title",
    bodyHint = "Write more…",
    addItem = "+ Add item",
    listItemHint = "List item",
    addToCalendar = "Add to calendar",
    shareIcs = "Share as .ics",
    recordMemo = "Record voice memo",
    voiceMemoTitle = "Voice memo",
    micBlocked = "Microphone blocked",
    micBlockedDetail = "Enable it in Settings to record memos",
    cantRecord = "Couldn't start recording",
    alreadyAdded = "Already added",
    alreadyAddedDetail = "Those tasks are in the list",
    tasksAdded = { n -> "$n task${if (n == 1) "" else "s"} added" },
    fromVoiceMemo = "From your voice memo",

    addDateReminder = "Add date & reminder",
    removeDate = "Remove date",
    remindMe = "Remind me",
    reminderOptions = listOf(
        "Off" to null,
        "At time" to 0,
        "5 min before" to 5,
        "15 min before" to 15,
        "1 hour before" to 60,
        "1 day before" to 1440
    ),

    play = "Play",
    pause = "Pause",
    deleteRecording = "Delete recording",
    transcriptHeading = "TRANSCRIPT",
    editTranscript = "Edit transcript",
    addTasksToList = { n -> "Add $n task${if (n == 1) "" else "s"} to checklist" },
    noTasksFound = "No tasks found",

    untitled = "Untitled",
    recordingsCount = { n -> "$n recording${if (n == 1) "" else "s"}" },
    allItemsDone = "All items done",
    itemsLeft = { n -> "$n item${if (n == 1) "" else "s"} left" },
    today = "Today",
    tomorrow = "Tomorrow",

    prevMonth = "Previous month",
    nextMonth = "Next month",
    todayButton = "Today",
    nothingScheduled = "Nothing scheduled.",
    weekdayInitials = listOf("S", "M", "T", "W", "T", "F", "S"),

    movedToDone = "Completed — moved to Done",
    reopenedInto = { label -> "Reopened — moved back to $label" },
    itsCategory = "its category",
    deletedNote = { title -> "Deleted \"$title\"" },

    channelName = "Note reminders",
    channelDescription = "Reminders for notes with a scheduled time",
    reminderFallbackTitle = "Reminder",
    tapToOpen = "Tap to open",

    backup = "Backup",
    backupHeading = "GOOGLE DRIVE BACKUP",
    signInWithGoogle = "Sign in with Google",
    signInExplainer = "Your notes are kept on this phone only. Sign in to keep a copy in your own Google Drive.",
    signedInAs = "Signed in as",
    signOut = "Sign out",
    backUpNow = "Back up now",
    backingUp = "Backing up…",
    restore = "Restore from Drive",
    restoring = "Restoring…",
    lastBackup = { when_ -> "Last backup: ${'$'}when_" },
    neverBackedUp = "Not backed up yet",
    backupDone = { n -> "Backed up ${'$'}n note${'$'}{if (n == 1) "" else "s"}" },
    audioUploaded = { n ->
        if (n == 0) "Notes and recordings are up to date"
        else "${'$'}n new recording${'$'}{if (n == 1) "" else "s"} uploaded"
    },
    restoreDone = { n -> "Restored ${'$'}n note${'$'}{if (n == 1) "" else "s"}" },
    noBackupFound = "No backup in Drive yet",
    backupFailed = "Backup failed",
    signInFailed = "Sign-in failed",
    signInNeeded = "Sign in again to reach Drive",
    restoreWarningTitle = "Restore from Drive?",
    restoreWarningBody = "Everything on this phone is replaced by the backup. Notes written since the last backup will be lost.",
    restoreConfirm = "Replace and restore",
    autoBackupNote = "Backs up on its own when you leave the app, at most once every 15 minutes.",
    driveFolderNote = "Saved in a \"Hanooot Notes Backup\" folder you can open in Drive yourself.",

    duePattern = "EEE, MMM d · h:mm a",
    timePattern = "h:mm a",
    monthDayPattern = "MMM d",
    monthYearPattern = "LLLL yyyy",
    dayHeadingPattern = "EEEE, MMMM d"
)

val ArabicStrings = Strings(
    lang = Lang.AR,
    locale = Lang.AR.locale,

    appTitle = "ملاحظاتي",
    notesCount = { n ->
        arPlural(n, "ملاحظة واحدة", "ملاحظتان", "# ملاحظات", "# ملاحظة")
    },
    all = "الكل",
    search = "بحث",
    searchHint = "ابحث في الملاحظات…",
    clear = "مسح",
    theme = "المظهر",
    newNote = "ملاحظة جديدة",
    emptyCategory = "لا توجد ملاحظات في هذا التصنيف",
    noMatches = { q -> "لا نتائج لـ «$q»" },
    selectNoteHint = "اختر ملاحظة، أو اضغط + لكتابة ملاحظة جديدة",

    add = "إضافة",
    cancel = "إلغاء",
    close = "إغلاق",
    save = "حفظ",
    delete = "حذف",
    remove = "إزالة",
    undo = "تراجع",
    back = "رجوع",
    pin = "تثبيت",

    deleteCategoryTitle = { label -> "حذف «$label»؟" },
    categoryHoldsNotes = { n ->
        val notes = arPlural(n, "ملاحظته", "ملاحظتاه", "# ملاحظات", "# ملاحظة")
        "ستُنقل $notes إلى تصنيف آخر ولن تُحذف."
    },
    categoryEmpty = "هذا التصنيف لا يحتوي على ملاحظات.",
    newCategory = "تصنيف جديد",
    categoryNameHint = "مثلاً: أفكار",
    colourHeading = "اللون",
    multicolourNote = "يظهر عند تفعيل مظهر «متعدد الألوان».",
    iconHeading = "الأيقونة",
    enterName = "الرجاء إدخال اسم.",
    categoryExists = "هذا التصنيف موجود مسبقاً.",

    appearance = "المظهر",
    languageHeading = "اللغة",
    accentHeading = "اللون الأساسي",
    themeName = { key ->
        when (key) {
            "red" -> "أسود وأحمر"
            "orange" -> "أسود وبرتقالي"
            "amber" -> "أسود وعنبري"
            "green" -> "أسود وأخضر"
            "blue" -> "أسود وأزرق"
            "purple" -> "أسود وبنفسجي"
            else -> "متعدد الألوان"
        }
    },

    catWork = "العمل",
    catPersonal = "شخصي",
    catWife = "الزوجة",
    catOther = "أخرى",
    catDone = "منجَز",

    titleHint = "العنوان",
    bodyHint = "اكتب المزيد…",
    addItem = "+ إضافة بند",
    listItemHint = "بند",
    addToCalendar = "إضافة إلى التقويم",
    shareIcs = "مشاركة كملف ‎.ics",
    recordMemo = "تسجيل مذكرة صوتية",
    voiceMemoTitle = "مذكرة صوتية",
    micBlocked = "الميكروفون محظور",
    micBlockedDetail = "فعّله من الإعدادات لتسجيل المذكرات",
    cantRecord = "تعذّر بدء التسجيل",
    alreadyAdded = "مضافة مسبقاً",
    alreadyAddedDetail = "هذه المهام موجودة في القائمة",
    tasksAdded = { n ->
        arPlural(n, "أُضيفت مهمة واحدة", "أُضيفت مهمتان", "أُضيفت # مهام", "أُضيفت # مهمة")
    },
    fromVoiceMemo = "من مذكرتك الصوتية",

    addDateReminder = "إضافة تاريخ وتنبيه",
    removeDate = "إزالة التاريخ",
    remindMe = "نبّهني",
    reminderOptions = listOf(
        "بدون" to null,
        "في الوقت نفسه" to 0,
        "قبل 5 دقائق" to 5,
        "قبل 15 دقيقة" to 15,
        "قبل ساعة" to 60,
        "قبل يوم" to 1440
    ),

    play = "تشغيل",
    pause = "إيقاف مؤقت",
    deleteRecording = "حذف التسجيل",
    transcriptHeading = "النص المكتوب",
    editTranscript = "تعديل النص",
    addTasksToList = { n ->
        arPlural(
            n,
            "إضافة مهمة واحدة إلى القائمة",
            "إضافة مهمتين إلى القائمة",
            "إضافة # مهام إلى القائمة",
            "إضافة # مهمة إلى القائمة"
        )
    },
    noTasksFound = "لم يُعثر على مهام",

    untitled = "بلا عنوان",
    recordingsCount = { n ->
        arPlural(n, "تسجيل واحد", "تسجيلان", "# تسجيلات", "# تسجيلاً")
    },
    allItemsDone = "أُنجزت كل البنود",
    itemsLeft = { n ->
        arPlural(n, "بند واحد متبقٍّ", "بندان متبقيان", "# بنود متبقية", "# بنداً متبقياً")
    },
    today = "اليوم",
    tomorrow = "غداً",

    prevMonth = "الشهر السابق",
    nextMonth = "الشهر التالي",
    todayButton = "اليوم",
    nothingScheduled = "لا شيء مجدول.",
    // Sunday first, matching the calendar grid: الأحد … السبت
    weekdayInitials = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س"),

    movedToDone = "أُنجزت — نُقلت إلى «منجَز»",
    reopenedInto = { label -> "أُعيد فتحها — رجعت إلى «$label»" },
    itsCategory = "تصنيفها",
    deletedNote = { title -> "حُذفت «$title»" },

    channelName = "تنبيهات الملاحظات",
    channelDescription = "تنبيهات الملاحظات التي لها وقت محدد",
    reminderFallbackTitle = "تنبيه",
    tapToOpen = "اضغط للفتح",

    backup = "النسخ الاحتياطي",
    backupHeading = "النسخ الاحتياطي على Google Drive",
    signInWithGoogle = "تسجيل الدخول بحساب Google",
    signInExplainer = "ملاحظاتك محفوظة على هذا الهاتف فقط. سجّل الدخول ليُحفظ نسخة منها في Google Drive الخاص بك.",
    signedInAs = "مسجَّل الدخول بحساب",
    signOut = "تسجيل الخروج",
    backUpNow = "انسخ الآن",
    backingUp = "جارٍ النسخ…",
    restore = "استعادة من Drive",
    restoring = "جارٍ الاستعادة…",
    lastBackup = { when_ -> "آخر نسخة: ${'$'}when_" },
    neverBackedUp = "لا توجد نسخة احتياطية بعد",
    backupDone = { n ->
        arPlural(n, "نُسخت ملاحظة واحدة", "نُسخت ملاحظتان", "نُسخت # ملاحظات", "نُسخت # ملاحظة")
    },
    audioUploaded = { n ->
        if (n == 0) "الملاحظات والتسجيلات محدَّثة"
        else arPlural(n, "رُفع تسجيل واحد جديد", "رُفع تسجيلان جديدان", "رُفعت # تسجيلات جديدة", "رُفع # تسجيلاً جديداً")
    },
    restoreDone = { n ->
        arPlural(n, "استُعيدت ملاحظة واحدة", "استُعيدت ملاحظتان", "استُعيدت # ملاحظات", "استُعيدت # ملاحظة")
    },
    noBackupFound = "ما توجد نسخة احتياطية في Drive",
    backupFailed = "فشل النسخ الاحتياطي",
    signInFailed = "فشل تسجيل الدخول",
    signInNeeded = "سجّل الدخول مرة أخرى للوصول إلى Drive",
    restoreWarningTitle = "استعادة من Drive؟",
    restoreWarningBody = "سيُستبدل كل ما على هذا الهاتف بالنسخة الاحتياطية. الملاحظات المكتوبة بعد آخر نسخة ستُفقد.",
    restoreConfirm = "استبدل واستعد",
    autoBackupNote = "ينسخ تلقائياً عند خروجك من التطبيق، بحد أقصى مرة كل 15 دقيقة.",
    driveFolderNote = "يُحفظ في مجلد «Hanooot Notes Backup» تگدر تفتحه في Drive بنفسك.",

    duePattern = "EEE، d MMM · h:mm a",
    timePattern = "h:mm a",
    monthDayPattern = "d MMM",
    monthYearPattern = "LLLL yyyy",
    dayHeadingPattern = "EEEE، d MMMM"
)

fun stringsFor(lang: Lang): Strings = if (lang == Lang.AR) ArabicStrings else EnglishStrings

/** The language changes rarely, so readers need no recomposition tracking. */
val LocalStrings = staticCompositionLocalOf { EnglishStrings }

/**
 * The same table, reachable outside composition — notification text is built
 * in a receiver and a scheduler, where there is no composition to read from.
 * Kept in step by the view model and, after a reboot, by [BootReceiver].
 */
object AppStrings {
    @Volatile
    var current: Strings = EnglishStrings
}

/**
 * Built-in categories carry English labels in the database, because the label
 * is seeded once and the language can change afterwards. Translating by key
 * keeps both languages correct without migrating rows.
 */
fun Strings.labelOf(category: Category?): String = when (category?.key) {
    null -> ""
    Category.WORK_KEY -> catWork
    Category.PERSONAL_KEY -> catPersonal
    Category.WIFE_KEY -> catWife
    Category.OTHER_KEY -> catOther
    Category.DONE_KEY -> catDone
    else -> category.label
}

/** As [labelOf], but a null category is the "All" tab rather than nothing. */
fun Strings.tabLabelOf(category: Category?): String =
    if (category == null) all else labelOf(category)

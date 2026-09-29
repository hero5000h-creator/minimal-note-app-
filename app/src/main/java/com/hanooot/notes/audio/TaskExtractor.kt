package com.hanooot.notes.audio

/**
 * Turns free-form dictation into separate checklist items.
 *
 * This is rule-based, not a language model: it splits on the connectors people
 * actually speak and strips the filler that shows up in speech but not in
 * writing. It handles English and Arabic. It is fast, offline and private —
 * but it does not understand context the way an LLM would, so it can't tell
 * "call Ahmed" (a task) from "Ahmed called yesterday" (not one).
 */
object TaskExtractor {

    private val splitPatterns = listOf(
        Regex("""\b(?:and then|after that|then also|then|also|next|plus|as well as|after which)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""\s(?:ثم|بعدين|وبعدها|وبعدين|كذلك|أيضا|أيضاً|وأيضا)\s""")
    )

    private val leadFiller = listOf(
        Regex("""^(?:um+|uh+|er+|okay|ok|so|well|like|right)\b[,\s]*""", RegexOption.IGNORE_CASE),
        Regex("""^(?:i need to|i have to|i must|i should|i want to|i've got to|ive got to)\s+""",
            RegexOption.IGNORE_CASE),
        Regex("""^(?:remember to|don'?t forget to|make sure to|make sure i|be sure to)\s+""",
            RegexOption.IGNORE_CASE),
        Regex("""^(?:let'?s|lets)\s+""", RegexOption.IGNORE_CASE),
        Regex("""^(?:please)\s+""", RegexOption.IGNORE_CASE),
        Regex("""^(?:لازم|يجب|اريد|أريد|محتاج|تذكر|لا تنسى)\s+""")
    )

    private val ordinals = Regex(
        """^(?:first(?:ly)?|second(?:ly)?|third(?:ly)?|fourth(?:ly)?|fifth(?:ly)?|number\s+\d+|\d+[.)])\s*[,:]?\s*""",
        RegexOption.IGNORE_CASE
    )

    private val sentenceBreak = Regex("""(?<=[.!?؟])\s+|\n+""")
    private val leadingConj = Regex("""^(?:and|or|but|و)\s+""", RegexOption.IGNORE_CASE)
    private val trailingConj = Regex("""\s+(?:and|or|but|و)$""", RegexOption.IGNORE_CASE)

    private fun clean(raw: String): String {
        var t = raw.trim()
        if (t.isEmpty()) return ""

        t = ordinals.replace(t, "")

        // Filler stacks up ("okay so I need to..."), so strip repeatedly
        var changed = true
        while (changed) {
            changed = false
            for (re in leadFiller) {
                val next = re.replace(t, "")
                if (next != t) { t = next; changed = true }
            }
        }

        t = t.trim().trim(',', ';', ':', '.', '-', '–', '—', ' ')
        // Splitting can leave a dangling conjunction at either end
        t = leadingConj.replace(t, "")
        t = trailingConj.replace(t, "")
        t = t.trim().trim(',', ';', ':', '.', ' ')
        if (t.isEmpty()) return ""

        // Sentence case, without lowercasing something already capitalised
        if (t.first().isLowerCase()) t = t.replaceFirstChar { it.uppercase() }
        return t
    }

    fun extract(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        var parts = text.split(sentenceBreak)
        for (re in splitPatterns) {
            parts = parts.flatMap { it.split(re) }
        }

        // A comma list ("milk, bread and eggs") is only split when the whole
        // dictation is short; otherwise commas are ordinary punctuation.
        if (parts.size == 1 && text.length < 140 && text.contains(',')) {
            parts = parts[0].split(Regex("""\s*,\s*|\s+and\s+|\s+و(?=\S)"""))
        }

        val seen = mutableSetOf<String>()
        return parts
            .map(::clean)
            .filter { it.length >= 2 && seen.add(it.lowercase()) }
            .take(25)
    }
}

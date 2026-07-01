package com.dynamicbookreader.utils

/**
 * Parses a [com.dynamicbookreader.data.model.Chapter.content] string into
 * renderable pieces, and — when present — detects a leading "সূচিপত্র"
 * (table of contents) block so the Reading screen can show a collapsible
 * jump-to-section table.
 *
 * ── Detection rule ───────────────────────────────────────────────────────
 * Many chapters in this book start with a block of short lines (section
 * headings) that are later repeated **verbatim** as inline headings inside
 * the body paragraphs. We treat the leading run of short lines as a
 * candidate table of contents, then confirm each candidate by checking
 * whether that exact line reappears later in the content. Only confirmed
 * lines become clickable ToC entries; if fewer than [MIN_CONFIRMED_ENTRIES]
 * lines are confirmed, no ToC is shown at all and the content renders as
 * plain paragraphs (nothing is hidden or altered).
 */
object ChapterContentParser {

    /** Leading lines longer than this are treated as body text, not headings. */
    private const val MAX_HEADING_LENGTH = 120

    /** Need at least this many confirmed repeats to justify showing a ToC. */
    private const val MIN_CONFIRMED_ENTRIES = 2

    /** Matches inline footnote markers like {{note:1}} or {{note:abc}}. */
    private val FOOTNOTE_MARKER_REGEX = Regex("""\{\{note:([^}]+)\}\}""")

    /**
     * A piece of a paragraph's text — either plain prose or a reference to
     * a footnote (by [key], matching [com.dynamicbookreader.data.model.Footnote.key]).
     * A paragraph with no `{{note:...}}` markers parses to a single [Plain] segment.
     */
    sealed class TextSegment {
        data class Plain(val text: String) : TextSegment()
        data class FootnoteRef(val key: String, val displayNumber: Int) : TextSegment()
    }

    /**
     * A single renderable paragraph. [headingKey] is non-null when this
     * exact paragraph text matches a confirmed ToC entry — i.e. this is
     * the in-body heading a ToC row should scroll to. [segments] is the
     * paragraph text broken into plain-text and footnote-reference pieces.
     */
    data class ContentBlock(
        val segments: List<TextSegment>,
        val headingKey: String? = null
    ) {
        /** Plain-text fallback (footnote markers stripped) — used for ToC matching, previews, etc. */
        val plainText: String
            get() = segments.joinToString("") { if (it is TextSegment.Plain) it.text else "" }
    }

    data class ParsedChapter(
        /** Confirmed table-of-contents entries, in original order. Empty if none detected. */
        val tocEntries: List<String>,
        /** All renderable paragraphs, in reading order (ToC block lines are excluded — see note below). */
        val blocks: List<ContentBlock>
    )

    /**
     * Parses [rawContent] into a [ParsedChapter].
     *
     * @param chapterTitle the chapter's own title (from JSON `title`). When
     *   this exact line appears near the top of the content, it marks the
     *   end of the leading ToC block — everything after it is body text.
     *   This is more reliable than a length threshold alone, since some
     *   heading lines can be as short as the title itself.
     *
     * If a ToC block is detected, its lines are excluded from [blocks]
     * (they'd otherwise appear twice — once as the ToC listing, once as
     * the in-body heading). If no ToC is detected, [blocks] contains every
     * non-blank line untouched, exactly as before.
     *
     * Footnote markers (`{{note:KEY}}`) are parsed out of every paragraph
     * (ToC detection uses the marker-stripped plain text, so markers never
     * interfere with heading matching) and numbered in first-appearance
     * order across the whole chapter, matching how printed footnotes work.
     */
    fun parse(rawContent: String, chapterTitle: String? = null): ParsedChapter {
        val rawLines = rawContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return ParsedChapter(emptyList(), emptyList())

        // Strip footnote markers to get the plain-text version of each line
        // for ToC candidate/confirmation matching — a heading should match
        // even if one of its occurrences happens to carry a footnote.
        val plainLines = rawLines.map { stripFootnoteMarkers(it) }

        // Optional literal "সূচিপত্র" label as the very first line — if present,
        // it's a strong signal but not required (some chapters may omit it).
        var startIndex = 0
        if (plainLines.first() == "সূচিপত্র") startIndex = 1

        // Collect the leading run of candidate heading lines (matched on
        // plain text). Two stop conditions, whichever comes first:
        //  1. A line exceeds MAX_HEADING_LENGTH (clearly body prose), or
        //  2. The chapter's own title line is encountered.
        val candidates = mutableListOf<String>()
        var i = startIndex
        while (i < plainLines.size) {
            val plain = plainLines[i]
            if (chapterTitle != null && plain == chapterTitle.trim()) {
                i++
                break
            }
            if (plain.length > MAX_HEADING_LENGTH) break
            candidates.add(plain)
            i++
        }

        val bodyStartIndex = i
        val bodyPlainLines = plainLines.subList(bodyStartIndex, plainLines.size)
        val bodyRawLines = rawLines.subList(bodyStartIndex, rawLines.size)

        // Confirm each candidate: does it reappear verbatim later in the body?
        val confirmed = candidates.filter { candidate ->
            bodyPlainLines.any { it == candidate }
        }

        // Running counter for footnote display numbers, assigned in the
        // order markers are first encountered across the chapter.
        val footnoteNumbers = LinkedHashMap<String, Int>()

        fun nextFootnoteBlock(rawLine: String, headingKey: String? = null): ContentBlock {
            val segments = splitIntoSegments(rawLine, footnoteNumbers)
            return ContentBlock(segments = segments, headingKey = headingKey)
        }

        if (confirmed.size < MIN_CONFIRMED_ENTRIES) {
            // Not a real ToC — render everything as plain paragraphs, untouched.
            return ParsedChapter(
                tocEntries = emptyList(),
                blocks = rawLines.map { nextFootnoteBlock(it) }
            )
        }

        val confirmedSet = confirmed.toHashSet()
        val occurrenceCounter = HashMap<String, Int>()

        val blocks = bodyRawLines.mapIndexed { idx, rawLine ->
            val plain = bodyPlainLines[idx]
            if (plain in confirmedSet) {
                val occurrence = occurrenceCounter.getOrDefault(plain, 0)
                occurrenceCounter[plain] = occurrence + 1
                nextFootnoteBlock(rawLine, headingKey(plain, occurrence))
            } else {
                nextFootnoteBlock(rawLine)
            }
        }

        return ParsedChapter(tocEntries = confirmed, blocks = blocks)
    }

    /** Removes `{{note:...}}` markers, leaving plain prose (used for ToC matching, and available for preview text elsewhere in the app). */
    fun stripFootnoteMarkers(line: String): String =
        FOOTNOTE_MARKER_REGEX.replace(line, "").trim()

    /**
     * Splits [rawLine] into [TextSegment]s, extracting footnote markers and
     * assigning each a running display number via [footnoteNumbers] (shared
     * across the whole chapter so numbering is sequential, e.g. ¹ ² ³…).
     */
    private fun splitIntoSegments(
        rawLine: String,
        footnoteNumbers: LinkedHashMap<String, Int>
    ): List<TextSegment> {
        val matches = FOOTNOTE_MARKER_REGEX.findAll(rawLine).toList()
        if (matches.isEmpty()) return listOf(TextSegment.Plain(rawLine))

        val segments = mutableListOf<TextSegment>()
        var cursor = 0
        for (match in matches) {
            if (match.range.first > cursor) {
                segments.add(TextSegment.Plain(rawLine.substring(cursor, match.range.first)))
            }
            val key = match.groupValues[1].trim()
            val number = footnoteNumbers.getOrPut(key) { footnoteNumbers.size + 1 }
            segments.add(TextSegment.FootnoteRef(key = key, displayNumber = number))
            cursor = match.range.last + 1
        }
        if (cursor < rawLine.length) {
            segments.add(TextSegment.Plain(rawLine.substring(cursor)))
        }
        return segments
    }

    /**
     * Stable, deterministic key derived from heading text + occurrence index
     * (used for LazyColumn item keys and scroll targeting). [occurrence] is
     * 0 for the first time this exact heading text appears in the body,
     * 1 for the second, etc. — this keeps keys unique even when the same
     * section title legitimately appears more than once.
     */
    fun headingKey(heading: String, occurrence: Int = 0): String =
        "heading_${heading.hashCode()}_$occurrence"
}

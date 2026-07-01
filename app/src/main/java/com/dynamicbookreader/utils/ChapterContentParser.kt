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

    /**
     * A single renderable paragraph. [headingKey] is non-null when this
     * exact paragraph text matches a confirmed ToC entry — i.e. this is
     * the in-body heading a ToC row should scroll to.
     */
    data class ContentBlock(val text: String, val headingKey: String? = null)

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
     */
    fun parse(rawContent: String, chapterTitle: String? = null): ParsedChapter {
        val allLines = rawContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (allLines.isEmpty()) return ParsedChapter(emptyList(), emptyList())

        // Optional literal "সূচিপত্র" label as the very first line — if present,
        // it's a strong signal but not required (some chapters may omit it).
        var startIndex = 0
        if (allLines.first() == "সূচিপত্র") startIndex = 1

        // Collect the leading run of candidate heading lines. Two stop
        // conditions, whichever comes first:
        //  1. A line exceeds MAX_HEADING_LENGTH (clearly body prose), or
        //  2. The chapter's own title line is encountered — since the title
        //     is repeated verbatim right before the body starts, and is
        //     itself short, a length check alone would wrongly swallow it
        //     (and the heading line immediately following it) into the
        //     candidate block.
        val candidates = mutableListOf<String>()
        var i = startIndex
        while (i < allLines.size) {
            val line = allLines[i]
            if (chapterTitle != null && line == chapterTitle.trim()) {
                i++ // consume the title line itself, then stop collecting
                break
            }
            if (line.length > MAX_HEADING_LENGTH) break
            candidates.add(line)
            i++
        }

        val bodyStartIndex = i
        val bodyLines = allLines.subList(bodyStartIndex, allLines.size)

        // Confirm each candidate: does it reappear verbatim later in the body?
        val confirmed = candidates.filter { candidate ->
            bodyLines.any { it == candidate }
        }

        if (confirmed.size < MIN_CONFIRMED_ENTRIES) {
            // Not a real ToC — render everything as plain paragraphs, untouched.
            return ParsedChapter(
                tocEntries = emptyList(),
                blocks = allLines.map { ContentBlock(it) }
            )
        }

        // Build a lookup for quick "is this line a confirmed heading?" checks
        // while walking the body, so each heading paragraph gets a stable
        // key the UI can scroll to. A heading's exact text CAN legitimately
        // repeat in the body (e.g. used as a section divider twice), so we
        // disambiguate by occurrence count, not just text — otherwise two
        // headings would collide on the same LazyColumn item key.
        val confirmedSet = confirmed.toHashSet()
        val occurrenceCounter = HashMap<String, Int>()

        val blocks = bodyLines.map { line ->
            if (line in confirmedSet) {
                val occurrence = occurrenceCounter.getOrDefault(line, 0)
                occurrenceCounter[line] = occurrence + 1
                ContentBlock(text = line, headingKey = headingKey(line, occurrence))
            } else {
                ContentBlock(text = line)
            }
        }

        return ParsedChapter(tocEntries = confirmed, blocks = blocks)
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

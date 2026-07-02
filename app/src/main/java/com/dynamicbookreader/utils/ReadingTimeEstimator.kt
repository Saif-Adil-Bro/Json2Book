package com.dynamicbookreader.utils

/**
 * Estimates reading time from a chapter's word count.
 *
 * Uses ~180 words/minute as an average silent-reading speed for Bengali
 * prose (slightly below the commonly-cited 200-250 wpm for English, since
 * Bengali script + religious/historical vocabulary tends to read a bit
 * slower). This is a rough estimate for UI purposes only — not meant to be
 * precise, just useful ("~10 minutes left" style hints).
 */
object ReadingTimeEstimator {

    private const val WORDS_PER_MINUTE = 180

    /** Counts words by splitting on whitespace — good enough for Bengali/Latin mixed text. */
    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    /** Total estimated minutes to read [text] start to finish (minimum 1). */
    fun totalMinutes(text: String): Int {
        val words = wordCount(text)
        return (words / WORDS_PER_MINUTE).coerceAtLeast(1)
    }

    /**
     * Estimated minutes *remaining* given [progressFraction] (0f..1f) of the
     * chapter already read. Returns 0 once progress reaches/exceeds 1f.
     */
    fun remainingMinutes(text: String, progressFraction: Float): Int {
        val total = totalMinutes(text)
        val remainingFraction = (1f - progressFraction).coerceIn(0f, 1f)
        return kotlin.math.ceil(total * remainingFraction).toInt()
    }
}

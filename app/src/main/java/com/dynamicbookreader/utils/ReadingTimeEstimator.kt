package com.dynamicbookreader.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Estimates reading time from a chapter's word count.
 *
 * Uses ~180 words/minute as an average silent-reading speed for Bengali
 * prose. Implements zero-allocation character scanning and in-memory
 * caching per chapter so scrolling chapter lists never lags.
 */
object ReadingTimeEstimator {

    private const val WORDS_PER_MINUTE = 180

    // In-memory cache keyed by chapter number for instant O(1) lookups
    private val totalMinutesCache = ConcurrentHashMap<Int, Int>()

    /**
     * Counts words by scanning characters directly without allocating intermediate
     * string arrays or running regex engines over large text blocks.
     */
    fun wordCount(text: String): Int {
        var count = 0
        var inWord = false
        val len = text.length
        for (i in 0 until len) {
            val c = text[i]
            if (c <= ' ' || Character.isWhitespace(c)) {
                inWord = false
            } else if (!inWord) {
                inWord = true
                count++
            }
        }
        return count
    }

    /**
     * Total estimated minutes for [chapterNo], cached in memory.
     * Subsequent calls return instantly in O(1) without re-scanning.
     */
    fun totalMinutes(chapterNo: Int, text: String): Int {
        return totalMinutesCache.getOrPut(chapterNo) {
            val words = wordCount(text)
            (words / WORDS_PER_MINUTE).coerceAtLeast(1)
        }
    }

    /** Total estimated minutes to read [text] start to finish (minimum 1). */
    fun totalMinutes(text: String): Int {
        val words = wordCount(text)
        return (words / WORDS_PER_MINUTE).coerceAtLeast(1)
    }

    /**
     * Estimated minutes *remaining* computed directly from precomputed [totalMinutes].
     * Zero text-processing or re-counting required!
     */
    fun remainingMinutes(totalMinutes: Int, progressFraction: Float): Int {
        val remainingFraction = (1f - progressFraction).coerceIn(0f, 1f)
        return kotlin.math.ceil(totalMinutes * remainingFraction).toInt()
    }

    /**
     * Estimated minutes *remaining* given [progressFraction] (0f..1f) of the
     * chapter already read. Returns 0 once progress reaches/exceeds 1f.
     */
    fun remainingMinutes(text: String, progressFraction: Float): Int {
        val total = totalMinutes(text)
        return remainingMinutes(total, progressFraction)
    }

    /** Pre-warm or clear cache */
    fun clearCache() {
        totalMinutesCache.clear()
    }
}

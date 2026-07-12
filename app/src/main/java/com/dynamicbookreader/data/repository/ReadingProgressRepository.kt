package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_progress"
)

/**
 * Represents how far the user has read (the single most-recent chapter —
 * used for the Home screen's "continue reading" shortcut card).
 *
 * @param chapterNo last-opened chapter number, or null if nothing read yet
 * @param scrollFraction 0f..1f — how far down that chapter the user had scrolled
 * @param chapterTitle cached title so the "continue reading" UI doesn't need
 *   to look the chapter back up before it can render
 * @param updatedAtMillis when this was last saved (System.currentTimeMillis())
 */
data class ReadingProgress(
    val chapterNo: Int?,
    val scrollFraction: Float,
    val chapterTitle: String?,
    val updatedAtMillis: Long
) {
    val hasProgress: Boolean get() = chapterNo != null
}

/** Serializable per-chapter progress entry, stored in a JSON-encoded map. */
@Serializable
private data class ChapterProgressEntry(
    val scrollFraction: Float,
    val updatedAtMillis: Long
)

/**
 * Persists reading position — both a single "last read anywhere" slot (for
 * the Home screen's continue-reading shortcut) and per-chapter progress (so
 * every chapter card on Home can show its own progress ring), independent
 * of [ReadingPreferencesRepository], which only stores display preferences
 * (font, theme, etc).
 */
class ReadingProgressRepository(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_CHAPTER_NO = intPreferencesKey("progress_chapter_no")
        private val KEY_SCROLL_FRACTION = floatPreferencesKey("progress_scroll_fraction")
        private val KEY_CHAPTER_TITLE = stringPreferencesKey("progress_chapter_title")
        private val KEY_UPDATED_AT = longPreferencesKey("progress_updated_at")

        /** JSON-encoded Map<String /* chapterNo */, ChapterProgressEntry>. */
        private val KEY_PER_CHAPTER_MAP = stringPreferencesKey("progress_per_chapter_json")

        /** JSON-encoded Map<String /* chapterNo */, List<String /* headingKey */>>. */
        private val KEY_READ_HEADINGS_MAP = stringPreferencesKey("progress_read_headings_json")
    }

    // ── Single "last read" slot (continue-reading shortcut) ──────────────────

    val readingProgress: Flow<ReadingProgress> = appContext.progressDataStore.data.map { prefs ->
        ReadingProgress(
            chapterNo = prefs[KEY_CHAPTER_NO],
            scrollFraction = prefs[KEY_SCROLL_FRACTION] ?: 0f,
            chapterTitle = prefs[KEY_CHAPTER_TITLE],
            updatedAtMillis = prefs[KEY_UPDATED_AT] ?: 0L
        )
    }

    // ── Per-chapter progress map (Home screen progress rings) ────────────────

    /** Map of chapterNo -> scrollFraction (0f..1f), for every chapter with any saved progress. */
    val perChapterProgress: Flow<Map<Int, Float>> = appContext.progressDataStore.data.map { prefs ->
        decodeMap(prefs[KEY_PER_CHAPTER_MAP])
            .mapNotNull { (key, entry) -> key.toIntOrNull()?.let { it to entry.scrollFraction } }
            .toMap()
    }

    // ── Per-chapter read-heading tracking (Home screen sub-section checkmarks) ──

    /**
     * Map of chapterNo -> set of headingKeys the user has scrolled past while
     * reading that chapter. Powers the read/unread icon next to each
     * sub-section in the Home screen's expanded chapter card.
     */
    val perChapterReadHeadings: Flow<Map<Int, Set<String>>> = appContext.progressDataStore.data.map { prefs ->
        decodeHeadingsMap(prefs[KEY_READ_HEADINGS_MAP])
    }

    /**
     * Marks a single heading as "read" for the given chapter — called from
     * the Reading screen once the user has scrolled past that heading's
     * section. Merges into any existing read-set for the chapter rather
     * than overwriting it, and is safe to call repeatedly for the same
     * heading (idempotent).
     */
    suspend fun markHeadingRead(chapterNo: Int, headingKey: String) {
        appContext.progressDataStore.edit { prefs ->
            val current = decodeHeadingsMap(prefs[KEY_READ_HEADINGS_MAP]).toMutableMap()
            val existingSet = current[chapterNo] ?: emptySet()
            if (headingKey in existingSet) return@edit  // no-op, already recorded
            current[chapterNo] = existingSet + headingKey
            prefs[KEY_READ_HEADINGS_MAP] = json.encodeToString(
                MapSerializer(Int.serializer(), SetSerializer(String.serializer())),
                current
            )
        }
    }

    /**
     * Saves the current reading position — updates both the single
     * "last read" slot and this chapter's entry in the per-chapter map.
     * Called periodically (debounced) while scrolling the Reading screen,
     * and once more when leaving it.
     */
    suspend fun saveProgress(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        val clamped = scrollFraction.coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        appContext.progressDataStore.edit { prefs ->
            // Single-slot "last read"
            prefs[KEY_CHAPTER_NO] = chapterNo
            prefs[KEY_CHAPTER_TITLE] = chapterTitle
            prefs[KEY_SCROLL_FRACTION] = clamped
            prefs[KEY_UPDATED_AT] = now

            // Per-chapter map
            val current = decodeMap(prefs[KEY_PER_CHAPTER_MAP]).toMutableMap()
            current[chapterNo.toString()] = ChapterProgressEntry(clamped, now)
            prefs[KEY_PER_CHAPTER_MAP] = json.encodeToString(
                MapSerializer(String.serializer(), ChapterProgressEntry.serializer()),
                current
            )
        }
    }

    /** Clears saved progress (e.g. if the user wants to start over). */
    suspend fun clearProgress() {
        appContext.progressDataStore.edit { prefs ->
            prefs.remove(KEY_CHAPTER_NO)
            prefs.remove(KEY_SCROLL_FRACTION)
            prefs.remove(KEY_CHAPTER_TITLE)
            prefs.remove(KEY_UPDATED_AT)
            prefs.remove(KEY_PER_CHAPTER_MAP)
            prefs.remove(KEY_READ_HEADINGS_MAP)
        }
    }

    private fun decodeMap(raw: String?): Map<String, ChapterProgressEntry> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString(
                MapSerializer(String.serializer(), ChapterProgressEntry.serializer()),
                raw
            )
        } catch (e: Exception) {
            // Corrupted/old-format data should never crash the app — just
            // treat it as empty and let saves overwrite it going forward.
            emptyMap()
        }
    }

    private fun decodeHeadingsMap(raw: String?): Map<Int, Set<String>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString(
                MapSerializer(Int.serializer(), SetSerializer(String.serializer())),
                raw
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

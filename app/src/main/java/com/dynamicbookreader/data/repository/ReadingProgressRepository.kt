package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_progress"
)

/**
 * Represents how far the user has read.
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

/**
 * Persists "last read" position so the user can jump straight back to
 * where they left off — independent of [ReadingPreferencesRepository],
 * which only stores display preferences (font, theme, etc).
 */
class ReadingProgressRepository(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val KEY_CHAPTER_NO = intPreferencesKey("progress_chapter_no")
        private val KEY_SCROLL_FRACTION = floatPreferencesKey("progress_scroll_fraction")
        private val KEY_CHAPTER_TITLE = androidx.datastore.preferences.core.stringPreferencesKey("progress_chapter_title")
        private val KEY_UPDATED_AT = longPreferencesKey("progress_updated_at")
    }

    val readingProgress: Flow<ReadingProgress> = appContext.progressDataStore.data.map { prefs ->
        ReadingProgress(
            chapterNo = prefs[KEY_CHAPTER_NO],
            scrollFraction = prefs[KEY_SCROLL_FRACTION] ?: 0f,
            chapterTitle = prefs[KEY_CHAPTER_TITLE],
            updatedAtMillis = prefs[KEY_UPDATED_AT] ?: 0L
        )
    }

    /**
     * Saves the current reading position. Called periodically (debounced)
     * while scrolling the Reading screen, and once more when leaving it.
     */
    suspend fun saveProgress(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        appContext.progressDataStore.edit { prefs ->
            prefs[KEY_CHAPTER_NO] = chapterNo
            prefs[KEY_CHAPTER_TITLE] = chapterTitle
            prefs[KEY_SCROLL_FRACTION] = scrollFraction.coerceIn(0f, 1f)
            prefs[KEY_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    /** Clears saved progress (e.g. if the user wants to start over). */
    suspend fun clearProgress() {
        appContext.progressDataStore.edit { prefs ->
            prefs.remove(KEY_CHAPTER_NO)
            prefs.remove(KEY_SCROLL_FRACTION)
            prefs.remove(KEY_CHAPTER_TITLE)
            prefs.remove(KEY_UPDATED_AT)
        }
    }
}

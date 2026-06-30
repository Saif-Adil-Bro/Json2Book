package com.dynamicbookreader.data.repository

import android.content.Context
import com.dynamicbookreader.data.model.BookData
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.utils.JsonParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository: single source of truth for all book data.
 *
 * The ViewModel never touches JsonParser directly; it always
 * goes through this repository. This makes it trivial to swap
 * the data source later (e.g. remote API, Room DB) without
 * touching UI code.
 *
 * ── Caching ───────────────────────────────────────────────────────────────
 * `book_data.json` is parsed once and cached in memory for the lifetime of
 * the process. Subsequent calls (e.g. opening a chapter from the list,
 * navigating back to Home) return instantly from cache instead of
 * re-reading + re-parsing the asset file every time.
 *
 * A [Mutex] guards the cache so concurrent calls (e.g. fast double-tap on a
 * chapter) don't trigger duplicate parses.
 */
class BookRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheMutex = Mutex()

    @Volatile
    private var cachedBookData: BookData? = null

    /**
     * Loads the full [BookData], using the in-memory cache when available.
     *
     * @param forceRefresh if true, bypasses the cache and re-reads the
     *   asset file (used by the "retry" action after a load failure).
     */
    suspend fun getBookData(forceRefresh: Boolean = false): JsonParser.Result<BookData> {
        // Fast path: cache hit, no lock needed for the read.
        cachedBookData?.let { cached ->
            if (!forceRefresh) return JsonParser.Result.Success(cached)
        }

        // Slow path: parse (or re-parse) under the lock so concurrent
        // callers don't duplicate work.
        return cacheMutex.withLock {
            // Re-check inside the lock — another coroutine may have
            // already populated the cache while we were waiting.
            val existing = cachedBookData
            if (existing != null && !forceRefresh) {
                return@withLock JsonParser.Result.Success(existing)
            }

            when (val result = JsonParser.loadBookData(appContext)) {
                is JsonParser.Result.Success -> {
                    cachedBookData = result.data
                    result
                }
                is JsonParser.Result.Error -> result
            }
        }
    }

    /**
     * Convenience: loads data (cached) and returns a single [Chapter] by its number.
     */
    suspend fun getChapterByNo(chapterNo: Int): JsonParser.Result<Chapter> {
        return when (val result = getBookData()) {
            is JsonParser.Result.Success -> {
                val chapter = result.data.chapters.find { it.chapterNo == chapterNo }
                if (chapter != null) {
                    JsonParser.Result.Success(chapter)
                } else {
                    JsonParser.Result.Error(
                        NoSuchElementException("Chapter $chapterNo not found"),
                        "অধ্যায় $chapterNo পাওয়া যায়নি।"
                    )
                }
            }
            is JsonParser.Result.Error -> result
        }
    }

    /** Clears the in-memory cache. Mostly useful for tests. */
    fun clearCache() {
        cachedBookData = null
    }
}

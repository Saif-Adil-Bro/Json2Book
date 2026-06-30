package com.dynamicbookreader.data.repository

import android.content.Context
import com.dynamicbookreader.data.model.BookData
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.utils.JsonParser

/**
 * Repository: single source of truth for all book data.
 *
 * The ViewModel never touches JsonParser directly; it always
 * goes through this repository. This makes it trivial to swap
 * the data source later (e.g. remote API, Room DB) without
 * touching UI code.
 */
class BookRepository(private val context: Context) {

    /**
     * Loads the full [BookData] from assets.
     */
    suspend fun getBookData(): JsonParser.Result<BookData> =
        JsonParser.loadBookData(context)

    /**
     * Convenience: loads data and returns a single [Chapter] by its number.
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
}

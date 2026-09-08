package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicbookreader.data.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_bookmarks"
)

class BookmarkRepository(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_BOOKMARKS_JSON = stringPreferencesKey("saved_bookmarks_json")
    }

    val bookmarks: Flow<List<Bookmark>> = appContext.bookmarkDataStore.data.map { prefs ->
        decodeBookmarks(prefs[KEY_BOOKMARKS_JSON])
    }

    suspend fun addBookmark(
        chapterNo: Int,
        chapterTitle: String,
        snippet: String,
        note: String = "",
        scrollFraction: Float = 0f,
        paragraphIndex: Int = 0
    ): Bookmark {
        val newBookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            chapterNo = chapterNo,
            chapterTitle = chapterTitle,
            textSnippet = snippet.trim().take(300),
            note = note.trim(),
            scrollFraction = scrollFraction.coerceIn(0f, 1f),
            paragraphIndex = paragraphIndex,
            createdAtMillis = System.currentTimeMillis()
        )

        appContext.bookmarkDataStore.edit { prefs ->
            val current = decodeBookmarks(prefs[KEY_BOOKMARKS_JSON]).toMutableList()
            // Add to top of list
            current.add(0, newBookmark)
            prefs[KEY_BOOKMARKS_JSON] = json.encodeToString(
                ListSerializer(Bookmark.serializer()),
                current
            )
        }
        return newBookmark
    }

    suspend fun deleteBookmark(id: String) {
        appContext.bookmarkDataStore.edit { prefs ->
            val current = decodeBookmarks(prefs[KEY_BOOKMARKS_JSON]).filterNot { it.id == id }
            prefs[KEY_BOOKMARKS_JSON] = json.encodeToString(
                ListSerializer(Bookmark.serializer()),
                current
            )
        }
    }

    suspend fun updateBookmarkNote(id: String, note: String) {
        appContext.bookmarkDataStore.edit { prefs ->
            val current = decodeBookmarks(prefs[KEY_BOOKMARKS_JSON]).map { bookmark ->
                if (bookmark.id == id) {
                    bookmark.copy(note = note.trim())
                } else {
                    bookmark
                }
            }
            prefs[KEY_BOOKMARKS_JSON] = json.encodeToString(
                ListSerializer(Bookmark.serializer()),
                current
            )
        }
    }

    private fun decodeBookmarks(raw: String?): List<Bookmark> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Bookmark.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

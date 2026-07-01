package com.dynamicbookreader.data.repository

import android.content.Context
import com.dynamicbookreader.data.model.Author
import com.dynamicbookreader.utils.JsonParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for author.json — same in-memory caching pattern as
 * [BookRepository], kept as a separate file/class so author data can
 * evolve independently (e.g. multiple books sharing one author, or vice
 * versa) without coupling to book content loading.
 */
class AuthorRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheMutex = Mutex()

    @Volatile
    private var cachedAuthor: Author? = null

    suspend fun getAuthor(forceRefresh: Boolean = false): JsonParser.Result<Author> {
        cachedAuthor?.let { cached ->
            if (!forceRefresh) return JsonParser.Result.Success(cached)
        }

        return cacheMutex.withLock {
            val existing = cachedAuthor
            if (existing != null && !forceRefresh) {
                return@withLock JsonParser.Result.Success(existing)
            }

            when (val result = JsonParser.loadAuthor(appContext)) {
                is JsonParser.Result.Success -> {
                    cachedAuthor = result.data
                    result
                }
                is JsonParser.Result.Error -> result
            }
        }
    }
}

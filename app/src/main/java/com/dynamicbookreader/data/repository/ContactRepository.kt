package com.dynamicbookreader.data.repository

import android.content.Context
import com.dynamicbookreader.data.model.ContactInfo
import com.dynamicbookreader.utils.JsonParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for contact.json — same in-memory caching pattern as
 * [AuthorRepository] / [BookRepository].
 */
class ContactRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheMutex = Mutex()

    @Volatile
    private var cachedContactInfo: ContactInfo? = null

    suspend fun getContactInfo(forceRefresh: Boolean = false): JsonParser.Result<ContactInfo> {
        cachedContactInfo?.let { cached ->
            if (!forceRefresh) return JsonParser.Result.Success(cached)
        }

        return cacheMutex.withLock {
            val existing = cachedContactInfo
            if (existing != null && !forceRefresh) {
                return@withLock JsonParser.Result.Success(existing)
            }

            when (val result = JsonParser.loadContactInfo(appContext)) {
                is JsonParser.Result.Success -> {
                    cachedContactInfo = result.data
                    result
                }
                is JsonParser.Result.Error -> result
            }
        }
    }
}

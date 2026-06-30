package com.dynamicbookreader.utils

import android.content.Context
import com.dynamicbookreader.data.model.BookData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Utility object that reads book_data.json from the assets folder.
 *
 * - Runs on IO dispatcher (non-blocking).
 * - Returns a sealed Result so the caller decides how to handle errors.
 * - Lenient JSON parsing tolerates extra / unknown keys, making the
 *   JSON schema forward-compatible.
 */
object JsonParser {

    private const val ASSET_FILE = "book_data.json"

    private val json = Json {
        ignoreUnknownKeys = true   // future-proof: extra JSON keys won't crash
        isLenient = true           // tolerates minor formatting issues
        coerceInputValues = true   // missing nullable fields default to null
    }

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Exception, val message: String) : Result<Nothing>()
    }

    /**
     * Parses [ASSET_FILE] asynchronously and returns [Result].
     *
     * Usage:
     * ```kotlin
     * when (val result = JsonParser.loadBookData(context)) {
     *     is Result.Success -> useData(result.data)
     *     is Result.Error   -> showError(result.message)
     * }
     * ```
     */
    suspend fun loadBookData(context: Context): Result<BookData> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets
                    .open(ASSET_FILE)
                    .bufferedReader()
                    .use { it.readText() }

                if (jsonString.isBlank()) {
                    return@withContext Result.Error(
                        IllegalStateException("Empty file"),
                        "book_data.json ফাইলটি খালি।"
                    )
                }

                val bookData = json.decodeFromString<BookData>(jsonString)
                Result.Success(bookData)

            } catch (e: kotlinx.serialization.SerializationException) {
                Result.Error(e, "JSON ফরম্যাট ত্রুটি: ${e.message}")
            } catch (e: java.io.IOException) {
                Result.Error(e, "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}")
            } catch (e: Exception) {
                Result.Error(e, "অজানা ত্রুটি: ${e.message}")
            }
        }
}

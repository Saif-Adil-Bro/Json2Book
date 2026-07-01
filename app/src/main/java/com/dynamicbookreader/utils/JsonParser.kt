package com.dynamicbookreader.utils

import android.content.Context
import com.dynamicbookreader.data.model.Author
import com.dynamicbookreader.data.model.BookData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Utility object that reads JSON asset files (book_data.json, author.json).
 *
 * - Runs on IO dispatcher (non-blocking).
 * - Returns a sealed Result so the caller decides how to handle errors.
 * - Lenient JSON parsing tolerates extra / unknown keys, making the
 *   JSON schema forward-compatible.
 */
object JsonParser {

    private const val BOOK_ASSET_FILE = "book_data.json"
    private const val AUTHOR_ASSET_FILE = "author.json"

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
     * Parses [BOOK_ASSET_FILE] asynchronously and returns [Result].
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
        readAsset(context, BOOK_ASSET_FILE) { json.decodeFromString<BookData>(it) }

    /**
     * Parses [AUTHOR_ASSET_FILE] asynchronously and returns [Result].
     */
    suspend fun loadAuthor(context: Context): Result<Author> =
        readAsset(context, AUTHOR_ASSET_FILE) { json.decodeFromString<Author>(it) }

    /**
     * Shared read + parse + error-mapping logic for any JSON asset file.
     */
    private suspend fun <T> readAsset(
        context: Context,
        fileName: String,
        decode: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }

            if (jsonString.isBlank()) {
                return@withContext Result.Error(
                    IllegalStateException("Empty file"),
                    "$fileName ফাইলটি খালি।"
                )
            }

            Result.Success(decode(jsonString))

        } catch (e: kotlinx.serialization.SerializationException) {
            Result.Error(e, "$fileName JSON ফরম্যাট ত্রুটি: ${e.message}")
        } catch (e: java.io.FileNotFoundException) {
            Result.Error(e, "$fileName ফাইলটি পাওয়া যায়নি।")
        } catch (e: java.io.IOException) {
            Result.Error(e, "$fileName পড়তে সমস্যা হয়েছে: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e, "অজানা ত্রুটি: ${e.message}")
        }
    }
}

package com.dynamicbookreader.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root model — maps exactly to book_data.json structure.
 * Change the JSON, the app updates automatically.
 */
@Serializable
data class BookData(
    @SerialName("book_title") val bookTitle: String,
    @SerialName("data") val chapters: List<Chapter>
)

@Serializable
data class Chapter(
    @SerialName("chapter_no") val chapterNo: Int,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String
)

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

/**
 * Author info — parsed from the separate `author.json` asset.
 * Kept independent of [BookData] so multi-book setups can share one author
 * file, or swap authors without touching book_data.json.
 */
@Serializable
data class Author(
    @SerialName("name") val name: String,
    @SerialName("name_en") val nameEn: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    @SerialName("birth_year") val birthYear: String = "",
    @SerialName("death_year") val deathYear: String = "",
    @SerialName("short_bio") val shortBio: String,
    @SerialName("full_biography") val fullBiography: String
)

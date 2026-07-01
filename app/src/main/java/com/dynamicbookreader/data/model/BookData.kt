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
    @SerialName("content") val content: String,
    /** Optional short subtitle shown under the title on Home card + Reading header. */
    @SerialName("subtitle") val subtitle: String = "",
    /** Optional printed-book page range, e.g. "৩৫–৪২". Shown once at the top of the Reading screen. */
    @SerialName("page_range") val pageRange: String = "",
    /**
     * Optional footnotes. Reference a footnote inline in [content] using
     * `{{note:KEY}}` (e.g. `...কুরাইশ বংশ{{note:1}} ছিল...`), then define
     * its text here with a matching [Footnote.key]. Markers with no
     * matching key are rendered as plain text so a typo never breaks
     * rendering.
     */
    @SerialName("footnotes") val footnotes: List<Footnote> = emptyList()
)

@Serializable
data class Footnote(
    @SerialName("key") val key: String,
    @SerialName("text") val text: String
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

/**
 * Contact page content — parsed from the separate `contact.json` asset.
 * [items] is an open-ended list so new contact methods (WhatsApp, Telegram,
 * X/Twitter, etc.) can be added purely via JSON — [ContactType] maps a
 * recognized `type` string to an icon; unknown types still render fine
 * with a generic fallback icon.
 */
@Serializable
data class ContactInfo(
    @SerialName("intro_title") val introTitle: String,
    @SerialName("intro_text") val introText: String,
    @SerialName("items") val items: List<ContactItem>
)

@Serializable
data class ContactItem(
    @SerialName("type") val type: String,
    @SerialName("label") val label: String,
    @SerialName("value") val value: String
)

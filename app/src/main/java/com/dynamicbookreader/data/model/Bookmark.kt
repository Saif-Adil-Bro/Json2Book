package com.dynamicbookreader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: String,
    val chapterNo: Int,
    val chapterTitle: String,
    val textSnippet: String,
    val note: String = "",
    val scrollFraction: Float = 0f,
    val paragraphIndex: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

package com.dynamicbookreader.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Utility to support simultaneous bilingual rendering of Bengali and Arabic text.
 * Automatically identifies Arabic character spans and applies the chosen Arabic font
 * (with appropriate optical scaling), while Bengali text retains the chosen Bangla font.
 */
object BilingualTextHelper {

    /**
     * Regex matching sequences of Arabic characters, diacritics (Harakat),
     * Tatweel, Quranic marks, Arabic numerals, and Quranic brackets.
     */
    val ARABIC_REGEX = Regex("""[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF\uFD3E\uFD3F\u200C\u200D]+(?:\s+[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF\uFD3E\uFD3F\u200C\u200D]+)*""")

    fun isArabic(char: Char): Boolean {
        val code = char.code
        return (code in 0x0600..0x06FF) ||
               (code in 0x0750..0x077F) ||
               (code in 0x08A0..0x08FF) ||
               (code in 0xFB50..0xFDFF) ||
               (code in 0xFE70..0xFEFF) ||
               code == 0xFD3E || code == 0xFD3F
    }

    fun containsArabic(text: String): Boolean {
        return text.any { isArabic(it) }
    }

    /**
     * Builds an [AnnotatedString] where Arabic segments are styled with [arabicFont]
     * (and optionally optical scale for readability), while remaining text uses [banglaFont].
     */
    fun buildBilingualAnnotatedString(
        text: String,
        banglaFont: FontFamily,
        arabicFont: FontFamily,
        baseFontSizeSp: Float? = null,
        arabicScaleMultiplier: Float = 1.12f
    ): AnnotatedString {
        if (!containsArabic(text)) {
            return AnnotatedString(text)
        }
        val builder = AnnotatedString.Builder(text)
        val matches = ARABIC_REGEX.findAll(text)
        for (match in matches) {
            val range = match.range
            builder.addStyle(
                style = SpanStyle(
                    fontFamily = arabicFont,
                    fontSize = baseFontSizeSp?.let { (it * arabicScaleMultiplier).sp } ?: TextUnit.Unspecified
                ),
                start = range.first,
                end = range.last + 1
            )
        }
        return builder.toAnnotatedString()
    }

    /**
     * Builds an [AnnotatedString] from chapter content segments, applying [arabicFont]
     * to Arabic substrings and superscript styling to footnote references.
     */
    fun buildContentBlockAnnotatedString(
        segments: List<ChapterContentParser.TextSegment>,
        banglaFont: FontFamily,
        arabicFont: FontFamily,
        footnoteColor: Color,
        fontSize: Float,
        arabicScaleMultiplier: Float = 1.12f
    ): AnnotatedString {
        return buildAnnotatedString {
            segments.forEach { segment ->
                when (segment) {
                    is ChapterContentParser.TextSegment.Plain -> {
                        val startPos = this.length
                        append(segment.text)
                        if (containsArabic(segment.text)) {
                            val matches = ARABIC_REGEX.findAll(segment.text)
                            for (match in matches) {
                                addStyle(
                                    style = SpanStyle(
                                        fontFamily = arabicFont,
                                        fontSize = (fontSize * arabicScaleMultiplier).sp
                                    ),
                                    start = startPos + match.range.first,
                                    end = startPos + match.range.last + 1
                                )
                            }
                        }
                    }
                    is ChapterContentParser.TextSegment.FootnoteRef -> {
                        pushStringAnnotation(
                            tag = "footnote",
                            annotation = segment.key
                        )
                        withStyle(
                            SpanStyle(
                                color = footnoteColor,
                                fontWeight = FontWeight.Bold,
                                baselineShift = BaselineShift.Superscript,
                                fontSize = (fontSize * 0.7f).sp,
                                fontFamily = banglaFont
                            )
                        ) {
                            append("[${segment.displayNumber}]")
                        }
                        pop()
                    }
                }
            }
        }
    }
}

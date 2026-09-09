package com.dynamicbookreader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.R

// ── Custom Font Family Definitions ──────────────────────────────────────────

val SolaimanLipiFont = FontFamily(
    Font(R.font.solaimanlipi, FontWeight.Normal),
    Font(R.font.solaimanlipi_bold, FontWeight.Bold)
)

val SolaimanLipiBoldFont = FontFamily(
    Font(R.font.solaimanlipi_bold, FontWeight.Normal),
    Font(R.font.solaimanlipi_bold, FontWeight.Bold)
)

val ShorifShishirFont = FontFamily(
    Font(R.font.shorif_shishir, FontWeight.Normal)
)

val HindSiliguriFont = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal)
)

val AmiriFont = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal)
)

val ScheherazadeFont = FontFamily(
    Font(R.font.scheherazade_new, FontWeight.Normal)
)

// ── Font Language Category ───────────────────────────────────────────────────

enum class FontLanguage(val label: String, val badge: String) {
    BANGLA("বাংলা", "৪টি ফন্ট"),
    ARABIC("আরবি", "২টি ফন্ট")
}

// ── Font Family Options ───────────────────────────────────────────────────────

enum class BanglaFontFamily(
    val displayName: String,
    val subtitle: String,
    val sampleText: String,
    val fontFamily: FontFamily,
    val isDefault: Boolean = false
) {
    SOLAIMAN_LIPI("সুলাইমান লিপি", "ডিফল্ট বাংলা", "জ্ঞান হলো আলোর মিনার", SolaimanLipiFont, isDefault = true),
    SOLAIMAN_LIPI_BOLD("সুলাইমান বোল্ড", "গাঢ় বাংলা", "জ্ঞান হলো আলোর মিনার", SolaimanLipiBoldFont),
    SHORIF_SHISHIR("শরিফ শিশির", "ক্লাসিক বাংলা", "জ্ঞান হলো আলোর মিনার", ShorifShishirFont),
    HIND_SILIGURI("হিন্দ শিলিগুড়ি", "আধুনিক বাংলা", "জ্ঞান হলো আলোর মিনার", HindSiliguriFont);

    companion object {
        val DEFAULT = SOLAIMAN_LIPI
    }
}

enum class ArabicFontFamily(
    val displayName: String,
    val subtitle: String,
    val sampleText: String,
    val fontFamily: FontFamily,
    val isDefault: Boolean = false
) {
    AMIRI("আমিরি (Amiri)", "ডিফল্ট আরবি", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", AmiriFont, isDefault = true),
    SCHEHERAZADE("শাহরাজাদ (Scheherazade)", "নাসখ আরবি", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", ScheherazadeFont);

    companion object {
        val DEFAULT = AMIRI
    }
}

enum class ReadingFontFamily(
    val displayName: String,
    val subtitle: String,
    val sampleText: String,
    val language: FontLanguage,
    val fontFamily: FontFamily,
    val isDefault: Boolean = false
) {
    // বাংলা ফন্ট (৪টি)
    SOLAIMAN_LIPI(
        displayName = "সুলাইমান লিপি",
        subtitle = "ডিফল্ট বাংলা",
        sampleText = "জ্ঞান হলো আলোর মিনার",
        language = FontLanguage.BANGLA,
        fontFamily = SolaimanLipiFont,
        isDefault = true
    ),
    SOLAIMAN_LIPI_BOLD(
        displayName = "সুলাইমান বোল্ড",
        subtitle = "গাঢ় বাংলা",
        sampleText = "জ্ঞান হলো আলোর মিনার",
        language = FontLanguage.BANGLA,
        fontFamily = SolaimanLipiBoldFont
    ),
    SHORIF_SHISHIR(
        displayName = "শরিফ শিশির",
        subtitle = "ক্লাসিক বাংলা",
        sampleText = "জ্ঞান হলো আলোর মিনার",
        language = FontLanguage.BANGLA,
        fontFamily = ShorifShishirFont
    ),
    HIND_SILIGURI(
        displayName = "হিন্দ শিলিগুড়ি",
        subtitle = "আধুনিক বাংলা",
        sampleText = "জ্ঞান হলো আলোর মিনার",
        language = FontLanguage.BANGLA,
        fontFamily = HindSiliguriFont
    ),

    // আরবি ফন্ট (২টি)
    AMIRI(
        displayName = "আমিরি (Amiri)",
        subtitle = "ডিফল্ট আরবি",
        sampleText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        language = FontLanguage.ARABIC,
        fontFamily = AmiriFont,
        isDefault = true
    ),
    SCHEHERAZADE(
        displayName = "শাহরাজাদ",
        subtitle = "নাসখ আরবি",
        sampleText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        language = FontLanguage.ARABIC,
        fontFamily = ScheherazadeFont
    );

    companion object {
        val DEFAULT_BANGLA = SOLAIMAN_LIPI
        val DEFAULT_ARABIC = AMIRI
    }
}

// ── Text Alignment Options ────────────────────────────────────────────────────

enum class TextAlignOption(
    val displayName: String,
    val emoji: String,
    val align: androidx.compose.ui.text.style.TextAlign
) {
    JUSTIFY("উভয়পাশ", "☰", androidx.compose.ui.text.style.TextAlign.Justify),
    START("বামপাশ", "⇤", androidx.compose.ui.text.style.TextAlign.Start),
    CENTER("মাঝখানে", "☵", androidx.compose.ui.text.style.TextAlign.Center)
}

/**
 * Typography scale.
 * Defaults to Solaiman Lipi for clean, natural Bengali glyph rendering across the app.
 */
val AppTypography = Typography(
    // App bar, book title
    headlineLarge = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    // Chapter titles on home screen
    headlineMedium = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // Chapter number badge label
    headlineSmall = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    // Chapter card title
    titleLarge = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    // Body text
    bodyLarge = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 30.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SolaimanLipiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

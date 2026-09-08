package com.dynamicbookreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Reading Themes ────────────────────────────────────────────────────────────

enum class ReadingTheme(
    val displayName: String,
    val emoji: String
) {
    DAY("দিন", "☀️"),
    SEPIA("সেপিয়া", "📜"),
    NIGHT("রাত", "🌙"),
    AMOLED("ওলেড", "🌑"),
    EMERALD("পান্না সবুজ", "🌲"),
    ROSE("রোজ ভেলভেট", "🌸")
}

// ── Reading Page Modes ────────────────────────────────────────────────────────

enum class ReadingMode(
    val displayName: String,
    val subtitle: String,
    val emoji: String
) {
    SCROLL("স্ক্রোল মোড", "ধারাবাহিক স্ক্রোল", "📜"),
    PAGE_FLIP("পাতা ওল্টানো", "বাস্তব বইয়ের পাতা", "📖")
}

// ── Color Palette ─────────────────────────────────────────────────────────────

// Primary brand: deep teal-indigo reflecting manuscript heritage
private val PrimaryDeep = Color(0xFF1A3A5C)          // deep navy blue
private val PrimaryLight = Color(0xFF2E6DA4)          // medium blue
private val PrimaryContainer = Color(0xFFD0E8FF)      // light blue tint

private val SecondaryDeep = Color(0xFF5C3A1A)         // warm amber brown
private val SecondaryContainer = Color(0xFFFFE4C4)    // parchment

// Day theme colours
private val DayBackground = Color(0xFFFAF9F6)         // warm near-white
private val DayOnBackground = Color(0xFF1C1B1F)
private val DaySurface = Color(0xFFFFFFFF)
private val DaySurfaceVariant = Color(0xFFEEECE6)

// Night theme colours
private val NightBackground = Color(0xFF0F1923)       // deep dark navy
private val NightOnBackground = Color(0xFFD6E4F0)     // cool light grey-blue
private val NightSurface = Color(0xFF1A2733)
private val NightSurfaceVariant = Color(0xFF243040)

// Sepia theme colours
private val SepiaBackground = Color(0xFFF4EDDA)       // classic sepia parchment
private val SepiaOnBackground = Color(0xFF3B2E1A)     // dark warm brown text
private val SepiaSurface = Color(0xFFFDF6E3)
private val SepiaSurfaceVariant = Color(0xFFE8D8B0)

// AMOLED True Black theme colours
private val AmoledBackground = Color(0xFF000000)      // pitch black for battery saving
private val AmoledOnBackground = Color(0xFFE2E2E2)    // soft readable white
private val AmoledSurface = Color(0xFF121212)
private val AmoledSurfaceVariant = Color(0xFF1E1E1E)

// Emerald Forest theme colours (Relaxing green for long reading sessions)
private val EmeraldBackground = Color(0xFFF0F5F1)
private val EmeraldOnBackground = Color(0xFF1B3322)
private val EmeraldSurface = Color(0xFFF7FAF7)
private val EmeraldSurfaceVariant = Color(0xFFDDE8DF)

// Rose Velvet theme colours (Warm soothing aesthetic)
private val RoseBackground = Color(0xFFFAF2F2)
private val RoseOnBackground = Color(0xFF3B2024)
private val RoseSurface = Color(0xFFFFF7F7)
private val RoseSurfaceVariant = Color(0xFFEEDCDC)

// ── ColorSchemes ──────────────────────────────────────────────────────────────

private val DayColorScheme = lightColorScheme(
    primary = PrimaryDeep,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDeep,
    secondary = SecondaryDeep,
    secondaryContainer = SecondaryContainer,
    background = DayBackground,
    onBackground = DayOnBackground,
    surface = DaySurface,
    onSurface = DayOnBackground,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = Color(0xFF44464F)
)

private val NightColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),            // soft blue for dark
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004880),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFFFB74D),
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnBackground,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0BECA)
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF6B4226),            // rich brown
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4A97A),
    onPrimaryContainer = Color(0xFF3B1F0A),
    secondary = Color(0xFF8B6914),
    secondaryContainer = Color(0xFFFFE082),
    background = SepiaBackground,
    onBackground = SepiaOnBackground,
    surface = SepiaSurface,
    onSurface = SepiaOnBackground,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = Color(0xFF5A4130)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),            // bright ice blue
    onPrimary = Color(0xFF002244),
    primaryContainer = Color(0xFF1A334D),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFFFFB74D),
    background = AmoledBackground,
    onBackground = AmoledOnBackground,
    surface = AmoledSurface,
    onSurface = AmoledOnBackground,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFF9E9E9E)
)

private val EmeraldColorScheme = lightColorScheme(
    primary = Color(0xFF2E6B48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7EBD3),
    onPrimaryContainer = Color(0xFF0F3820),
    secondary = Color(0xFF4A7558),
    secondaryContainer = Color(0xFFCCE4D4),
    background = EmeraldBackground,
    onBackground = EmeraldOnBackground,
    surface = EmeraldSurface,
    onSurface = EmeraldOnBackground,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = Color(0xFF385240)
)

private val RoseColorScheme = lightColorScheme(
    primary = Color(0xFF8C3E52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDD5DE),
    onPrimaryContainer = Color(0xFF4A1020),
    secondary = Color(0xFF965360),
    secondaryContainer = Color(0xFFFBE0E5),
    background = RoseBackground,
    onBackground = RoseOnBackground,
    surface = RoseSurface,
    onSurface = RoseOnBackground,
    surfaceVariant = RoseSurfaceVariant,
    onSurfaceVariant = Color(0xFF5C333D)
)

// ── Theme selectors ───────────────────────────────────────────────────────────

fun ReadingTheme.colorScheme(): ColorScheme = when (this) {
    ReadingTheme.DAY -> DayColorScheme
    ReadingTheme.NIGHT -> NightColorScheme
    ReadingTheme.SEPIA -> SepiaColorScheme
    ReadingTheme.AMOLED -> AmoledColorScheme
    ReadingTheme.EMERALD -> EmeraldColorScheme
    ReadingTheme.ROSE -> RoseColorScheme
}

// ── App-wide MaterialTheme wrapper ────────────────────────────────────────────

@Composable
fun DynamicBookReaderTheme(
    readingTheme: ReadingTheme = ReadingTheme.DAY,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = readingTheme.colorScheme(),
        typography = AppTypography,
        content = content
    )
}

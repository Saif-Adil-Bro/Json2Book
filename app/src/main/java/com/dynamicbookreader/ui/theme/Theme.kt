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
    NIGHT("রাত", "🌙"),
    SEPIA("সেপিয়া", "📜")
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

// ── Theme selectors ───────────────────────────────────────────────────────────

fun ReadingTheme.colorScheme(): ColorScheme = when (this) {
    ReadingTheme.DAY -> DayColorScheme
    ReadingTheme.NIGHT -> NightColorScheme
    ReadingTheme.SEPIA -> SepiaColorScheme
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

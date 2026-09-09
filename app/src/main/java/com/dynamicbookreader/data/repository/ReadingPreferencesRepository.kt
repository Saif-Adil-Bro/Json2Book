package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicbookreader.ui.theme.ArabicFontFamily
import com.dynamicbookreader.ui.theme.BanglaFontFamily
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.ReadingMode
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.ui.theme.TextAlignOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_preferences"
)

/**
 * Persists user reading preferences (font size, line height, bangla & arabic font families,
 * text alignment, keep screen on, theme) across app sessions using Jetpack DataStore.
 */
class ReadingPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_FONT_SIZE = floatPreferencesKey("font_size")
        private val KEY_LINE_HEIGHT = floatPreferencesKey("line_height")
        private val KEY_THEME = stringPreferencesKey("reading_theme")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        private val KEY_BANGLA_FONT = stringPreferencesKey("bangla_font_family")
        private val KEY_ARABIC_FONT = stringPreferencesKey("arabic_font_family")
        private val KEY_TEXT_ALIGN = stringPreferencesKey("text_align")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_READING_MODE = stringPreferencesKey("reading_mode")
        private val KEY_PAPER_TEXTURE = booleanPreferencesKey("paper_texture")

        const val DEFAULT_FONT_SIZE = 17f
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 28f
        const val DEFAULT_LINE_HEIGHT = 1.8f
        const val MIN_LINE_HEIGHT = 1.2f
        const val MAX_LINE_HEIGHT = 2.8f
    }

    val fontSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: DEFAULT_FONT_SIZE
    }

    val lineHeight: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_LINE_HEIGHT] ?: DEFAULT_LINE_HEIGHT
    }

    val readingTheme: Flow<ReadingTheme> = context.dataStore.data.map { prefs ->
        val themeName = prefs[KEY_THEME] ?: ReadingTheme.DAY.name
        try {
            ReadingTheme.valueOf(themeName)
        } catch (e: Exception) {
            ReadingTheme.DAY
        }
    }

    val banglaFont: Flow<BanglaFontFamily> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_BANGLA_FONT] ?: prefs[KEY_FONT_FAMILY] ?: BanglaFontFamily.SOLAIMAN_LIPI.name
        try {
            BanglaFontFamily.valueOf(name)
        } catch (e: Exception) {
            BanglaFontFamily.SOLAIMAN_LIPI
        }
    }

    val arabicFont: Flow<ArabicFontFamily> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_ARABIC_FONT] ?: ArabicFontFamily.AMIRI.name
        try {
            ArabicFontFamily.valueOf(name)
        } catch (e: Exception) {
            ArabicFontFamily.AMIRI
        }
    }

    val fontFamily: Flow<ReadingFontFamily> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_FONT_FAMILY] ?: ReadingFontFamily.SOLAIMAN_LIPI.name
        try {
            ReadingFontFamily.valueOf(name)
        } catch (e: Exception) {
            ReadingFontFamily.SOLAIMAN_LIPI
        }
    }

    val textAlign: Flow<TextAlignOption> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_TEXT_ALIGN] ?: TextAlignOption.JUSTIFY.name
        try {
            TextAlignOption.valueOf(name)
        } catch (e: Exception) {
            TextAlignOption.JUSTIFY
        }
    }

    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_KEEP_SCREEN_ON] ?: true
    }

    val readingMode: Flow<ReadingMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_READING_MODE] ?: ReadingMode.SCROLL.name
        try {
            ReadingMode.valueOf(name)
        } catch (e: Exception) {
            ReadingMode.SCROLL
        }
    }

    val paperTextureEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PAPER_TEXTURE] ?: false
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        }
    }

    suspend fun setLineHeight(height: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LINE_HEIGHT] = height.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT)
        }
    }

    suspend fun setReadingTheme(theme: ReadingTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_READING_MODE] = mode.name
        }
    }

    suspend fun setPaperTextureEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAPER_TEXTURE] = enabled
        }
    }

    suspend fun setBanglaFont(family: BanglaFontFamily) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BANGLA_FONT] = family.name
        }
    }

    suspend fun setArabicFont(family: ArabicFontFamily) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ARABIC_FONT] = family.name
        }
    }

    suspend fun setFontFamily(family: ReadingFontFamily) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_FAMILY] = family.name
            if (family.language == com.dynamicbookreader.ui.theme.FontLanguage.BANGLA) {
                try {
                    prefs[KEY_BANGLA_FONT] = BanglaFontFamily.valueOf(family.name).name
                } catch (_: Exception) {}
            } else {
                try {
                    prefs[KEY_ARABIC_FONT] = ArabicFontFamily.valueOf(family.name).name
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun setTextAlign(align: TextAlignOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEXT_ALIGN] = align.name
        }
    }

    suspend fun setKeepScreenOn(keep: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_KEEP_SCREEN_ON] = keep
        }
    }
}


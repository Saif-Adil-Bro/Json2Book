package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicbookreader.ui.theme.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_preferences"
)

/**
 * Persists user reading preferences (font size, line height, theme)
 * across app sessions using Jetpack DataStore.
 */
class ReadingPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_FONT_SIZE = floatPreferencesKey("font_size")
        private val KEY_LINE_HEIGHT = floatPreferencesKey("line_height")
        private val KEY_THEME = stringPreferencesKey("reading_theme")

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
        ReadingTheme.valueOf(themeName)
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
}

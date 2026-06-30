package com.dynamicbookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicbookreader.data.model.BookData
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.data.repository.BookRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_LINE_HEIGHT
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.utils.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── UI State sealed class ────────────────────────────────────────────────────

sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val bookData: BookData) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

/**
 * Shared ViewModel between all screens.
 *
 * Uses AndroidViewModel to access [Application] context for the repository —
 * avoids leaking Activity context.
 */
class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val prefsRepository = ReadingPreferencesRepository(application)

    // ── Book data state ──────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<BookUiState>(BookUiState.Loading)
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    // ── Currently selected chapter ───────────────────────────────────────────

    private val _selectedChapter = MutableStateFlow<Chapter?>(null)
    val selectedChapter: StateFlow<Chapter?> = _selectedChapter.asStateFlow()

    // ── Reading preferences (persisted via DataStore) ────────────────────────

    val fontSize: StateFlow<Float> = prefsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_FONT_SIZE)

    val lineHeight: StateFlow<Float> = prefsRepository.lineHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_LINE_HEIGHT)

    val readingTheme: StateFlow<ReadingTheme> = prefsRepository.readingTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingTheme.DAY)

    // ── Init: load data immediately ──────────────────────────────────────────

    init {
        loadBook()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun loadBook() {
        viewModelScope.launch {
            _uiState.value = BookUiState.Loading
            when (val result = bookRepository.getBookData()) {
                is JsonParser.Result.Success ->
                    _uiState.value = BookUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _uiState.value = BookUiState.Error(result.message)
            }
        }
    }

    fun selectChapter(chapter: Chapter) {
        _selectedChapter.value = chapter
    }

    fun increaseFontSize() = viewModelScope.launch {
        val current = fontSize.value
        prefsRepository.setFontSize(current + 1f)
    }

    fun decreaseFontSize() = viewModelScope.launch {
        val current = fontSize.value
        prefsRepository.setFontSize(current - 1f)
    }

    fun increaseLineHeight() = viewModelScope.launch {
        val current = lineHeight.value
        prefsRepository.setLineHeight(current + 0.2f)
    }

    fun decreaseLineHeight() = viewModelScope.launch {
        val current = lineHeight.value
        prefsRepository.setLineHeight(current - 0.2f)
    }

    fun setReadingTheme(theme: ReadingTheme) = viewModelScope.launch {
        prefsRepository.setReadingTheme(theme)
    }
}

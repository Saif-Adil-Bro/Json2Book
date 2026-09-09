package com.dynamicbookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicbookreader.data.model.Author
import com.dynamicbookreader.data.model.BookData
import com.dynamicbookreader.data.model.Bookmark
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.data.model.ContactInfo
import com.dynamicbookreader.data.model.ReadingAnalyticsData
import com.dynamicbookreader.data.repository.AuthorRepository
import com.dynamicbookreader.data.repository.BookmarkRepository
import com.dynamicbookreader.data.repository.BookRepository
import com.dynamicbookreader.data.repository.ContactRepository
import com.dynamicbookreader.data.repository.ReadingAnalyticsRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_LINE_HEIGHT
import com.dynamicbookreader.data.repository.ReadingProgress
import com.dynamicbookreader.data.repository.ReadingProgressRepository
import com.dynamicbookreader.ui.theme.ArabicFontFamily
import com.dynamicbookreader.ui.theme.BanglaFontFamily
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.ui.theme.TextAlignOption
import com.dynamicbookreader.utils.BengaliTtsManager
import com.dynamicbookreader.utils.JsonParser
import com.dynamicbookreader.utils.TtsPlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── UI State sealed classes ──────────────────────────────────────────────────

/** State of the full book / chapter-list load (Home screen). */
sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val bookData: BookData) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

/** State of opening a single chapter (Reading screen). */
sealed class ChapterUiState {
    object Idle : ChapterUiState()
    object Loading : ChapterUiState()
    data class Success(val chapter: Chapter) : ChapterUiState()
    data class Error(val message: String) : ChapterUiState()
}

/** State of the author.json load. */
sealed class AuthorUiState {
    object Loading : AuthorUiState()
    data class Success(val author: Author) : AuthorUiState()
    data class Error(val message: String) : AuthorUiState()
}

/** State of the contact.json load. */
sealed class ContactUiState {
    object Idle : ContactUiState()
    object Loading : ContactUiState()
    data class Success(val contactInfo: ContactInfo) : ContactUiState()
    data class Error(val message: String) : ContactUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val authorRepository = AuthorRepository(application)
    private val contactRepository = ContactRepository(application)
    private val prefsRepository = ReadingPreferencesRepository(application)
    private val progressRepository = ReadingProgressRepository(application)
    private val bookmarkRepository = BookmarkRepository(application)
    private val analyticsRepository = ReadingAnalyticsRepository(application)

    val ttsManager = BengaliTtsManager(application)
    val ttsPlaybackState: StateFlow<TtsPlaybackState> = ttsManager.playbackState
    val ttsSleepTimerMinutes: StateFlow<Int?> = ttsManager.sleepTimerMinutes
    val ttsSleepTimerSecondsLeft: StateFlow<Int> = ttsManager.sleepTimerSecondsLeft

    // ── Book data state ──────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<BookUiState>(BookUiState.Loading)
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    val bookData: StateFlow<BookData?> = _uiState.map {
        (it as? BookUiState.Success)?.bookData
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Author state ─────────────────────────────────────────────────────────

    private val _authorUiState = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val authorUiState: StateFlow<AuthorUiState> = _authorUiState.asStateFlow()

    val authorData: StateFlow<Author?> = _authorUiState.map {
        (it as? AuthorUiState.Success)?.author
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Contact state ────────────────────────────────────────────────────────

    private val _contactUiState = MutableStateFlow<ContactUiState>(ContactUiState.Idle)
    val contactUiState: StateFlow<ContactUiState> = _contactUiState.asStateFlow()

    // ── Chapter state ────────────────────────────────────────────────────────

    private val _chapterUiState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val chapterUiState: StateFlow<ChapterUiState> = _chapterUiState.asStateFlow()

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Chapter>> = combine(_searchQuery, uiState) { query, book ->
        if (query.isBlank()) return@combine emptyList()
        val chapters = (book as? BookUiState.Success)?.bookData?.chapters ?: return@combine emptyList()
        chapters.filter { chapter ->
            chapter.title.contains(query, ignoreCase = true) ||
                    chapter.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Reading preferences ──────────────────────────────────────────────────

    val fontSize: StateFlow<Float> = prefsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_FONT_SIZE)

    val lineHeight: StateFlow<Float> = prefsRepository.lineHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_LINE_HEIGHT)

    val readingTheme: StateFlow<ReadingTheme> = prefsRepository.readingTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingTheme.DAY)

    val banglaFont: StateFlow<BanglaFontFamily> = prefsRepository.banglaFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BanglaFontFamily.SOLAIMAN_LIPI)

    val arabicFont: StateFlow<ArabicFontFamily> = prefsRepository.arabicFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArabicFontFamily.AMIRI)

    val fontFamily: StateFlow<ReadingFontFamily> = prefsRepository.fontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingFontFamily.SOLAIMAN_LIPI)

    val textAlign: StateFlow<TextAlignOption> = prefsRepository.textAlign
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TextAlignOption.JUSTIFY)

    val keepScreenOn: StateFlow<Boolean> = prefsRepository.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val readingMode: StateFlow<com.dynamicbookreader.ui.theme.ReadingMode> = prefsRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.dynamicbookreader.ui.theme.ReadingMode.SCROLL)

    val paperTextureEnabled: StateFlow<Boolean> = prefsRepository.paperTextureEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Bookmarks & Notes ───────────────────────────────────────────────────

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Reading progress ─────────────────────────────────────────────────────

    val readingProgress: StateFlow<ReadingProgress> = progressRepository.readingProgress
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReadingProgress(chapterNo = null, scrollFraction = 0f, chapterTitle = null, updatedAtMillis = 0L)
        )

    val perChapterProgress: StateFlow<Map<Int, Float>> = progressRepository.perChapterProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val perChapterReadHeadings: StateFlow<Map<Int, Set<String>>> = progressRepository.perChapterReadHeadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Reading Analytics & Streaks ──────────────────────────────────────────

    val analyticsData: StateFlow<ReadingAnalyticsData> = analyticsRepository.analyticsData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingAnalyticsData())

    fun markHeadingRead(chapterNo: Int, headingKey: String) {
        viewModelScope.launch {
            progressRepository.markHeadingRead(chapterNo, headingKey)
        }
    }

    private var progressSaveJob: Job? = null

    init {
        loadBook()
        loadAuthor()
    }

    // ── Public API: Book / Chapter ───────────────────────────────────────────

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

    fun reloadBookFromSource() {
        viewModelScope.launch {
            _uiState.value = BookUiState.Loading
            when (val result = bookRepository.getBookData(forceRefresh = true)) {
                is JsonParser.Result.Success ->
                    _uiState.value = BookUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _uiState.value = BookUiState.Error(result.message)
            }
        }
    }

    // ── Public API: Author ────────────────────────────────────────────────────

    fun loadAuthor() {
        viewModelScope.launch {
            _authorUiState.value = AuthorUiState.Loading
            when (val result = authorRepository.getAuthor()) {
                is JsonParser.Result.Success ->
                    _authorUiState.value = AuthorUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _authorUiState.value = AuthorUiState.Error(result.message)
            }
        }
    }

    fun reloadAuthorFromSource() {
        viewModelScope.launch {
            _authorUiState.value = AuthorUiState.Loading
            when (val result = authorRepository.getAuthor(forceRefresh = true)) {
                is JsonParser.Result.Success ->
                    _authorUiState.value = AuthorUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _authorUiState.value = AuthorUiState.Error(result.message)
            }
        }
    }

    // ── Public API: Contact ───────────────────────────────────────────────────

    fun loadContactInfoIfNeeded() {
        if (_contactUiState.value is ContactUiState.Success) return
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            when (val result = contactRepository.getContactInfo()) {
                is JsonParser.Result.Success ->
                    _contactUiState.value = ContactUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _contactUiState.value = ContactUiState.Error(result.message)
            }
        }
    }

    fun reloadContactInfoFromSource() {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            when (val result = contactRepository.getContactInfo(forceRefresh = true)) {
                is JsonParser.Result.Success ->
                    _contactUiState.value = ContactUiState.Success(result.data)
                is JsonParser.Result.Error ->
                    _contactUiState.value = ContactUiState.Error(result.message)
            }
        }
    }

    // ── Public API: Search ────────────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ── Public API: Chapter reading ──────────────────────────────────────────

    fun openChapter(chapterNo: Int) {
        _chapterUiState.value = ChapterUiState.Loading
        viewModelScope.launch {
            when (val result = bookRepository.getChapterByNo(chapterNo)) {
                is JsonParser.Result.Success -> {
                    _chapterUiState.value = ChapterUiState.Success(result.data)
                }
                is JsonParser.Result.Error -> {
                    _chapterUiState.value = ChapterUiState.Error(result.message)
                }
            }
        }
    }

    fun retryOpenChapter(chapterNo: Int) = openChapter(chapterNo)

    fun clearChapterState() {
        _chapterUiState.value = ChapterUiState.Idle
        progressSaveJob?.cancel()
        ttsManager.stop()
    }

    // ── Public API: Progress & Analytics ─────────────────────────────────────

    fun updateReadingProgress(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        if (scrollFraction < 0.01f) return
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(600)
            progressRepository.saveProgress(chapterNo, chapterTitle, scrollFraction)
            if (scrollFraction >= 0.95f) {
                analyticsRepository.incrementCompletedChapters()
            }
        }
    }

    fun saveReadingProgressNow(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        if (scrollFraction < 0.01f) return
        progressSaveJob?.cancel()
        viewModelScope.launch {
            progressRepository.saveProgress(chapterNo, chapterTitle, scrollFraction)
        }
    }

    fun recordReadingTime(seconds: Long) = viewModelScope.launch {
        analyticsRepository.recordReadingSession(seconds)
    }

    fun setDailyGoalMinutes(minutes: Int) = viewModelScope.launch {
        analyticsRepository.setDailyGoalMinutes(minutes)
    }

    // ── Public API: Reading preferences ──────────────────────────────────────

    fun increaseFontSize() = viewModelScope.launch {
        prefsRepository.setFontSize(fontSize.value + 1f)
    }

    fun decreaseFontSize() = viewModelScope.launch {
        prefsRepository.setFontSize(fontSize.value - 1f)
    }

    fun increaseLineHeight() = viewModelScope.launch {
        prefsRepository.setLineHeight(lineHeight.value + 0.2f)
    }

    fun decreaseLineHeight() = viewModelScope.launch {
        prefsRepository.setLineHeight(lineHeight.value - 0.2f)
    }

    fun setReadingTheme(theme: ReadingTheme) = viewModelScope.launch {
        prefsRepository.setReadingTheme(theme)
    }

    fun setBanglaFont(family: BanglaFontFamily) = viewModelScope.launch {
        prefsRepository.setBanglaFont(family)
    }

    fun setArabicFont(family: ArabicFontFamily) = viewModelScope.launch {
        prefsRepository.setArabicFont(family)
    }

    fun setFontFamily(family: ReadingFontFamily) = viewModelScope.launch {
        prefsRepository.setFontFamily(family)
    }

    fun setTextAlign(align: TextAlignOption) = viewModelScope.launch {
        prefsRepository.setTextAlign(align)
    }

    fun setKeepScreenOn(keep: Boolean) = viewModelScope.launch {
        prefsRepository.setKeepScreenOn(keep)
    }

    fun setReadingMode(mode: com.dynamicbookreader.ui.theme.ReadingMode) = viewModelScope.launch {
        prefsRepository.setReadingMode(mode)
    }

    fun setPaperTextureEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setPaperTextureEnabled(enabled)
    }

    // ── Public API: Bookmarks & Notes ────────────────────────────────────────

    fun addBookmark(
        chapterNo: Int,
        chapterTitle: String,
        snippet: String,
        note: String = "",
        scrollFraction: Float = 0f,
        paragraphIndex: Int = 0
    ) = viewModelScope.launch {
        bookmarkRepository.addBookmark(
            chapterNo = chapterNo,
            chapterTitle = chapterTitle,
            snippet = snippet,
            note = note,
            scrollFraction = scrollFraction,
            paragraphIndex = paragraphIndex
        )
    }

    fun deleteBookmark(id: String) = viewModelScope.launch {
        bookmarkRepository.deleteBookmark(id)
    }

    fun updateBookmarkNote(id: String, note: String) = viewModelScope.launch {
        bookmarkRepository.updateBookmarkNote(id, note)
    }

    // ── Public API: Text to Speech (TTS) ─────────────────────────────────────

    fun startTts(paragraphs: List<String>, startIndex: Int = 0) {
        ttsManager.startReading(paragraphs, startIndex)
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun skipTtsNext() {
        ttsManager.skipToNext()
    }

    fun skipTtsPrevious() {
        ttsManager.skipToPrevious()
    }

    fun setTtsSpeechRate(speed: Float) {
        ttsManager.setSpeechRate(speed)
    }

    fun setTtsSleepTimer(minutes: Int?) {
        ttsManager.setSleepTimer(minutes)
    }

    fun cancelTtsSleepTimer() {
        ttsManager.cancelSleepTimer()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

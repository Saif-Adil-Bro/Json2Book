package com.dynamicbookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicbookreader.data.model.Author
import com.dynamicbookreader.data.model.BookData
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.data.model.ContactInfo
import com.dynamicbookreader.data.repository.AuthorRepository
import com.dynamicbookreader.data.repository.BookRepository
import com.dynamicbookreader.data.repository.ContactRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.dynamicbookreader.data.repository.ReadingPreferencesRepository.Companion.DEFAULT_LINE_HEIGHT
import com.dynamicbookreader.data.repository.ReadingProgress
import com.dynamicbookreader.data.repository.ReadingProgressRepository
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.utils.JsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── UI State sealed classes ──────────────────────────────────────────────────

/** State of the full book / chapter-list load (Home screen). */
sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val bookData: BookData) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

/**
 * State of opening a single chapter (triggered by tapping a list item).
 * Kept separate from [BookUiState] so the Home screen's list stays mounted
 * and responsive while a chapter is being resolved — only the Reading
 * screen (or a small overlay) needs to react to this.
 */
sealed class ChapterUiState {
    object Idle : ChapterUiState()
    object Loading : ChapterUiState()
    data class Success(val chapter: Chapter) : ChapterUiState()
    data class Error(val message: String) : ChapterUiState()
}

/** State of the author.json load (used by the hero section + author detail page). */
sealed class AuthorUiState {
    object Loading : AuthorUiState()
    data class Success(val author: Author) : AuthorUiState()
    data class Error(val message: String) : AuthorUiState()
}

/**
 * State of the contact.json load. Lazy-loaded (Idle until the Contact page
 * is first opened) since it's small and rarely-visited — no reason to
 * delay/compete with book + author loading at app start.
 */
sealed class ContactUiState {
    object Idle : ContactUiState()
    object Loading : ContactUiState()
    data class Success(val contactInfo: ContactInfo) : ContactUiState()
    data class Error(val message: String) : ContactUiState()
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
    private val authorRepository = AuthorRepository(application)
    private val contactRepository = ContactRepository(application)
    private val prefsRepository = ReadingPreferencesRepository(application)
    private val progressRepository = ReadingProgressRepository(application)

    // ── Book data state (Home / chapter list) ────────────────────────────────

    private val _uiState = MutableStateFlow<BookUiState>(BookUiState.Loading)
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    // ── Author state (hero section + author detail page) ─────────────────────

    private val _authorUiState = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val authorUiState: StateFlow<AuthorUiState> = _authorUiState.asStateFlow()

    // ── Contact state (Menu → Contact page, lazy-loaded) ──────────────────────

    private val _contactUiState = MutableStateFlow<ContactUiState>(ContactUiState.Idle)
    val contactUiState: StateFlow<ContactUiState> = _contactUiState.asStateFlow()

    // ── Selected / opened chapter state (Reading screen) ─────────────────────

    private val _chapterUiState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val chapterUiState: StateFlow<ChapterUiState> = _chapterUiState.asStateFlow()

    // ── Search (local, in-memory — filters the already-loaded chapters) ──────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Chapters whose title or content match [searchQuery] (case-insensitive).
     * Empty query -> empty result list (search tab shows a prompt instead of
     * dumping the whole book).
     */
    val searchResults: StateFlow<List<Chapter>> = combine(_searchQuery, uiState) { query, book ->
        if (query.isBlank()) return@combine emptyList()
        val chapters = (book as? BookUiState.Success)?.bookData?.chapters ?: return@combine emptyList()
        chapters.filter { chapter ->
            chapter.title.contains(query, ignoreCase = true) ||
                    chapter.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Reading preferences (persisted via DataStore) ────────────────────────

    val fontSize: StateFlow<Float> = prefsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_FONT_SIZE)

    val lineHeight: StateFlow<Float> = prefsRepository.lineHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_LINE_HEIGHT)

    val readingTheme: StateFlow<ReadingTheme> = prefsRepository.readingTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingTheme.DAY)

    // ── Reading progress ("continue reading") ────────────────────────────────

    val readingProgress: StateFlow<ReadingProgress> = progressRepository.readingProgress
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReadingProgress(chapterNo = null, scrollFraction = 0f, chapterTitle = null, updatedAtMillis = 0L)
        )

    /** chapterNo -> scrollFraction (0f..1f) for every chapter with any saved progress. Powers per-card progress rings on Home. */
    val perChapterProgress: StateFlow<Map<Int, Float>> = progressRepository.perChapterProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Debounce scroll-position saves so we don't hammer DataStore on every pixel.
    private var progressSaveJob: Job? = null

    // ── Init: load data immediately ──────────────────────────────────────────

    init {
        loadBook()
        loadAuthor()
    }

    // ── Public API: book / chapter list ──────────────────────────────────────

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

    /** Force re-read from disk, bypassing the in-memory cache (pull-to-retry). */
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

    // ── Public API: author ────────────────────────────────────────────────────

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

    // ── Public API: contact ───────────────────────────────────────────────────

    /**
     * Loads contact.json on demand (called when the Contact screen appears).
     * Skips reloading if already successfully loaded, so re-visiting the
     * page doesn't re-read the asset file every time.
     */
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

    // ── Public API: search ────────────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ── Public API: opening a chapter ────────────────────────────────────────

    /**
     * Called when the user taps a chapter card. Since the book is normally
     * already cached, this usually resolves instantly — but we still route
     * through [ChapterUiState.Loading] so the UI can show a spinner if the
     * cache happens to be cold (first-ever app launch deep link, slow
     * storage, etc.) and an error state if parsing fails.
     */
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

    /** Retry after a failed chapter open. */
    fun retryOpenChapter(chapterNo: Int) = openChapter(chapterNo)

    /** Resets chapter state when leaving the Reading screen. */
    fun clearChapterState() {
        _chapterUiState.value = ChapterUiState.Idle
        progressSaveJob?.cancel()
    }

    // ── Public API: reading progress ─────────────────────────────────────────

    /**
     * Records how far the user has scrolled into the current chapter.
     * Debounced by 600ms so rapid scroll events don't spam DataStore writes.
     * Ignores near-zero scroll so simply opening and immediately leaving a
     * chapter doesn't erase a previously saved deeper position.
     */
    fun updateReadingProgress(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        if (scrollFraction < 0.01f) return
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(600)
            progressRepository.saveProgress(chapterNo, chapterTitle, scrollFraction)
        }
    }

    /**
     * Saves immediately (e.g. when the user navigates away) — no debounce.
     * Same near-zero guard as [updateReadingProgress].
     */
    fun saveReadingProgressNow(chapterNo: Int, chapterTitle: String, scrollFraction: Float) {
        if (scrollFraction < 0.01f) return
        progressSaveJob?.cancel()
        viewModelScope.launch {
            progressRepository.saveProgress(chapterNo, chapterTitle, scrollFraction)
        }
    }

    // ── Public API: reading preferences ──────────────────────────────────────

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
}

package com.dynamicbookreader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.Bookmark
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.ui.components.PagedReadingContent
import com.dynamicbookreader.ui.components.PaperTextureBackground
import com.dynamicbookreader.ui.components.ReadingProgressBubble
import com.dynamicbookreader.ui.theme.ArabicFontFamily
import com.dynamicbookreader.ui.theme.BanglaFontFamily
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.ReadingMode
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.ui.theme.TextAlignOption
import com.dynamicbookreader.utils.*
import com.dynamicbookreader.viewmodel.BookViewModel
import com.dynamicbookreader.viewmodel.ChapterUiState
import kotlinx.coroutines.launch

/**
 * Distraction-free, fully polished reading screen.
 *
 * Core Reader Enhancements:
 * – Dynamic typography with custom Font Families (Default, Serif, Sans, Monospace).
 * – Text alignment selection (Justify, Left, Center).
 * – Four distinct themes: Day, Sepia, Night, and AMOLED Pitch Black.
 * – Keep Screen On / Wake Lock control.
 * – Bookmarking & paragraph note management with instant Snackbar notifications.
 * – Direct Quote / Chapter Excerpt sharing and copying.
 * – Footnote popups with superscript annotations.
 * – Table of contents navigation with live progress indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    chapterNo: Int,
    viewModel: BookViewModel,
    onBack: () -> Unit,
    targetHeading: String? = null,
    onQuoteCardClick: (quote: String, bookTitle: String) -> Unit = { _, _ -> }
) {
    val chapterState by viewModel.chapterUiState.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val banglaFont by viewModel.banglaFont.collectAsState()
    val arabicFont by viewModel.arabicFont.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val textAlign by viewModel.textAlign.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val perChapterProgress by viewModel.perChapterProgress.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val ttsState by viewModel.ttsPlaybackState.collectAsState()
    val ttsSleepTimerMinutes by viewModel.ttsSleepTimerMinutes.collectAsState()
    val ttsSleepTimerSecondsLeft by viewModel.ttsSleepTimerSecondsLeft.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val paperTextureEnabled by viewModel.paperTextureEnabled.collectAsState()
    val bookData by viewModel.bookData.collectAsState()

    val totalChapters = bookData?.chapters?.size ?: 30
    val hasPreviousChapter = chapterNo > 1
    val hasNextChapter = chapterNo < totalChapters

    // Active reading session tracker: every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            viewModel.recordReadingTime(5)
        }
    }

    // Trigger chapter load
    LaunchedEffect(chapterNo) {
        viewModel.openChapter(chapterNo)
    }

    // Clean up when leaving screen
    DisposableEffect(Unit) {
        onDispose { viewModel.clearChapterState() }
    }

    // Keep screen on control
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose {
            view.keepScreenOn = false
        }
    }

    when (val state = chapterState) {
        is ChapterUiState.Idle, is ChapterUiState.Loading -> {
            ReadingLoadingState(onBack = onBack)
        }

        is ChapterUiState.Error -> {
            ReadingErrorState(
                message = state.message,
                onRetry = { viewModel.retryOpenChapter(chapterNo) },
                onBack = onBack
            )
        }

        is ChapterUiState.Success -> {
            ReadingContent(
                chapter = state.chapter,
                fontSize = fontSize,
                lineHeight = lineHeight,
                readingTheme = readingTheme,
                fontFamily = fontFamily,
                banglaFont = banglaFont,
                arabicFont = arabicFont,
                textAlign = textAlign,
                keepScreenOn = keepScreenOn,
                bookmarks = bookmarks,
                targetHeading = targetHeading,
                ttsState = ttsState,
                sleepTimerMinutes = ttsSleepTimerMinutes,
                sleepTimerSecondsLeft = ttsSleepTimerSecondsLeft,
                onSetSleepTimer = { viewModel.setTtsSleepTimer(it) },
                savedScrollFraction = readingProgress
                    .takeIf { it.chapterNo == chapterNo }
                    ?.scrollFraction
                    ?: perChapterProgress[chapterNo],
                onScrollProgressChanged = { fraction ->
                    viewModel.updateReadingProgress(chapterNo, state.chapter.title, fraction)
                },
                onLeaveScreen = { fraction ->
                    viewModel.saveReadingProgressNow(chapterNo, state.chapter.title, fraction)
                },
                onMarkHeadingRead = { headingKey ->
                    viewModel.markHeadingRead(chapterNo, headingKey)
                },
                onAddBookmark = { snippet, note, fraction, paragraphIndex ->
                    viewModel.addBookmark(
                        chapterNo = chapterNo,
                        chapterTitle = state.chapter.title,
                        snippet = snippet,
                        note = note,
                        scrollFraction = fraction,
                        paragraphIndex = paragraphIndex
                    )
                },
                onDeleteBookmark = { bookmarkId ->
                    viewModel.deleteBookmark(bookmarkId)
                },
                onFontIncrease = { viewModel.increaseFontSize() },
                onFontDecrease = { viewModel.decreaseFontSize() },
                onLineHeightIncrease = { viewModel.increaseLineHeight() },
                onLineHeightDecrease = { viewModel.decreaseLineHeight() },
                onThemeChange = { viewModel.setReadingTheme(it) },
                onFontFamilyChange = { viewModel.setFontFamily(it) },
                onBanglaFontChange = { viewModel.setBanglaFont(it) },
                onArabicFontChange = { viewModel.setArabicFont(it) },
                onTextAlignChange = { viewModel.setTextAlign(it) },
                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) },
                readingMode = readingMode,
                paperTextureEnabled = paperTextureEnabled,
                onReadingModeChange = { viewModel.setReadingMode(it) },
                onPaperTextureEnabledChange = { viewModel.setPaperTextureEnabled(it) },
                hasPreviousChapter = hasPreviousChapter,
                hasNextChapter = hasNextChapter,
                onPreviousChapter = {
                    if (hasPreviousChapter) {
                        viewModel.openChapter(chapterNo - 1)
                    }
                },
                onNextChapter = {
                    if (hasNextChapter) {
                        viewModel.openChapter(chapterNo + 1)
                    }
                },
                onStartTts = { paragraphs, idx -> viewModel.startTts(paragraphs, idx) },
                onPauseTts = { viewModel.pauseTts() },
                onResumeTts = { viewModel.resumeTts() },
                onStopTts = { viewModel.stopTts() },
                onNextTts = { viewModel.skipTtsNext() },
                onPreviousTts = { viewModel.skipTtsPrevious() },
                onSetTtsSpeed = { viewModel.setTtsSpeechRate(it) },
                onQuoteCardClick = onQuoteCardClick,
                onBack = onBack
            )
        }
    }
}

// ── Loading state ─────────────────────────────────────────────────────────────

@Composable
private fun ReadingLoadingState(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "অধ্যায় লোড হচ্ছে…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Error state ──────────────────────────────────────────────────────────────

@Composable
private fun ReadingErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 44.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "অধ্যায় লোড করা যায়নি",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("আবার চেষ্টা করুন")
            }
        }
    }
}

// ── Success state: full reading UI ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContent(
    chapter: Chapter,
    fontSize: Float,
    lineHeight: Float,
    readingTheme: ReadingTheme,
    fontFamily: ReadingFontFamily = ReadingFontFamily.SOLAIMAN_LIPI,
    banglaFont: BanglaFontFamily = BanglaFontFamily.SOLAIMAN_LIPI,
    arabicFont: ArabicFontFamily = ArabicFontFamily.AMIRI,
    textAlign: TextAlignOption,
    keepScreenOn: Boolean,
    bookmarks: List<Bookmark>,
    targetHeading: String?,
    ttsState: com.dynamicbookreader.utils.TtsPlaybackState,
    sleepTimerMinutes: Int?,
    sleepTimerSecondsLeft: Int,
    onSetSleepTimer: (Int?) -> Unit,
    savedScrollFraction: Float?,
    onScrollProgressChanged: (Float) -> Unit,
    onLeaveScreen: (Float) -> Unit,
    onMarkHeadingRead: (String) -> Unit,
    onAddBookmark: (snippet: String, note: String, fraction: Float, paragraphIndex: Int) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit,
    onLineHeightIncrease: () -> Unit,
    onLineHeightDecrease: () -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onFontFamilyChange: (ReadingFontFamily) -> Unit = {},
    onBanglaFontChange: (BanglaFontFamily) -> Unit = {},
    onArabicFontChange: (ArabicFontFamily) -> Unit = {},
    onTextAlignChange: (TextAlignOption) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    readingMode: ReadingMode,
    paperTextureEnabled: Boolean,
    onReadingModeChange: (ReadingMode) -> Unit,
    onPaperTextureEnabledChange: (Boolean) -> Unit,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onStartTts: (paragraphs: List<String>, startIndex: Int) -> Unit,
    onPauseTts: () -> Unit,
    onResumeTts: () -> Unit,
    onStopTts: () -> Unit,
    onNextTts: () -> Unit,
    onPreviousTts: () -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onQuoteCardClick: (quote: String, bookTitle: String) -> Unit,
    onBack: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsPanelVisible by remember { mutableStateOf(false) }
    var tocExpanded by remember { mutableStateOf(true) }
    var subtitleExpanded by remember { mutableStateOf(false) }
    var selectedFootnoteKey by remember { mutableStateOf<String?>(null) }
    var selectedParagraphForAction by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showAddNoteDialogForParagraph by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isChapterBookmarked = remember(bookmarks, chapter.chapterNo) {
        bookmarks.any { it.chapterNo == chapter.chapterNo }
    }

    val subtitleRotation by animateFloatAsState(
        targetValue = if (subtitleExpanded) 180f else 0f,
        label = "subtitle_chevron_rotation"
    )

    // Quick key -> text lookup for footnotes
    val footnoteLookup = remember(chapter.chapterNo) {
        chapter.footnotes.associateBy { it.key }
    }

    val parsedChapter = remember(chapter.chapterNo) {
        ChapterContentParser.getOrParse(chapter.chapterNo, chapter.content, chapter.title)
    }
    val readableParagraphs = remember(parsedChapter) {
        parsedChapter.blocks.map { it.plainText }
    }
    val hasToc = parsedChapter.tocEntries.isNotEmpty()
    val totalItemCount = parsedChapter.blocks.size + 2 + (if (hasToc) 1 else 0)

    val initialTargetIndex = remember(chapter.chapterNo, targetHeading) {
        if (targetHeading.isNullOrBlank() && savedScrollFraction != null && savedScrollFraction > 0.01f && totalItemCount > 1) {
            (savedScrollFraction * (totalItemCount - 1)).toInt().coerceIn(0, totalItemCount - 1)
        } else {
            0
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialTargetIndex)
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val headingIndexMap = remember(parsedChapter) {
        val map = HashMap<String, Int>()
        val bodyStartIndex = 1 + (if (hasToc) 1 else 0)
        parsedChapter.blocks.forEachIndexed { i, block ->
            if (block.headingKey != null && block.headingKey !in map) {
                map[block.headingKey] = bodyStartIndex + i
            }
        }
        map
    }

    val tocEntryToHeadingKey = remember(parsedChapter) {
        parsedChapter.tocEntries.associateWith { ChapterContentParser.headingKey(it, 0) }
    }

    var hasAutoRestoredScroll by remember(chapter.chapterNo) { mutableStateOf(false) }

    LaunchedEffect(chapter.chapterNo, targetHeading, savedScrollFraction) {
        if (!targetHeading.isNullOrBlank()) {
            val headingKey = tocEntryToHeadingKey[targetHeading] ?: return@LaunchedEffect
            val targetIndex = headingIndexMap[headingKey] ?: return@LaunchedEffect
            kotlinx.coroutines.delay(50)
            listState.scrollToItem(targetIndex)
        } else if (!hasAutoRestoredScroll && savedScrollFraction != null && savedScrollFraction > 0.01f && totalItemCount > 1) {
            hasAutoRestoredScroll = true
            val targetIndex = (savedScrollFraction * (totalItemCount - 1))
                .toInt()
                .coerceIn(0, totalItemCount - 1)
            if (targetIndex > 0) {
                kotlinx.coroutines.delay(50)
                listState.scrollToItem(targetIndex)
            }
        }
    }

    // Auto-scroll when TTS reads next paragraph
    LaunchedEffect(ttsState.currentParagraphIndex, ttsState.isPlaying) {
        if (ttsState.isPlaying && ttsState.currentParagraphIndex in 0 until parsedChapter.blocks.size) {
            val bodyStartIndex = 1 + (if (hasToc) 1 else 0)
            val targetIndex = bodyStartIndex + ttsState.currentParagraphIndex
            listState.animateScrollToItem(targetIndex)
        }
    }

    var pagedReadingFraction by remember(chapter.chapterNo) {
        mutableFloatStateOf(savedScrollFraction ?: 0f)
    }
    var pagedIsScrolling by remember { mutableStateOf(false) }

    var maxFractionReached by remember(chapter.chapterNo) {
        mutableFloatStateOf(savedScrollFraction ?: 0f)
    }

    val currentFraction by remember(readingMode, pagedReadingFraction) {
        derivedStateOf {
            if (readingMode == ReadingMode.PAGE_FLIP) {
                pagedReadingFraction
            } else {
                val layoutInfo = listState.layoutInfo
                val total = layoutInfo.totalItemsCount
                if (total <= 1) return@derivedStateOf 0f

                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@derivedStateOf 0f

                val lastVisible = visibleItems.last()
                // If end marker (total - 2) or nav buttons (total - 1) are in view, chapter is 100% completed
                if (lastVisible.index >= total - 2) {
                    return@derivedStateOf 1f
                }

                val firstIndex = listState.firstVisibleItemIndex
                val firstOffset = listState.firstVisibleItemScrollOffset
                val firstItemSize = visibleItems.firstOrNull()?.size ?: 1
                val fractionalOffset = if (firstItemSize > 0) firstOffset.toFloat() / firstItemSize else 0f

                val visibleCount = visibleItems.size
                val maxScrollable = (total - visibleCount + 1).coerceAtLeast(1)
                ((firstIndex + fractionalOffset) / maxScrollable).coerceIn(0f, 1f)
            }
        }
    }

    val effectiveFraction by remember(currentFraction, maxFractionReached) {
        derivedStateOf {
            maxOf(currentFraction, maxFractionReached)
        }
    }

    LaunchedEffect(currentFraction) {
        if (currentFraction > maxFractionReached) {
            maxFractionReached = currentFraction
        }
        val eff = maxOf(currentFraction, maxFractionReached)
        onScrollProgressChanged(eff)
    }

    val latestEffectiveFraction by rememberUpdatedState(effectiveFraction)

    BackHandler {
        onLeaveScreen(latestEffectiveFraction)
        onBack()
    }

    val sortedHeadingEntries = remember(headingIndexMap) {
        headingIndexMap.entries.sortedBy { it.value }
    }
    val alreadyReported = remember(chapter.chapterNo) { mutableSetOf<String>() }

    LaunchedEffect(listState, sortedHeadingEntries) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { currentIndex ->
                for (i in sortedHeadingEntries.indices) {
                    val (key, index) = sortedHeadingEntries[i]
                    if (key in alreadyReported) continue
                    val nextIndex = sortedHeadingEntries.getOrNull(i + 1)?.value
                    val passed = if (nextIndex != null) {
                        currentIndex >= nextIndex
                    } else {
                        currentIndex >= index
                    }
                    if (passed) {
                        alreadyReported.add(key)
                        onMarkHeadingRead(key)
                    }
                }
            }
    }

    DisposableEffect(chapter.chapterNo) {
        onDispose {
            onLeaveScreen(latestEffectiveFraction)
        }
    }

    PaperTextureBackground(
        readingTheme = readingTheme,
        paperTextureEnabled = paperTextureEnabled
    ) {
        if (readingMode == ReadingMode.PAGE_FLIP) {
            PagedReadingContent(
                chapter = chapter,
                parsedChapter = parsedChapter,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                banglaFont = banglaFont,
                arabicFont = arabicFont,
                textAlign = textAlign,
                textColor = textColor,
                ttsState = ttsState,
                initialFraction = savedScrollFraction ?: 0f,
                onPageProgressChanged = { fraction, isScrolling ->
                    pagedReadingFraction = fraction
                    pagedIsScrolling = isScrolling
                    onScrollProgressChanged(fraction)
                },
                onParagraphLongPress = { idx, text ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedParagraphForAction = Pair(idx, text)
                },
                onFootnoteClick = { key ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedFootnoteKey = key
                },
                onQuoteCardClick = onQuoteCardClick,
                onNextChapter = onNextChapter,
                onPreviousChapter = onPreviousChapter,
                hasPreviousChapter = hasPreviousChapter,
                hasNextChapter = hasNextChapter,
                controlsVisible = controlsVisible,
                onToggleControls = {
                    controlsVisible = !controlsVisible
                }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!settingsPanelVisible) {
                                    controlsVisible = !controlsVisible
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(
                    top = if (controlsVisible) 76.dp else 24.dp,
                    bottom = if (ttsState.isPlaying || ttsState.isPaused) 160.dp else if (controlsVisible) 100.dp else 40.dp,
                    start = 20.dp,
                    end = 20.dp
                )
            ) {
                // Header item
                item(key = "header") {
                    SelectionContainer {
                        Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "অধ্যায় ${chapter.chapterNo}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = banglaFont.fontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (chapter.pageRange.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "পাতা ${chapter.pageRange}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = banglaFont.fontFamily
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = banglaFont.fontFamily,
                                        fontSize = (fontSize + 4).sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = (fontSize + 14).sp,
                                        color = textColor
                                    )
                                )
                                if (chapter.subtitle.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    DisableSelection {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { subtitleExpanded = !subtitleExpanded }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "সংক্ষিপ্ত বিবরণ",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = banglaFont.fontFamily
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = if (subtitleExpanded) "সংক্ষিপ্ত করুন" else "বিস্তারিত দেখুন",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .rotate(subtitleRotation)
                                            )
                                        }
                                    }
                                    AnimatedVisibility(visible = subtitleExpanded) {
                                        Text(
                                            text = chapter.subtitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = banglaFont.fontFamily,
                                                fontSize = (fontSize - 2).coerceAtLeast(12f).sp,
                                                lineHeight = (fontSize + 4).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(if (hasToc) 16.dp else 24.dp))
                            }
                        }
                    }

                    // Collapsible ToC
                    if (hasToc) {
                        item(key = "toc_card") {
                            TocCard(
                                entries = parsedChapter.tocEntries,
                                fontFamily = fontFamily,
                                banglaFont = banglaFont,
                                expanded = tocExpanded,
                                onToggleExpanded = { tocExpanded = !tocExpanded },
                                        onEntryClick = { entry ->
                                            val headingKey = tocEntryToHeadingKey[entry]
                                            val targetIndex = headingKey?.let { headingIndexMap[it] }
                                            if (targetIndex != null) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(targetIndex)
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )
                                }
                        } else {
                            item(key = "header_divider") {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    thickness = 1.5.dp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                            }
                        }

                        // Paragraphs
                        itemsIndexed(
                            items = parsedChapter.blocks,
                            key = { index, block -> block.headingKey ?: "para_$index" }
                        ) { index, block ->
                            val isHeading = block.headingKey != null
                            val isTtsSpeakingThis = (ttsState.isPlaying || ttsState.isPaused) && ttsState.currentParagraphIndex == index

                            val baseStyle = if (isHeading) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = banglaFont.fontFamily,
                                    fontSize = (fontSize + 1).sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (fontSize * lineHeight).sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = textAlign.align
                                )
                            } else {
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = banglaFont.fontFamily,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineHeight).sp,
                                    color = textColor,
                                    textAlign = textAlign.align
                                )
                            }

                            val hasFootnotes = block.segments.any { it is ChapterContentParser.TextSegment.FootnoteRef }
                            val footnoteColor = MaterialTheme.colorScheme.primary
                            val annotated = remember(block, banglaFont, arabicFont, footnoteColor, fontSize) {
                                if (hasFootnotes) {
                                    BilingualTextHelper.buildContentBlockAnnotatedString(
                                        segments = block.segments,
                                        banglaFont = banglaFont.fontFamily,
                                        arabicFont = arabicFont.fontFamily,
                                        footnoteColor = footnoteColor,
                                        fontSize = fontSize
                                    )
                                } else {
                                    BilingualTextHelper.buildBilingualAnnotatedString(
                                        text = block.plainText,
                                        banglaFont = banglaFont.fontFamily,
                                        arabicFont = arabicFont.fontFamily,
                                        baseFontSizeSp = if (isHeading) fontSize + 1 else fontSize
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = if (isHeading) 8.dp else 0.dp,
                                        bottom = 16.dp
                                    )
                                    .then(
                                        if (isTtsSpeakingThis) {
                                            Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                .padding(10.dp)
                                        } else Modifier
                                    )
                                    .pointerInput(block.plainText) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedParagraphForAction = Pair(index, block.plainText)
                                            },
                                            onTap = {
                                                if (!settingsPanelVisible) {
                                                    controlsVisible = !controlsVisible
                                                }
                                            }
                                        )
                                    }
                            ) {
                                SelectionContainer {
                                    if (!hasFootnotes) {
                                        Text(
                                            text = annotated,
                                            style = baseStyle
                                        )
                                    } else {
                                        FootnoteAwareText(
                                            text = annotated,
                                            style = baseStyle,
                                            onFootnoteClick = { key ->
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedFootnoteKey = key
                                            },
                                            onNonFootnoteClick = {
                                                if (!settingsPanelVisible) {
                                                    controlsVisible = !controlsVisible
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // End marker
                        item(key = "end_marker") {
                            Column {
                                Spacer(Modifier.height(32.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "﴿ সমাপ্ত ﴾",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = banglaFont.fontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Bottom Chapter Navigation Buttons (Scroll Mode)
                        item(key = "chapter_nav_buttons") {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (hasPreviousChapter) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLeaveScreen(latestEffectiveFraction)
                                            onPreviousChapter()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("পূর্ববর্তী অধ্যায়")
                                    }
                                }
                                if (hasNextChapter) {
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLeaveScreen(latestEffectiveFraction)
                                            onNextChapter()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("পরবর্তী অধ্যায়")
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
        }

        // ── Top App Bar ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        onLeaveScreen(latestEffectiveFraction)
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Audio Reader (TTS) button
                    IconButton(
                        onClick = {
                            if (ttsState.isPlaying) {
                                onPauseTts()
                            } else if (ttsState.isPaused) {
                                onResumeTts()
                            } else {
                                onStartTts(readableParagraphs, 0)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "অডিও প্লেয়ার (TTS)",
                            tint = if (ttsState.isPlaying || ttsState.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Quick Bookmark Toggle Button
                    IconButton(
                        onClick = {
                            val firstPara = parsedChapter.blocks.firstOrNull()?.plainText ?: chapter.title
                            onAddBookmark(firstPara.take(200), "", currentFraction, 0)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("অধ্যায়টি বুকমার্কে সংরক্ষিত হয়েছে")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isChapterBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "বুকমার্ক করুন",
                            tint = if (isChapterBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share chapter excerpt
                    IconButton(
                        onClick = {
                            val excerpt = parsedChapter.blocks.firstOrNull()?.plainText?.take(300) ?: ""
                            val shareText = "📖 ${chapter.title} (অধ্যায় ${chapter.chapterNo})\n\n\"$excerpt...\"\n\n— আর-রাহীকুল মাখতূম"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "অধ্যায় শেয়ার করুন"))
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "শেয়ার করুন",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings Button
                    IconButton(onClick = { settingsPanelVisible = !settingsPanelVisible }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "পাঠ সেটিংস",
                            tint = if (settingsPanelVisible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ── Scroll progress indicator ───────────────────────────────────────
        if (totalItemCount > 1) {
            LinearProgressIndicator(
                progress = { effectiveFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // ── Floating Reading Progress Bubble (Scroll & Read Indicator) ─────
        ReadingProgressBubble(
            progressFraction = effectiveFraction,
            isScrolling = if (readingMode == ReadingMode.PAGE_FLIP) pagedIsScrolling else listState.isScrollInProgress,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // ── Percentage read badge ────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible && totalItemCount > 1 && !settingsPanelVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 24.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "${(effectiveFraction * 100).toInt()}% পঠিত",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // ── Floating TTS Player Bar ─────────────────────────────────────────
        AnimatedVisibility(
            visible = (ttsState.isPlaying || ttsState.isPaused) && !settingsPanelVisible,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            TtsPlayerBottomBar(
                ttsState = ttsState,
                totalParagraphs = readableParagraphs.size,
                sleepTimerMinutes = sleepTimerMinutes,
                sleepTimerSecondsLeft = sleepTimerSecondsLeft,
                onSetSleepTimer = onSetSleepTimer,
                onPlayPause = {
                    if (ttsState.isPlaying) onPauseTts() else onResumeTts()
                },
                onPrevious = onPreviousTts,
                onNext = onNextTts,
                onSpeedChange = onSetTtsSpeed,
                onClose = onStopTts
            )
        }

        // ── Snackbar Host ───────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // ── Reading Settings Panel ──────────────────────────────────────────
        AnimatedVisibility(
            visible = settingsPanelVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReadingSettingsPanel(
                fontSize = fontSize,
                lineHeight = lineHeight,
                currentTheme = readingTheme,
                currentFontFamily = fontFamily,
                currentBanglaFont = banglaFont,
                currentArabicFont = arabicFont,
                currentTextAlign = textAlign,
                keepScreenOn = keepScreenOn,
                currentReadingMode = readingMode,
                paperTextureEnabled = paperTextureEnabled,
                onFontIncrease = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFontIncrease()
                },
                onFontDecrease = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFontDecrease()
                },
                onLineHeightIncrease = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLineHeightIncrease()
                },
                onLineHeightDecrease = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLineHeightDecrease()
                },
                onThemeChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onThemeChange(it)
                },
                onFontFamilyChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFontFamilyChange(it)
                },
                onBanglaFontChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBanglaFontChange(it)
                },
                onArabicFontChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onArabicFontChange(it)
                },
                onTextAlignChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTextAlignChange(it)
                },
                onKeepScreenOnChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onKeepScreenOnChange(it)
                },
                onReadingModeChange = onReadingModeChange,
                onPaperTextureEnabledChange = onPaperTextureEnabledChange,
                onDismiss = { settingsPanelVisible = false }
            )
        }
    }

    // ── Footnote Dialog ─────────────────────────────────────────────────────
    val activeFootnote = selectedFootnoteKey?.let { footnoteLookup[it] }
    if (selectedFootnoteKey != null) {
        FootnoteDialog(
            footnoteText = activeFootnote?.text,
            onDismiss = { selectedFootnoteKey = null }
        )
    }

    // ── Paragraph Action Sheet (Copy, Share, Bookmark, Add Note) ────────────
    selectedParagraphForAction?.let { (paraIdx, paraText) ->
        ModalBottomSheet(
            onDismissRequest = { selectedParagraphForAction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "অনুচ্ছেদ বিকল্প",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\"${paraText.take(120)}…\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                Spacer(Modifier.height(16.dp))

                // Quote Card Generator Action
                ListItem(
                    headlineContent = { Text("কোট ফটো কার্ড তৈরি করুন") },
                    supportingContent = { Text("উক্তি দিয়ে আকর্ষণীয় ফটো কার্ড বানিয়ে শেয়ার করুন") },
                    leadingContent = { Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        val textToQuote = paraText
                        selectedParagraphForAction = null
                        onQuoteCardClick(textToQuote, chapter.title)
                    }
                )

                // Audio Read TTS Action
                ListItem(
                    headlineContent = { Text("এখান থেকে অডিও শুনুন") },
                    supportingContent = { Text("বাংলা অডিওতে অনুচ্ছেদটি শুনুন") },
                    leadingContent = { Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    modifier = Modifier.clickable {
                        onStartTts(readableParagraphs, paraIdx)
                        selectedParagraphForAction = null
                    }
                )

                // Copy Action
                ListItem(
                    headlineContent = { Text("কপি করুন") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Chapter Text", paraText)
                        clipboard.setPrimaryClip(clip)
                        selectedParagraphForAction = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("টেক্সট ক্লিপবোর্ডে কপি করা হয়েছে")
                        }
                    }
                )

                // Share Quote Action
                ListItem(
                    headlineContent = { Text("উক্তি শেয়ার করুন") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val shareText = "📖 ${chapter.title}\n\n\"$paraText\"\n\n— আর-রাহীকুল মাখতূম"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "উক্তি শেয়ার করুন"))
                        selectedParagraphForAction = null
                    }
                )

                // Bookmark & Add Note Action
                ListItem(
                    headlineContent = { Text("বুকমার্ক ও নোট যুক্ত করুন") },
                    leadingContent = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val selected = selectedParagraphForAction
                        selectedParagraphForAction = null
                        showAddNoteDialogForParagraph = selected
                    }
                )
            }
        }
    }

    // ── Add Note Dialog for Paragraph ───────────────────────────────────────
    showAddNoteDialogForParagraph?.let { (paraIdx, paraText) ->
        var noteInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialogForParagraph = null },
            icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
            title = { Text("বুকমার্ক ও নোট") },
            text = {
                Column {
                    Text(
                        text = "\"${paraText.take(100)}…\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("ব্যক্তিগত নোট বা মন্তব্য (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBookmark(paraText.take(250), noteInput, currentFraction, paraIdx)
                        showAddNoteDialogForParagraph = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("বুকমার্ক ও নোট সংরক্ষিত হয়েছে")
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialogForParagraph = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ── Footnote Dialog ─────────────────────────────────────────────────────────

@Composable
private fun FootnoteDialog(
    footnoteText: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("টীকা") },
        text = {
            Text(
                text = footnoteText ?: "এই টীকার তথ্য পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

// ── Footnote-aware text renderer ────────────────────────────────────────────

@Composable
internal fun FootnoteAwareText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    onFootnoteClick: (String) -> Unit,
    onNonFootnoteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    Text(
        text = text,
        style = style,
        modifier = modifier.pointerInput(text) {
            detectTapGestures(
                onTap = { offset ->
                    val result = layoutResult
                    if (result != null) {
                        val position = result.getOffsetForPosition(offset)
                        val footnote = text.getStringAnnotations(tag = "footnote", start = position, end = position)
                            .firstOrNull()
                        if (footnote != null) {
                            onFootnoteClick(footnote.item)
                            return@detectTapGestures
                        }
                    }
                    onNonFootnoteClick?.invoke()
                }
            )
        },
        onTextLayout = { layoutResult = it }
    )
}

// ── Table of Contents Card ──────────────────────────────────────────────────

@Composable
private fun TocCard(
    entries: List<String>,
    fontFamily: ReadingFontFamily = ReadingFontFamily.SOLAIMAN_LIPI,
    banglaFont: BanglaFontFamily = BanglaFontFamily.SOLAIMAN_LIPI,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "toc_chevron_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "সূচিপত্র",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = banglaFont.fontFamily
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${entries.size} টি",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "সঙ্কুচিত করুন" else "সম্প্রসারিত করুন",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    entries.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEntryClick(entry) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = banglaFont.fontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = banglaFont.fontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Reading Settings Panel ───────────────────────────────────────────────────

@Composable
private fun ReadingSettingsPanel(
    fontSize: Float,
    lineHeight: Float,
    currentTheme: ReadingTheme,
    currentFontFamily: ReadingFontFamily = ReadingFontFamily.SOLAIMAN_LIPI,
    currentBanglaFont: BanglaFontFamily = BanglaFontFamily.SOLAIMAN_LIPI,
    currentArabicFont: ArabicFontFamily = ArabicFontFamily.AMIRI,
    currentTextAlign: TextAlignOption,
    keepScreenOn: Boolean,
    currentReadingMode: ReadingMode,
    paperTextureEnabled: Boolean,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit,
    onLineHeightIncrease: () -> Unit,
    onLineHeightDecrease: () -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onFontFamilyChange: (ReadingFontFamily) -> Unit = {},
    onBanglaFontChange: (BanglaFontFamily) -> Unit = {},
    onArabicFontChange: (ArabicFontFamily) -> Unit = {},
    onTextAlignChange: (TextAlignOption) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onPaperTextureEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পাঠ সেটিংস ও নান্দনিকতা",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Reading Mode (Scroll vs Page Flip)
            Text(
                text = "পড়ার ধরণ (Reading Mode)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReadingMode.entries.forEach { mode ->
                    val selected = mode == currentReadingMode
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReadingModeChange(mode)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = mode.emoji, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Paper & Parchment Texture Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "পেপার টেক্সচার মোড (Paper & Parchment)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "চোখের আরামের জন্য হালকা এন্টিক পেপার ও মার্জিন ফ্রেম",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = paperTextureEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPaperTextureEnabledChange(it)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Font size row
            SettingsRow(label = "ফন্ট সাইজ: ${fontSize.toInt()}sp") {
                ControlButton(label = "A−", onClick = onFontDecrease)
                Spacer(Modifier.width(8.dp))
                ControlButton(label = "A+", onClick = onFontIncrease, large = true)
            }

            Spacer(Modifier.height(12.dp))

            // Line height row
            SettingsRow(label = "লাইন উচ্চতা: ${"%.1f".format(lineHeight)}×") {
                ControlButton(label = "−", onClick = onLineHeightDecrease)
                Spacer(Modifier.width(8.dp))
                ControlButton(label = "+", onClick = onLineHeightIncrease)
            }

            Spacer(Modifier.height(16.dp))

            // ── Bangla Font Selection ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "বাংলা ফন্ট শৈলী (Bangla Font)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "বাংলা ৪টি",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BanglaFontFamily.entries.chunked(2).forEach { rowFonts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowFonts.forEach { family ->
                            val selected = family == currentBanglaFont
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBanglaFontChange(family)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = family.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = family.fontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (family.isDefault) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "ডিফল্ট",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = family.sampleText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = family.fontFamily
                                        ),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        if (rowFonts.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Arabic Font Selection ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "আরবি ফন্ট শৈলী (Arabic Font)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "আরবি ২টি",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArabicFontFamily.entries.forEach { family ->
                    val selected = family == currentArabicFont
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onArabicFontChange(family)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = family.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = family.fontFamily
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (family.isDefault) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ডিফল্ট",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = family.sampleText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = family.fontFamily
                                ),
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Text Alignment row
            Text(
                text = "টেক্সট অ্যালাইনমেন্ট",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextAlignOption.entries.forEach { alignOpt ->
                    val selected = alignOpt == currentTextAlign
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTextAlignChange(alignOpt)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = alignOpt.emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = alignOpt.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Theme grid (2 rows of 3)
            Text(
                text = "রিডিং থিম",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingTheme.entries.chunked(3).forEach { themeRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeRow.forEach { theme ->
                            val selected = theme == currentTheme
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onThemeChange(theme)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = theme.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = theme.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Keep Screen On Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "স্ক্রিন চালু রাখুন",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "পড়ার সময় স্ক্রিনের আলো নিভে যাবে না",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKeepScreenOnChange(it)
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsRow(label: String, controls: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(content = controls)
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit, large: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(
            text = label,
            fontSize = if (large) 18.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── TTS Audio Player Floating Bottom Bar ────────────────────────────────────

@Composable
private fun TtsPlayerBottomBar(
    ttsState: com.dynamicbookreader.utils.TtsPlaybackState,
    totalParagraphs: Int,
    sleepTimerMinutes: Int?,
    sleepTimerSecondsLeft: Int,
    onSetSleepTimer: (Int?) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val sleepOptions = listOf(
        Pair("বন্ধ", null),
        Pair("৫ মিনিট", 5),
        Pair("১০ মিনিট", 10),
        Pair("১৫ মিনিট", 15),
        Pair("৩০ মিনিট", 30),
        Pair("৪৫ মিনিট", 45),
        Pair("৬০ মিনিট", 60)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Waveform or Headphones Icon
                if (ttsState.isPlaying) {
                    AudioWaveformIndicator(modifier = Modifier.size(24.dp, 18.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (ttsState.isInitializing) "অডিও ইঞ্জিন চালু হচ্ছে…"
                    else "অডিও পাঠ: অনুচ্ছেদ ${(ttsState.currentParagraphIndex + 1).coerceAtLeast(1)}/$totalParagraphs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Sleep Timer Button
                Box {
                    AssistChip(
                        onClick = { showSleepMenu = true },
                        leadingIcon = {
                            Icon(
                                imageVector = if (sleepTimerMinutes != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                                contentDescription = "স্লিপ টাইমার",
                                modifier = Modifier.size(16.dp),
                                tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            val text = if (sleepTimerMinutes != null) {
                                val mins = sleepTimerSecondsLeft / 60
                                val secs = sleepTimerSecondsLeft % 60
                                String.format("%02d:%02d", mins, secs)
                            } else "টাইমার"
                            Text(text, fontSize = 11.sp)
                        },
                        modifier = Modifier.height(28.dp)
                    )
                    DropdownMenu(
                        expanded = showSleepMenu,
                        onDismissRequest = { showSleepMenu = false }
                    ) {
                        Text(
                            text = "⏲️ স্লিপ টাইমার নির্ধারণ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()
                        sleepOptions.forEach { (label, mins) ->
                            val isSelected = sleepTimerMinutes == mins
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSetSleepTimer(mins)
                                    showSleepMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Speed Dropdown
                Box {
                    AssistChip(
                        onClick = { showSpeedMenu = true },
                        label = { Text("${ttsState.speechRate}x", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        speeds.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x গতি") },
                                onClick = {
                                    onSpeedChange(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Progress bar
            val audioProgress = if (totalParagraphs > 0) {
                ((ttsState.currentParagraphIndex + 1).toFloat() / totalParagraphs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { audioProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(8.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = ttsState.currentParagraphIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "পূর্ববর্তী অনুচ্ছেদ",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (ttsState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (ttsState.isPlaying) "পজ" else "প্লে",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                IconButton(
                    onClick = onNext,
                    enabled = ttsState.currentParagraphIndex < totalParagraphs - 1
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "পরবর্তী অনুচ্ছেদ",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val bar1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3, bar4).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}


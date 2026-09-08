package com.dynamicbookreader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.ui.theme.TextAlignOption
import com.dynamicbookreader.utils.ChapterContentParser
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
    targetHeading: String? = null
) {
    val chapterState by viewModel.chapterUiState.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val textAlign by viewModel.textAlign.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

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
                textAlign = textAlign,
                keepScreenOn = keepScreenOn,
                bookmarks = bookmarks,
                targetHeading = targetHeading,
                savedScrollFraction = readingProgress
                    .takeIf { it.chapterNo == chapterNo }
                    ?.scrollFraction,
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
                onTextAlignChange = { viewModel.setTextAlign(it) },
                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) },
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
    fontFamily: ReadingFontFamily,
    textAlign: TextAlignOption,
    keepScreenOn: Boolean,
    bookmarks: List<Bookmark>,
    targetHeading: String?,
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
    onFontFamilyChange: (ReadingFontFamily) -> Unit,
    onTextAlignChange: (TextAlignOption) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
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
        ChapterContentParser.parse(chapter.content, chapter.title)
    }
    val hasToc = parsedChapter.tocEntries.isNotEmpty()
    val totalItemCount = parsedChapter.blocks.size + 2 + (if (hasToc) 1 else 0)

    val listState = rememberLazyListState()
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

    LaunchedEffect(chapter.chapterNo, targetHeading) {
        if (targetHeading.isNullOrBlank()) return@LaunchedEffect
        val headingKey = tocEntryToHeadingKey[targetHeading] ?: return@LaunchedEffect
        val targetIndex = headingIndexMap[headingKey] ?: return@LaunchedEffect
        kotlinx.coroutines.delay(50)
        listState.scrollToItem(targetIndex)
    }

    val currentFraction by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total <= 1) return@derivedStateOf 0f

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisible != null && lastVisible.index == total - 1) {
                val viewportEnd = layoutInfo.viewportEndOffset
                val itemBottom = lastVisible.offset + lastVisible.size
                if (itemBottom <= viewportEnd) return@derivedStateOf 1f
            }

            val firstIndex = listState.firstVisibleItemIndex
            firstIndex.toFloat() / (total - 1).toFloat()
        }
    }

    LaunchedEffect(currentFraction) {
        onScrollProgressChanged(currentFraction)
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

    DisposableEffect(Unit) {
        onDispose { onLeaveScreen(currentFraction) }
    }

    val showResumeButton = savedScrollFraction != null &&
            savedScrollFraction > 0.02f &&
            listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0

    var selectionResetKey by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(settingsPanelVisible) {
                detectTapGestures {
                    selectionResetKey++
                    if (!settingsPanelVisible) {
                        controlsVisible = !controlsVisible
                    }
                }
            }
    ) {
        key(selectionResetKey) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = if (controlsVisible) 76.dp else 24.dp,
                        bottom = if (controlsVisible) 100.dp else 40.dp,
                        start = 20.dp,
                        end = 20.dp
                    )
                ) {
                    // Header item
                    item(key = "header") {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "অধ্যায় ${chapter.chapterNo}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontFamily.fontFamily
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
                                                fontFamily = fontFamily.fontFamily
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
                                    fontFamily = fontFamily.fontFamily,
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
                                                fontFamily = fontFamily.fontFamily
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
                                            fontFamily = fontFamily.fontFamily,
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

                    // Collapsible ToC
                    if (hasToc) {
                        item(key = "toc_card") {
                            DisableSelection {
                                TocCard(
                                    entries = parsedChapter.tocEntries,
                                    fontFamily = fontFamily,
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
                        val baseStyle = if (isHeading) {
                            MaterialTheme.typography.titleMedium.copy(
                                fontFamily = fontFamily.fontFamily,
                                fontSize = (fontSize + 1).sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (fontSize * lineHeight).sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = textAlign.align
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = fontFamily.fontFamily,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineHeight).sp,
                                color = textColor,
                                textAlign = textAlign.align
                            )
                        }

                        val hasFootnotes = block.segments.any { it is ChapterContentParser.TextSegment.FootnoteRef }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (isHeading) 8.dp else 0.dp,
                                    bottom = 16.dp
                                )
                                .pointerInput(block.plainText) {
                                    detectTapGestures(
                                        onLongPress = {
                                            selectedParagraphForAction = Pair(index, block.plainText)
                                        }
                                    )
                                }
                        ) {
                            if (!hasFootnotes) {
                                Text(
                                    text = block.plainText,
                                    style = baseStyle
                                )
                            } else {
                                val footnoteColor = MaterialTheme.colorScheme.primary
                                val annotated = remember(block, footnoteColor, fontSize) {
                                    buildAnnotatedString {
                                        block.segments.forEach { segment ->
                                            when (segment) {
                                                is ChapterContentParser.TextSegment.Plain ->
                                                    append(segment.text)
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
                                                            fontSize = (fontSize * 0.7f).sp
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
                                FootnoteAwareText(
                                    text = annotated,
                                    style = baseStyle,
                                    onFootnoteClick = { key -> selectedFootnoteKey = key }
                                )
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
                                        fontFamily = fontFamily.fontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                    IconButton(onClick = onBack) {
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
                            val shareText = "📖 ${chapter.title} (অধ্যায় ${chapter.chapterNo})\n\n\"$excerpt...\"\n\n— Dynamic Book Reader"
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

        // ── Resume Position FAB ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showResumeButton && !settingsPanelVisible,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.8f),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    savedScrollFraction?.let { fraction ->
                        coroutineScope.launch {
                            val targetIndex = (fraction * (totalItemCount - 1))
                                .toInt()
                                .coerceIn(0, totalItemCount - 1)
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                },
                text = { Text("শেষ পঠিত অংশে যান") }
            )
        }

        // ── Scroll progress indicator ───────────────────────────────────────
        if (totalItemCount > 1) {
            LinearProgressIndicator(
                progress = { currentFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

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
                    text = "${(currentFraction * 100).toInt()}% পঠিত",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
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
                currentTextAlign = textAlign,
                keepScreenOn = keepScreenOn,
                onFontIncrease = onFontIncrease,
                onFontDecrease = onFontDecrease,
                onLineHeightIncrease = onLineHeightIncrease,
                onLineHeightDecrease = onLineHeightDecrease,
                onThemeChange = onThemeChange,
                onFontFamilyChange = onFontFamilyChange,
                onTextAlignChange = onTextAlignChange,
                onKeepScreenOnChange = onKeepScreenOnChange,
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
                        val shareText = "📖 ${chapter.title}\n\n\"$paraText\"\n\n— Dynamic Book Reader"
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
private fun FootnoteAwareText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    onFootnoteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    Text(
        text = text,
        style = style,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { offset ->
                val result = layoutResult ?: return@detectTapGestures
                val position = result.getOffsetForPosition(offset)
                text.getStringAnnotations(tag = "footnote", start = position, end = position)
                    .firstOrNull()
                    ?.let { onFootnoteClick(it.item) }
            }
        },
        onTextLayout = { layoutResult = it }
    )
}

// ── Table of Contents Card ──────────────────────────────────────────────────

@Composable
private fun TocCard(
    entries: List<String>,
    fontFamily: ReadingFontFamily,
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
                        fontFamily = fontFamily.fontFamily
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
                                    fontFamily = fontFamily.fontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = fontFamily.fontFamily
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
    currentFontFamily: ReadingFontFamily,
    currentTextAlign: TextAlignOption,
    keepScreenOn: Boolean,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit,
    onLineHeightIncrease: () -> Unit,
    onLineHeightDecrease: () -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onFontFamilyChange: (ReadingFontFamily) -> Unit,
    onTextAlignChange: (TextAlignOption) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = "পাঠ সেটিংস",
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

            // Font Family row
            Text(
                text = "ফন্ট শৈলী (Font Family)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingFontFamily.entries.forEach { family ->
                    val selected = family == currentFontFamily
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFontFamilyChange(family) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = family.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = family.subtitle,
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
                            .clickable { onTextAlignChange(alignOpt) }
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

            // Theme row
            Text(
                text = "রিডিং থিম",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingTheme.entries.forEach { theme ->
                    val selected = theme == currentTheme
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onThemeChange(theme) }
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
                    onCheckedChange = onKeepScreenOnChange
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

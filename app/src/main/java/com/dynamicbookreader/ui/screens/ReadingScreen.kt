package com.dynamicbookreader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.utils.ChapterContentParser
import com.dynamicbookreader.viewmodel.BookViewModel
import com.dynamicbookreader.viewmodel.ChapterUiState
import kotlinx.coroutines.launch

/**
 * Distraction-free reading screen.
 *
 * Features:
 * – Loads the requested chapter via [BookViewModel.openChapter] and reacts
 *   to Loading / Success / Error states (cache makes this near-instant in
 *   the common case, but the states still cover cold-start / failure).
 * – Tap anywhere to show/hide controls (immersive reading).
 * – Font size A+/A− with live preview.
 * – Line height +/− adjustment.
 * – Day / Night / Sepia theme toggle.
 * – Smooth vertical scroll with a live progress bar.
 * – Reading position is saved (debounced) as the user scrolls, and a
 *   "resume" floating button jumps back to the last saved position.
 * – All settings + progress persist across sessions via DataStore.
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
    val readingProgress by viewModel.readingProgress.collectAsState()

    // Trigger the chapter load once when this screen is shown for this chapterNo.
    LaunchedEffect(chapterNo) {
        viewModel.openChapter(chapterNo)
    }

    // Clean up debounce job / reset state when leaving the screen.
    DisposableEffect(Unit) {
        onDispose { viewModel.clearChapterState() }
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
                targetHeading = targetHeading,
                // Only offer "resume" if the saved progress belongs to THIS chapter
                // (otherwise it's stale progress from a different chapter).
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
                onFontIncrease = { viewModel.increaseFontSize() },
                onFontDecrease = { viewModel.decreaseFontSize() },
                onLineHeightIncrease = { viewModel.increaseLineHeight() },
                onLineHeightDecrease = { viewModel.decreaseLineHeight() },
                onThemeChange = { viewModel.setReadingTheme(it) },
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

@Composable
private fun ReadingContent(
    chapter: Chapter,
    fontSize: Float,
    lineHeight: Float,
    readingTheme: ReadingTheme,
    targetHeading: String?,
    savedScrollFraction: Float?,
    onScrollProgressChanged: (Float) -> Unit,
    onLeaveScreen: (Float) -> Unit,
    onMarkHeadingRead: (String) -> Unit,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit,
    onLineHeightIncrease: () -> Unit,
    onLineHeightDecrease: () -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onBack: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsPanelVisible by remember { mutableStateOf(false) }
    var tocExpanded by remember { mutableStateOf(true) }
    var subtitleExpanded by remember { mutableStateOf(false) }
    var selectedFootnoteKey by remember { mutableStateOf<String?>(null) }

    val subtitleRotation by animateFloatAsState(
        targetValue = if (subtitleExpanded) 180f else 0f,
        label = "subtitle_chevron_rotation"
    )

    // Quick key -> text lookup for footnotes defined on this chapter.
    val footnoteLookup = remember(chapter.chapterNo) {
        chapter.footnotes.associateBy { it.key }
    }

    // Parse once per chapter: splits content into paragraphs and — when
    // detected — a confirmed table-of-contents block (see
    // ChapterContentParser for the detection rule). If no ToC is found,
    // tocEntries is empty and every line renders as a plain paragraph,
    // identical to before.
    val parsedChapter = remember(chapter.chapterNo) {
        ChapterContentParser.parse(chapter.content, chapter.title)
    }
    val hasToc = parsedChapter.tocEntries.isNotEmpty()

    // +3 synthetic items: header block, end marker, plus +1 for the ToC
    // card when present.
    val totalItemCount = parsedChapter.blocks.size + 2 + (if (hasToc) 1 else 0)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    // Maps a heading's first-occurrence key to its LazyColumn item index,
    // so tapping a ToC row can scroll straight to it. Built once per
    // chapter parse — cheap, since it's just an index scan.
    val headingIndexMap = remember(parsedChapter) {
        val map = HashMap<String, Int>()
        // Item 0 is the header block; ToC card (if present) is item 1;
        // body blocks start right after.
        val bodyStartIndex = 1 + (if (hasToc) 1 else 0)
        parsedChapter.blocks.forEachIndexed { i, block ->
            if (block.headingKey != null && block.headingKey !in map) {
                map[block.headingKey] = bodyStartIndex + i
            }
        }
        map
    }
    // First-occurrence lookup: ToC entry text -> its headingKey (occurrence 0).
    val tocEntryToHeadingKey = remember(parsedChapter) {
        parsedChapter.tocEntries.associateWith { ChapterContentParser.headingKey(it, 0) }
    }

    // Auto-scroll to a specific heading when opened from Home's "sub-sections"
    // list or the in-Reading ToC's "open in a new instance" path. Runs once
    // per (chapter, targetHeading) pair — if the heading text doesn't match
    // anything in this chapter's confirmed ToC (e.g. stale/edited JSON), it
    // simply does nothing and the screen opens at the top as normal.
    LaunchedEffect(chapter.chapterNo, targetHeading) {
        if (targetHeading.isNullOrBlank()) return@LaunchedEffect
        val headingKey = tocEntryToHeadingKey[targetHeading] ?: return@LaunchedEffect
        val targetIndex = headingIndexMap[headingKey] ?: return@LaunchedEffect
        // Wait a frame so the LazyColumn has laid out its items before we
        // try to scroll — scrolling immediately on first composition can
        // be a no-op if layoutInfo isn't ready yet.
        kotlinx.coroutines.delay(50)
        listState.scrollToItem(targetIndex)
    }

    // Approximate scroll fraction from item position — cheap and stable,
    // avoids needing exact pixel heights of variable-length paragraphs.
    val currentFraction by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total <= 1) return@derivedStateOf 0f

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisible != null && lastVisible.index == total - 1) {
                // Last item is at least partially visible — check if it's
                // fully on-screen to report 100%, otherwise interpolate.
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

    // ── Per-heading read tracking ─────────────────────────────────────────
    // A heading counts as "read" once the user has scrolled past its
    // section (i.e. the next heading — or the end of the chapter — has
    // become the current position). We derive the set of "passed" heading
    // indices from firstVisibleItemIndex, then report any newly-passed
    // ones exactly once via onMarkHeadingRead.
    val sortedHeadingEntries = remember(headingIndexMap) {
        headingIndexMap.entries.sortedBy { it.value }
    }
    val alreadyReported = remember(chapter.chapterNo) { mutableSetOf<String>() }

    LaunchedEffect(listState, sortedHeadingEntries) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { currentIndex ->
                // A heading at itemIndex X is "passed" once the reader has
                // scrolled to or beyond the NEXT heading's item index (or,
                // for the last heading, simply been visited at all — since
                // there's no "next" boundary to confirm they moved past it,
                // reaching it is treated as sufficient for the last section).
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

    // Save immediately when this composable leaves composition (back press, etc).
    DisposableEffect(Unit) {
        onDispose { onLeaveScreen(currentFraction) }
    }

    // Whether to show the "resume" floating action button — only meaningful
    // before the user has scrolled in this session, and only if there's a
    // saved position worth jumping to.
    val showResumeButton = savedScrollFraction != null &&
            savedScrollFraction > 0.02f &&
            listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0

    // Bumping this key forces the SelectionContainer below to fully recompose,
    // which is the standard way to clear its active text selection (there is
    // no public API to imperatively clear a SelectionContainer's selection).
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
        // ── Lazily-rendered reading content (wrapped once for cross-paragraph selection) ──
        // key(selectionResetKey): recreates SelectionContainer's subtree
        // whenever the outer Box registers a tap, which clears any active
        // text selection (SelectionContainer has no public imperative
        // "clear selection" API, so recomposition-via-key is the
        // recommended workaround).
        key(selectionResetKey) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (controlsVisible) 72.dp else 24.dp,
                    bottom = if (controlsVisible) 100.dp else 40.dp,
                    start = 20.dp,
                    end = 20.dp
                )
            ) {
                // Chapter number + title + optional subtitle/page range
                item(key = "header") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "অধ্যায় ${chapter.chapterNo}",
                                style = MaterialTheme.typography.labelLarge,
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
                                        style = MaterialTheme.typography.labelMedium,
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
                                        style = MaterialTheme.typography.labelLarge,
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

                // Collapsible table of contents (only when confirmed headings were found)
                if (hasToc) {
                    item(key = "toc_card") {
                        DisableSelection {
                            TocCard(
                                entries = parsedChapter.tocEntries,
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

                // Main content: one lazy item per paragraph. Selection spans
                // across items since the whole LazyColumn is wrapped in a
                // single SelectionContainer above. Footnote markers render
                // as clickable superscript numbers.
                itemsIndexed(
                    items = parsedChapter.blocks,
                    key = { index, block -> block.headingKey ?: "para_$index" }
                ) { _, block ->
                    val isHeading = block.headingKey != null
                    val baseStyle = if (isHeading) {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = (fontSize + 1).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * lineHeight).sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineHeight).sp,
                            color = textColor,
                            textAlign = TextAlign.Justify
                        )
                    }

                    val hasFootnotes = block.segments.any { it is ChapterContentParser.TextSegment.FootnoteRef }

                    if (!hasFootnotes) {
                        // Fast path: no footnotes in this paragraph, render as plain Text.
                        Text(
                            text = block.plainText,
                            style = baseStyle,
                            modifier = Modifier.padding(
                                top = if (isHeading) 8.dp else 0.dp,
                                bottom = 16.dp
                            )
                        )
                    } else {
                        val footnoteColor = MaterialTheme.colorScheme.primary
                        val annotated = remember(block, footnoteColor) {
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
                            onFootnoteClick = { key -> selectedFootnoteKey = key },
                            modifier = Modifier.padding(
                                top = if (isHeading) 8.dp else 0.dp,
                                bottom = 16.dp
                            )
                        )
                    }
                }

                // End-of-chapter marker
                item(key = "end_marker") {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "﴿ সমাপ্ত ﴾",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        } // close key(selectionResetKey)

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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        text = chapter.title.take(40) + if (chapter.title.length > 40) "…" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { settingsPanelVisible = !settingsPanelVisible }) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "পাঠ সেটিংস",
                            tint = if (settingsPanelVisible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ── Settings Panel (bottom sheet style) ─────────────────────────────
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
                onFontIncrease = onFontIncrease,
                onFontDecrease = onFontDecrease,
                onLineHeightIncrease = onLineHeightIncrease,
                onLineHeightDecrease = onLineHeightDecrease,
                onThemeChange = onThemeChange,
                onDismiss = { settingsPanelVisible = false }
            )
        }

        // ── Resume-position shortcut ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showResumeButton,
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
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // ── Percentage read badge (bottom-left, subtle) ───────────────────────
        AnimatedVisibility(
            visible = controlsVisible && totalItemCount > 1,
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
    }

    // ── Footnote popup ──────────────────────────────────────────────────────
    val activeFootnote = selectedFootnoteKey?.let { footnoteLookup[it] }
    if (selectedFootnoteKey != null) {
        FootnoteDialog(
            footnoteText = activeFootnote?.text,
            onDismiss = { selectedFootnoteKey = null }
        )
    }
}

// ── Footnote popup dialog ───────────────────────────────────────────────────

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

/**
 * Renders an [AnnotatedString] that may contain "footnote" string
 * annotations (pushed via `pushStringAnnotation(tag = "footnote", ...)`)
 * and invokes [onFootnoteClick] with the annotation's key when the user
 * taps directly on one of those spans. Taps elsewhere in the text are
 * ignored (so normal reading/selection isn't affected).
 */
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

// ── Table of Contents card ──────────────────────────────────────────────────

@Composable
private fun TocCard(
    entries: List<String>,
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
            // Header row — always visible, toggles expand/collapse
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
                    style = MaterialTheme.typography.titleMedium,
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium,
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

// ── Settings Panel ────────────────────────────────────────────────────────────

@Composable
private fun ReadingSettingsPanel(
    fontSize: Float,
    lineHeight: Float,
    currentTheme: ReadingTheme,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit,
    onLineHeightIncrease: () -> Unit,
    onLineHeightDecrease: () -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "পাঠ সেটিংস",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            // Font size row
            SettingsRow(label = "ফন্ট সাইজ: ${fontSize.toInt()}sp") {
                ControlButton(label = "A−", onClick = onFontDecrease)
                Spacer(Modifier.width(10.dp))
                ControlButton(label = "A+", onClick = onFontIncrease, large = true)
            }

            Spacer(Modifier.height(12.dp))

            // Line height row
            SettingsRow(label = "লাইন উচ্চতা: ${"%.1f".format(lineHeight)}×") {
                ControlButton(label = "−", onClick = onLineHeightDecrease)
                Spacer(Modifier.width(10.dp))
                ControlButton(label = "+", onClick = onLineHeightIncrease)
            }

            Spacer(Modifier.height(16.dp))

            // Theme row
            Text(
                text = "থিম",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReadingTheme.entries.forEach { theme ->
                    ThemeChip(
                        theme = theme,
                        selected = theme == currentTheme,
                        onClick = { onThemeChange(theme) }
                    )
                }
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

@Composable
private fun ThemeChip(
    theme: ReadingTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (selected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = theme.emoji, fontSize = 16.sp)
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

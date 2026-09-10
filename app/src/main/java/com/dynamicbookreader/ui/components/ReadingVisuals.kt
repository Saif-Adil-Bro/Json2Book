package com.dynamicbookreader.ui.components

import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import com.dynamicbookreader.ui.screens.FootnoteAwareText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.ui.theme.ArabicFontFamily
import com.dynamicbookreader.ui.theme.BanglaFontFamily
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.ui.theme.TextAlignOption
import com.dynamicbookreader.utils.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Procedural Antique Paper / Parchment Texture Background.
 * Renders subtle paper grain, fiber specks, antique margin guides,
 * and vintage corner accents while maintaining pitch black for AMOLED.
 */
@Composable
fun PaperTextureBackground(
    readingTheme: ReadingTheme,
    paperTextureEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseBackgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(baseBackgroundColor)) {
        // Only draw paper texture if AMOLED is false and (texture is enabled or Sepia)
        if (readingTheme != ReadingTheme.AMOLED && (paperTextureEnabled || readingTheme == ReadingTheme.SEPIA)) {
            val grainColor = when (readingTheme) {
                ReadingTheme.SEPIA -> Color(0xFF6B4226)
                ReadingTheme.DAY -> Color(0xFF2C2416)
                ReadingTheme.EMERALD -> Color(0xFF1B3322)
                ReadingTheme.ROSE -> Color(0xFF5C333D)
                ReadingTheme.NIGHT -> Color(0xFF88A0B8)
                ReadingTheme.AMOLED -> Color.Transparent
            }
            val borderColor = grainColor.copy(alpha = 0.08f)
            val cornerColor = grainColor.copy(alpha = 0.15f)

            // Seeded random points for consistent, zero-recalculation grain pattern
            val grainPoints = remember {
                val rnd = Random(42)
                List(180) {
                    Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 1.5f + 0.5f)
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 1. Subtle antique page border frame
                val inset = 12.dp.toPx()
                drawRect(
                    color = borderColor,
                    topLeft = Offset(inset, inset),
                    size = Size(canvasWidth - (inset * 2), canvasHeight - (inset * 2)),
                    style = Stroke(width = 1.dp.toPx())
                )

                // 2. Vintage corner corner notches
                val cornerSize = 14.dp.toPx()
                // Top-Left
                drawLine(cornerColor, Offset(inset, inset + cornerSize), Offset(inset, inset), 2.dp.toPx())
                drawLine(cornerColor, Offset(inset, inset), Offset(inset + cornerSize, inset), 2.dp.toPx())
                // Top-Right
                drawLine(cornerColor, Offset(canvasWidth - inset - cornerSize, inset), Offset(canvasWidth - inset, inset), 2.dp.toPx())
                drawLine(cornerColor, Offset(canvasWidth - inset, inset), Offset(canvasWidth - inset, inset + cornerSize), 2.dp.toPx())
                // Bottom-Left
                drawLine(cornerColor, Offset(inset, canvasHeight - inset - cornerSize), Offset(inset, canvasHeight - inset), 2.dp.toPx())
                drawLine(cornerColor, Offset(inset, canvasHeight - inset), Offset(inset + cornerSize, canvasHeight - inset), 2.dp.toPx())
                // Bottom-Right
                drawLine(cornerColor, Offset(canvasWidth - inset - cornerSize, canvasHeight - inset), Offset(canvasWidth - inset, canvasHeight - inset), 2.dp.toPx())
                drawLine(cornerColor, Offset(canvasWidth - inset, canvasHeight - inset - cornerSize), Offset(canvasWidth - inset, canvasHeight - inset), 2.dp.toPx())

                // 3. Subtle paper fibers & grain specks
                grainPoints.forEach { (relX, relY, radius) ->
                    val alpha = if (readingTheme == ReadingTheme.NIGHT) 0.025f else 0.04f
                    drawCircle(
                        color = grainColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(relX * canvasWidth, relY * canvasHeight)
                    )
                }
            }
        }

        // Child content
        content()
    }
}

/**
 * Floating Reading Progress Bubble (Scroll Indicator).
 * Automatically animates in while scrolling/paging, displaying exact reading percentage
 * with a circular progress indicator, and smoothly fades out after reading settles.
 */
@Composable
fun ReadingProgressBubble(
    progressFraction: Float,
    isScrolling: Boolean,
    modifier: Modifier = Modifier
) {
    var isVisibleByTimer by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Keep visible while scrolling, plus 1.4s grace period after scrolling stops
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            isVisibleByTimer = true
        } else {
            kotlinx.coroutines.delay(1400)
            isVisibleByTimer = false
        }
    }

    // Light subtle haptic feedback at major progress milestones (25%, 50%, 75%, 100%)
    val percentInt = (progressFraction * 100).toInt().coerceIn(0, 100)
    var lastHapticMilestone by remember { mutableIntStateOf(-1) }
    LaunchedEffect(percentInt) {
        val currentMilestone = percentInt / 25
        if (currentMilestone > 0 && currentMilestone != lastHapticMilestone) {
            lastHapticMilestone = currentMilestone
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    AnimatedVisibility(
        visible = (isScrolling || isVisibleByTimer) && progressFraction > 0.01f,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.75f) + slideInHorizontally(tween(220)) { it / 2 },
        exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 0.8f) + slideOutHorizontally(tween(350)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mini Circular Progress Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "📖",
                        fontSize = 10.sp
                    )
                }

                // Percent Text
                Text(
                    text = "${percentInt}% পঠিত",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Realistic Paginated / Page-Turn Reading Experience.
 * Supports realistic Horizontal Pager transitions with smooth 3D curl/angle effect,
 * edge tap to flip, and page indicators (Page X of Y).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagedReadingContent(
    chapter: Chapter,
    parsedChapter: ChapterContentParser.ParsedChapter,
    fontSize: Float,
    lineHeight: Float,
    fontFamily: ReadingFontFamily = ReadingFontFamily.SOLAIMAN_LIPI,
    banglaFont: BanglaFontFamily = BanglaFontFamily.SOLAIMAN_LIPI,
    arabicFont: ArabicFontFamily = ArabicFontFamily.AMIRI,
    textAlign: TextAlignOption,
    textColor: Color,
    ttsState: com.dynamicbookreader.utils.TtsPlaybackState,
    initialFraction: Float,
    onPageProgressChanged: (fraction: Float, isScrolling: Boolean) -> Unit,
    onParagraphLongPress: (Int, String) -> Unit,
    onFootnoteClick: (String) -> Unit,
    onQuoteCardClick: (String, String) -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Chunk blocks into pages (2 blocks per page for clean, comfortable reading without overflow)
    val pages = remember(parsedChapter, fontSize) {
        val list = mutableListOf<List<ChapterContentParser.ContentBlock>>()
        val blocks = parsedChapter.blocks
        var i = 0
        while (i < blocks.size) {
            val chunkSize = if (blocks[i].plainText.length > 500) 1 else 2
            val chunk = blocks.subList(i, (i + chunkSize).coerceAtMost(blocks.size))
            list.add(chunk)
            i += chunk.size
        }
        if (list.isEmpty()) {
            list.add(emptyList())
        }
        list
    }

    val totalPages = pages.size
    val initialPage = (initialFraction * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    var selectionResetKey by remember { mutableIntStateOf(0) }

    // Report reading fraction and scrolling state continuously
    LaunchedEffect(pagerState, totalPages) {
        snapshotFlow {
            val frac = if (totalPages > 1) {
                ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / (totalPages - 1f)).coerceIn(0f, 1f)
            } else 1f
            frac to pagerState.isScrollInProgress
        }.collect { (frac, isScrolling) ->
            onPageProgressChanged(frac, isScrolling)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            selectionResetKey++
                            val width = size.width
                            when {
                                // Tap left 20% -> previous page
                                offset.x < width * 0.22f -> {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }
                                // Tap right 20% -> next page
                                offset.x > width * 0.78f -> {
                                    if (pagerState.currentPage < totalPages - 1) {
                                        coroutineScope.launch {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                                // Center 60% -> toggle controls
                                else -> onToggleControls()
                            }
                        }
                    )
                }
        ) { pageIndex ->
            // Realistic Page Flip / Curl 3D Graphics Transform
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
            val absOffset = pageOffset.absoluteValue

            val pageBlocks = pages.getOrElse(pageIndex) { emptyList() }
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 3D perspective curl effect
                        cameraDistance = 12 * density
                        rotationY = -28f * pageOffset.coerceIn(-1f, 1f)
                        alpha = 1f - (absOffset * 0.35f).coerceIn(0f, 0.7f)
                        scaleX = 1f - (absOffset * 0.04f).coerceIn(0f, 0.1f)
                        scaleY = 1f - (absOffset * 0.04f).coerceIn(0f, 0.1f)
                        // Book shadow on page transition
                        shadowElevation = (1f - absOffset.coerceIn(0f, 1f)) * 4.dp.toPx()
                    }
                    .padding(
                        top = if (controlsVisible) 76.dp else 36.dp,
                        bottom = if (controlsVisible) 100.dp else 46.dp,
                        start = 22.dp,
                        end = 22.dp
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                        // On first page, show chapter header
                        if (pageIndex == 0) {
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
                            Spacer(Modifier.height(18.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                thickness = 1.5.dp,
                                modifier = Modifier.padding(bottom = 18.dp)
                            )
                        } else {
                            // Running header on inner pages
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = banglaFont.fontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "পৃষ্ঠা ${pageIndex + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = banglaFont.fontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        // Page paragraphs
                        pageBlocks.forEach { block ->
                            val isHeading = block.headingKey != null
                            val globalIndex = parsedChapter.blocks.indexOf(block)
                            val isTtsSpeaking = (ttsState.isPlaying || ttsState.isPaused) && ttsState.currentParagraphIndex == globalIndex

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
                                    .padding(top = if (isHeading) 8.dp else 0.dp, bottom = 14.dp)
                                    .then(
                                        if (isTtsSpeaking) {
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
                                                onParagraphLongPress(globalIndex, block.plainText)
                                            },
                                            onTap = {
                                                selectionResetKey++
                                                onToggleControls()
                                            }
                                        )
                                    }
                            ) {
                                key(selectionResetKey) {
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
                                                    onFootnoteClick(key)
                                                },
                                                onNonFootnoteClick = {
                                                    selectionResetKey++
                                                    onToggleControls()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Last Page: End marker and Next Chapter navigation
                        if (pageIndex == totalPages - 1) {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "﴿ সমাপ্ত ﴾",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily.fontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (hasPreviousChapter) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
        }

        // Paged Bottom Page Indicator & Flip Bar (visible when controls are active)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী পাতা", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "পৃষ্ঠা ${pagerState.currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < totalPages - 1) {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী পাতা", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

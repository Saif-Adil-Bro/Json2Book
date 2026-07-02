package com.dynamicbookreader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.data.repository.ReadingProgress
import com.dynamicbookreader.utils.ChapterContentParser
import com.dynamicbookreader.utils.ReadingTimeEstimator
import com.dynamicbookreader.viewmodel.AuthorUiState
import com.dynamicbookreader.viewmodel.BookUiState
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Home / Chapter List screen.
 *
 * - Shows book title + author (from JSON) at the top in a hero banner.
 * - "Continue reading" shortcut card if the user has prior progress.
 * - Lazily-loaded, performant list of chapter cards.
 * - Error state with retry button (bypasses cache on retry).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookViewModel,
    onChapterClick: (Chapter) -> Unit,
    onContinueReadingClick: (chapterNo: Int) -> Unit,
    onAuthorReadMoreClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authorState by viewModel.authorUiState.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val perChapterProgress by viewModel.perChapterProgress.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is BookUiState.Loading -> FullScreenLoading()

                is BookUiState.Error -> FullScreenError(
                    message = state.message,
                    onRetry = { viewModel.reloadBookFromSource() }
                )

                is BookUiState.Success -> {
                    val bookData = state.bookData
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // ── Hero Banner ──────────────────────────────────────
                        item {
                            HeroBanner(
                                bookTitle = bookData.bookTitle,
                                authorState = authorState,
                                onReadMoreClick = onAuthorReadMoreClick
                            )
                        }

                        // ── Continue Reading shortcut ─────────────────────────
                        if (readingProgress.hasProgress && readingProgress.scrollFraction < 0.96f) {
                            item {
                                ContinueReadingCard(
                                    progress = readingProgress,
                                    totalChapters = bookData.chapters.size,
                                    onClick = {
                                        readingProgress.chapterNo?.let(onContinueReadingClick)
                                    }
                                )
                            }
                        }

                        // ── Section Header ───────────────────────────────────
                        item {
                            SectionHeader(chapterCount = bookData.chapters.size)
                        }

                        // ── Chapter Cards ─────────────────────────────────────
                        itemsIndexed(
                            items = bookData.chapters,
                            key = { _, chapter -> chapter.chapterNo }
                        ) { _, chapter ->
                            ChapterCard(
                                chapter = chapter,
                                progressFraction = perChapterProgress[chapter.chapterNo] ?: 0f,
                                isLastRead = chapter.chapterNo == readingProgress.chapterNo,
                                onClick = { onChapterClick(chapter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Continue Reading Card ───────────────────────────────────────────────────

@Composable
private fun ContinueReadingCard(
    progress: ReadingProgress,
    totalChapters: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "পড়া চালিয়ে যান",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "অধ্যায় ${progress.chapterNo}" +
                            (progress.chapterTitle?.let { ": ${it.take(28)}${if (it.length > 28) "…" else ""}" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.scrollFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "চালিয়ে যান",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ── Hero Banner ───────────────────────────────────────────────────────────────


@Composable
private fun HeroBanner(
    bookTitle: String,
    authorState: AuthorUiState,
    onReadMoreClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // App icon row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ডিজিটাল বুক রিডার",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Dynamic book title from JSON
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 36.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )

            // ── Author section (only rendered once author.json resolves) ────
            if (authorState is AuthorUiState.Success) {
                val author = authorState.author

                Spacer(Modifier.height(14.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f),
                    thickness = 1.dp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "লেখক",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = author.shortBio,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "আরো পড়ুন ›",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clickable(onClick = onReadMoreClick)
                )
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(chapterCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "অধ্যায়সমূহ",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "$chapterCount টি",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Chapter Card ──────────────────────────────────────────────────────────────

private const val COMPLETED_THRESHOLD = 0.96f

@Composable
private fun ChapterCard(
    chapter: Chapter,
    progressFraction: Float,
    isLastRead: Boolean = false,
    onClick: () -> Unit
) {
    // Cache the preview substring once per chapter — avoids recomputing
    // String.take()/trim() on every recomposition while scrolling.
    // Footnote markers are stripped so raw {{note:KEY}} text never leaks
    // into the preview.
    val previewText = remember(chapter.chapterNo) {
        ChapterContentParser.stripFootnoteMarkers(chapter.content).take(80).trim() + "…"
    }

    val isCompleted = progressFraction >= COMPLETED_THRESHOLD
    val isInProgress = progressFraction > 0f && !isCompleted

    // Reading-time estimate, computed once per chapter (word-count based).
    val totalMinutes = remember(chapter.chapterNo) {
        ReadingTimeEstimator.totalMinutes(chapter.content)
    }
    val remainingMinutes = remember(chapter.chapterNo, progressFraction) {
        ReadingTimeEstimator.remainingMinutes(chapter.content, progressFraction)
    }

    // ── Color scheme per state (stays within the app's light Material 3 palette) ──
    val containerColor = when {
        isCompleted -> Color(0xFFDCEEFF)   // soft blue tint — "done"
        isInProgress -> Color(0xFFDFF3E6)  // soft green tint — "in progress"
        isLastRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }
    val accentColor = when {
        isCompleted -> Color(0xFF1565C0)
        isInProgress -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }
    val onContainerColor = when {
        isCompleted -> Color(0xFF0D3D6B)
        isInProgress -> Color(0xFF184620)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Chapter number badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${chapter.chapterNo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Chapter title + subtitle/preview
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = onContainerColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    if (chapter.subtitle.isNotBlank()) {
                        // Subtitle takes priority over the content preview when
                        // present, to keep the card from feeling cluttered.
                        Text(
                            text = chapter.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Content preview (precomputed above)
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.65f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Trailing indicator: checkmark (done) / progress ring (in progress) / chevron (unread)
                when {
                    isCompleted -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "সম্পূর্ণ পঠিত",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    isInProgress -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.size(34.dp),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.18f),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "পড়ুন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Status line: completion note / time-remaining / start estimate ──
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.CheckCircle
                        isInProgress -> Icons.Default.Schedule
                        else -> Icons.Default.PlayCircleOutline
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        isCompleted -> "আলহামদুলিল্লাহ! সম্পূর্ণ পড়া শেষ।"
                        isInProgress -> "পড়া চালিয়ে যান — বাকি মাত্র $remainingMinutes মিনিট"
                        else -> "এখনই শুরু করুন — লাগবে মাত্র $totalMinutes মিনিট"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── State screens ─────────────────────────────────────────────────────────────

@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "বই লোড হচ্ছে…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ত্রুটি হয়েছে",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

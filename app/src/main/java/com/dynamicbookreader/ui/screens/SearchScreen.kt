package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.Chapter
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Search tab — entirely local/in-memory (no network, no server).
 * Filters the already-loaded [BookViewModel.searchResults] by title or
 * content match as the user types (debounce happens implicitly since the
 * filtering is cheap — a plain `contains` scan over a few dozen chapters).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel,
    onChapterClick: (Chapter) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("অনুসন্ধান") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search field ───────────────────────────────────────────────
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("অধ্যায়ের শিরোনাম বা বিষয়বস্তু খুঁজুন…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // ── Results / empty states ────────────────────────────────────
            when {
                query.isBlank() -> SearchPrompt()
                results.isEmpty() -> SearchNoResults(query = query)
                else -> {
                    Text(
                        text = "${results.size} টি ফলাফল পাওয়া গেছে",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(items = results, key = { it.chapterNo }) { chapter ->
                            SearchResultCard(
                                chapter = chapter,
                                query = query,
                                onClick = { onChapterClick(chapter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPrompt() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "অধ্যায়ের নাম বা বিষয়বস্তু লিখে অনুসন্ধান করুন",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchNoResults(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔍", fontSize = 40.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "\"$query\" এর জন্য কোনো ফলাফল পাওয়া যায়নি",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    chapter: Chapter,
    query: String,
    onClick: () -> Unit
) {
    // Build a short snippet around the first match in the content, so the
    // user sees *why* this chapter matched rather than just the title.
    val snippet = remember(chapter.chapterNo, query) {
        buildSnippet(chapter.content, query)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "অধ্যায় ${chapter.chapterNo}: ${chapter.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (snippet.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Extracts a short window of text around the first case-insensitive match
 * of [query] inside [content], so the search result gives useful context.
 * Falls back to the start of the content if no match is found there
 * (e.g. the match was only in the title).
 */
private fun buildSnippet(content: String, query: String): String {
    val idx = content.indexOf(query, ignoreCase = true)
    if (idx < 0) return content.take(90).trim() + "…"

    val windowStart = (idx - 30).coerceAtLeast(0)
    val windowEnd = (idx + query.length + 60).coerceAtMost(content.length)
    val prefix = if (windowStart > 0) "…" else ""
    val suffix = if (windowEnd < content.length) "…" else ""
    return prefix + content.substring(windowStart, windowEnd).trim() + suffix
}

package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.viewmodel.AuthorUiState
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Full author biography page — reached via "আরো পড়ুন" on the Home hero
 * section. Reads from the same cached author.json data via the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorDetailScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val authorState by viewModel.authorUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লেখক পরিচিতি") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = authorState) {
                is AuthorUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is AuthorUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("⚠️", fontSize = 44.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "লেখকের তথ্য লোড করা যায়নি",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.reloadAuthorFromSource() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("আবার চেষ্টা করুন")
                            }
                        }
                    }
                }

                is AuthorUiState.Success -> {
                    val author = state.author
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        // Avatar placeholder (photo_url support ready for future use)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = author.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (author.nameEn.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = author.nameEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (author.birthYear.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            val lifespan = if (author.deathYear.isNotBlank())
                                "${author.birthYear} – ${author.deathYear}"
                            else
                                "জন্ম: ${author.birthYear}"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = lifespan,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(Modifier.height(24.dp))

                        // Full biography — paragraph by paragraph
                        val paragraphs = author.fullBiography.split("\n").filter { it.isNotBlank() }
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph.trim(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Justify
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

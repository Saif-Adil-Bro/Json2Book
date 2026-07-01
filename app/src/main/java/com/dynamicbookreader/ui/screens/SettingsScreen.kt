package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Standalone Settings page reachable from the Menu tab.
 * Controls the same DataStore-backed preferences used by the in-Reading
 * settings panel, so changes here apply immediately the next time a
 * chapter is opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সেটিংস") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "পাঠ পছন্দ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Font size
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ফন্ট সাইজ: ${fontSize.toInt()}sp",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { viewModel.decreaseFontSize() }) { Text("A−") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.increaseFontSize() }) { Text("A+") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Line height
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "লাইন উচ্চতা: ${"%.1f".format(lineHeight)}×",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { viewModel.decreaseLineHeight() }) { Text("−") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.increaseLineHeight() }) { Text("+") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Theme
            SettingsCard {
                Column {
                    Text(
                        text = "থিম",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReadingTheme.entries.forEach { theme ->
                            val selected = theme == readingTheme
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { viewModel.setReadingTheme(theme) }
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
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

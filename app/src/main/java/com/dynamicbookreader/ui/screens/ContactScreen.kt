package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.data.model.ContactItem
import com.dynamicbookreader.viewmodel.BookViewModel
import com.dynamicbookreader.viewmodel.ContactUiState

/**
 * Contact page — 100% data-driven from `contact.json`. Edit that file to
 * change the intro text or add/remove/reorder contact methods; no UI code
 * changes needed. Unrecognized `type` values still render correctly with
 * a generic link icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val contactState by viewModel.contactUiState.collectAsState()

    // Lazy-load on first visit; the ViewModel skips re-fetching if already loaded.
    LaunchedEffect(Unit) {
        viewModel.loadContactInfoIfNeeded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("যোগাযোগ করুন") },
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
            when (val state = contactState) {
                is ContactUiState.Idle, is ContactUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is ContactUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("⚠️", fontSize = 44.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "যোগাযোগের তথ্য লোড করা যায়নি",
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
                            Button(onClick = { viewModel.reloadContactInfoFromSource() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("আবার চেষ্টা করুন")
                            }
                        }
                    }
                }

                is ContactUiState.Success -> {
                    val info = state.contactInfo
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = info.introTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = info.introText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        info.items.forEach { item ->
                            ContactRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(item: ContactItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForContactType(item.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Maps a `type` string from contact.json to an icon. Unrecognized types
 * (e.g. future additions like "whatsapp" or "telegram" before an icon is
 * wired up) fall back to a generic link icon instead of crashing or
 * rendering nothing — the JSON stays the single source of truth without
 * requiring a matching code change for every new type.
 */
private fun iconForContactType(type: String): ImageVector = when (type.lowercase()) {
    "email" -> Icons.Default.Email
    "phone" -> Icons.Default.Phone
    "website" -> Icons.Default.Language
    "facebook" -> Icons.Default.Public
    else -> Icons.Default.Link
}

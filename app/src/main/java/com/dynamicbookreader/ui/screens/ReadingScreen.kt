package com.dynamicbookreader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Distraction-free reading screen.
 *
 * Features:
 * – Tap anywhere to show/hide controls (immersive reading).
 * – Font size A+/A− with live preview.
 * – Line height +/− adjustment.
 * – Day / Night / Sepia theme toggle.
 * – Smooth vertical scroll.
 * – All settings persist across sessions via DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val chapter by viewModel.selectedChapter.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var settingsPanelVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Reading theme background / text colours (pulled from MaterialTheme via parent)
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                if (!settingsPanelVisible) controlsVisible = !controlsVisible
            }
    ) {
        // ── Scrollable reading content ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = if (controlsVisible) 72.dp else 24.dp,
                    bottom = if (controlsVisible) 100.dp else 40.dp,
                    start = 20.dp,
                    end = 20.dp
                )
        ) {
            chapter?.let { ch ->
                // Chapter number
                Text(
                    text = "অধ্যায় ${ch.chapterNo}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Chapter title
                Text(
                    text = ch.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = (fontSize + 4).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (fontSize + 14).sp,
                        color = textColor
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Decorative divider
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    thickness = 1.5.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Main content: split by newlines to respect paragraph breaks
                val paragraphs = ch.content.split("\n").filter { it.isNotBlank() }
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph.trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineHeight).sp,
                            color = textColor,
                            textAlign = TextAlign.Justify
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // End-of-chapter marker
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
            } ?: run {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো অধ্যায় নির্বাচিত হয়নি")
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
                        text = chapter?.title?.take(40)?.plus(if ((chapter?.title?.length ?: 0) > 40) "…" else "") ?: "",
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
                onFontIncrease = { viewModel.increaseFontSize() },
                onFontDecrease = { viewModel.decreaseFontSize() },
                onLineHeightIncrease = { viewModel.increaseLineHeight() },
                onLineHeightDecrease = { viewModel.decreaseLineHeight() },
                onThemeChange = { viewModel.setReadingTheme(it) },
                onDismiss = { settingsPanelVisible = false }
            )
        }

        // ── Scroll progress indicator ───────────────────────────────────────
        if (scrollState.maxValue > 0) {
            LinearProgressIndicator(
                progress = { scrollState.value.toFloat() / scrollState.maxValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
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

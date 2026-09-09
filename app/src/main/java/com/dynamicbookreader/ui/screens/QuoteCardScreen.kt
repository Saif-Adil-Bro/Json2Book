package com.dynamicbookreader.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.ui.theme.ReadingFontFamily
import com.dynamicbookreader.ui.theme.SolaimanLipiFont
import androidx.compose.ui.viewinterop.AndroidView
import com.dynamicbookreader.utils.QuoteImageExporter
import com.dynamicbookreader.viewmodel.BookViewModel
import kotlinx.coroutines.launch

enum class QuoteCardBackground(
    val displayName: String,
    val backgroundBrush: Brush,
    val textColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    INDIGO_ROYAL(
        displayName = "রাজকীয় নীল",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF1A1B4B), Color(0xFF2E0854), Color(0xFF3B1E78))
        ),
        textColor = Color(0xFFF0F3FF),
        secondaryColor = Color(0xFFBAC3FF),
        accentColor = Color(0xFFFFD54F)
    ),
    GOLDEN_PARCHMENT(
        displayName = "সোনালী ক্যালিগ্রাফি",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFFDF7E7), Color(0xFFF5E8C7), Color(0xFFE8D6A4))
        ),
        textColor = Color(0xFF3E2723),
        secondaryColor = Color(0xFF6D4C41),
        accentColor = Color(0xFFB8860B)
    ),
    EMERALD_WISDOM(
        displayName = "পান্না সবুজ",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF062C22), Color(0xFF0F4D3C), Color(0xFF16604D))
        ),
        textColor = Color(0xFFE8F5E9),
        secondaryColor = Color(0xFFA5D6A7),
        accentColor = Color(0xFFFFE082)
    ),
    SUNSET_AMBER(
        displayName = "গোধূলি আভা",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF4A1525), Color(0xFF7A2048), Color(0xFFB83A4B))
        ),
        textColor = Color(0xFFFFF3E0),
        secondaryColor = Color(0xFFFFCC80),
        accentColor = Color(0xFFFFE57F)
    ),
    CELESTIAL_ROSE(
        displayName = "রোজ ভেলভেট",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF2B0A1E), Color(0xFF4A1934), Color(0xFF6E284A))
        ),
        textColor = Color(0xFFFFF0F5),
        secondaryColor = Color(0xFFF4C2D7),
        accentColor = Color(0xFFFFD1DC)
    ),
    AMOLED_MIDNIGHT(
        displayName = "পিচ ব্ল্যাক ওলেড",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF0A0A0A), Color(0xFF141414), Color(0xFF1E1E1E))
        ),
        textColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFB0B0B0),
        accentColor = Color(0xFF64B5F6)
    ),
    OCEAN_BREEZE(
        displayName = "সমুদ্র নীল",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF0A2540), Color(0xFF0E3D66), Color(0xFF1565C0))
        ),
        textColor = Color(0xFFE1F5FE),
        secondaryColor = Color(0xFF81D4FA),
        accentColor = Color(0xFF80DEEA)
    )
}

enum class QuoteAspectRatio(val label: String, val ratio: Float?) {
    SQUARE("১:১ স্কয়ার", 1f),
    PORTRAIT("৪:৫ পোর্ট্রেট", 4f / 5f),
    STORY("৯:১৬ স্টোরি", 9f / 16f),
    AUTO("স্বয়ংক্রিয়", null)
}

enum class QuoteBorderStyle(val label: String) {
    NONE("বর্ডারহীন"),
    CLASSIC_GOLD("সোনালী ফ্রেম"),
    ORNAMENTAL("অলংকৃত ফ্রেম")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCardScreen(
    initialQuote: String = "",
    initialBookTitle: String = "",
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    var quoteText by remember { mutableStateOf(initialQuote.ifBlank { "জ্ঞানের আলো মানুষকে অন্ধকারের গহ্বর থেকে আলোর পথে নিয়ে যায়।" }) }
    var bookTitle by remember { mutableStateOf(initialBookTitle.ifBlank { "Dynamic Book Reader" }) }
    var authorName by remember { mutableStateOf("") }
    var selectedBackground by remember { mutableStateOf(QuoteCardBackground.INDIGO_ROYAL) }
    var cardFontSize by remember { mutableFloatStateOf(18f) }
    var selectedFontFamily by remember { mutableStateOf(SolaimanLipiFont) }
    var selectedAspectRatio by remember { mutableStateOf(QuoteAspectRatio.AUTO) }
    var selectedBorderStyle by remember { mutableStateOf(QuoteBorderStyle.ORNAMENTAL) }
    var showQuoteIcon by remember { mutableStateOf(true) }
    var showWatermark by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val authorData by viewModel.authorData.collectAsState()

    LaunchedEffect(authorData) {
        if (authorName.isBlank()) {
            authorName = authorData?.name ?: "লেখক"
        }
    }

    var captureViewRef by remember { mutableStateOf<View?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কোট কার্ড মেকার (Quote Studio)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            captureViewRef?.let { view ->
                                val bitmap = QuoteImageExporter.createBitmapFromView(view)
                                val uri = QuoteImageExporter.saveBitmapToCache(context, bitmap)
                                if (uri != null) {
                                    QuoteImageExporter.shareQuoteImage(context, uri, quoteText)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("ছবি তৈরি করতে সমস্যা হয়েছে")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "কার্ড শেয়ার করুন")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Preview Card with AndroidView capture wrapper
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            setContent {
                                QuoteCardViewContent(
                                    quoteText = quoteText,
                                    bookTitle = bookTitle,
                                    authorName = authorName,
                                    bgTheme = selectedBackground,
                                    fontSize = cardFontSize,
                                    fontFamily = selectedFontFamily,
                                    aspectRatio = selectedAspectRatio,
                                    borderStyle = selectedBorderStyle,
                                    showQuoteIcon = showQuoteIcon,
                                    showWatermark = showWatermark
                                )
                            }
                        }.also { captureViewRef = it }
                    },
                    update = { view ->
                        view.setContent {
                            QuoteCardViewContent(
                                quoteText = quoteText,
                                bookTitle = bookTitle,
                                authorName = authorName,
                                bgTheme = selectedBackground,
                                fontSize = cardFontSize,
                                fontFamily = selectedFontFamily,
                                aspectRatio = selectedAspectRatio,
                                borderStyle = selectedBorderStyle,
                                showQuoteIcon = showQuoteIcon,
                                showWatermark = showWatermark
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            // Share & Copy Buttons (With "ইমেজ শেয়ার" strictly honored)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        captureViewRef?.let { view ->
                            val bitmap = QuoteImageExporter.createBitmapFromView(view)
                            val uri = QuoteImageExporter.saveBitmapToCache(context, bitmap)
                            if (uri != null) {
                                QuoteImageExporter.shareQuoteImage(context, uri, quoteText)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("ছবি শেয়ার করা যায়নি")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ইমেজ শেয়ার")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Quote", "\"$quoteText\"\n— $authorName ($bookTitle)")
                        clipboard.setPrimaryClip(clip)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("উক্তি টেক্সট কপি করা হয়েছে")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("টেক্সট কপি")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Editing & Controls Section
            Text(
                text = "উক্তি সম্পাদনা করুন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quoteText,
                onValueChange = { quoteText = it },
                label = { Text("উক্তির টেক্সট") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Aspect Ratio Selector
            Text(
                text = "কার্ড সাইজ / অনুপাত (Aspect Ratio)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuoteAspectRatio.entries.forEach { ratio ->
                    val isSelected = ratio == selectedAspectRatio
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAspectRatio = ratio },
                        label = { Text(ratio.label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Border Style Selector
            Text(
                text = "ফ্রেম ও বর্ডার শৈলী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuoteBorderStyle.entries.forEach { border ->
                    val isSelected = border == selectedBorderStyle
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBorderStyle = border },
                        label = { Text(border.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Background Theme Selector
            Text(
                text = "কার্ড ব্যাকগ্রাউন্ড থিম",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuoteCardBackground.entries.forEach { bg ->
                    val isSelected = bg == selectedBackground
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg.backgroundBrush)
                            .clickable { selectedBackground = bg }
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = bg.textColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Font Size & Typography controls
            Text(
                text = "ফন্ট শৈলী ও সাইজ (${cardFontSize.toInt()}sp)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReadingFontFamily.entries.size) { index ->
                    val family = ReadingFontFamily.entries[index]
                    FilterChip(
                        selected = selectedFontFamily == family.fontFamily,
                        onClick = { selectedFontFamily = family.fontFamily },
                        label = {
                            Text(
                                text = family.displayName,
                                fontFamily = family.fontFamily
                            )
                        }
                    )
                }
            }

            Slider(
                value = cardFontSize,
                onValueChange = { cardFontSize = it },
                valueRange = 14f..28f,
                steps = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Additional Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("উদ্ধৃতি আইকন (“) দেখান", modifier = Modifier.weight(1f))
                Switch(checked = showQuoteIcon, onCheckedChange = { showQuoteIcon = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ওয়াটারমার্ক ও অ্যাপ নাম", modifier = Modifier.weight(1f))
                Switch(checked = showWatermark, onCheckedChange = { showWatermark = it })
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuoteCardViewContent(
    quoteText: String,
    bookTitle: String,
    authorName: String,
    bgTheme: QuoteCardBackground,
    fontSize: Float,
    fontFamily: FontFamily,
    aspectRatio: QuoteAspectRatio,
    borderStyle: QuoteBorderStyle,
    showQuoteIcon: Boolean,
    showWatermark: Boolean
) {
    val boxModifier = Modifier
        .fillMaxWidth()
        .then(
            if (aspectRatio.ratio != null) Modifier.aspectRatio(aspectRatio.ratio)
            else Modifier.defaultMinSize(minHeight = 360.dp)
        )
        .background(bgTheme.backgroundBrush)
        .padding(16.dp)

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        // Optional Ornate or Classic Border Inner Box
        val innerModifier = when (borderStyle) {
            QuoteBorderStyle.NONE -> Modifier.fillMaxSize().padding(12.dp)
            QuoteBorderStyle.CLASSIC_GOLD -> Modifier
                .fillMaxSize()
                .border(1.5.dp, bgTheme.accentColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(20.dp)
            QuoteBorderStyle.ORNAMENTAL -> Modifier
                .fillMaxSize()
                .border(2.dp, bgTheme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(4.dp)
                .border(1.dp, bgTheme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        }

        Box(
            modifier = innerModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (borderStyle == QuoteBorderStyle.ORNAMENTAL) {
                    Text(
                        text = "❖ ─── ❖ ─── ❖",
                        fontSize = 11.sp,
                        color = bgTheme.accentColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (showQuoteIcon) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(bgTheme.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "“",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = bgTheme.accentColor
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Text(
                    text = quoteText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        lineHeight = (fontSize * 1.55f).sp,
                        textAlign = TextAlign.Center
                    ),
                    color = bgTheme.textColor
                )

                Spacer(Modifier.height(18.dp))

                // Divider accent
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.dp)
                        .background(bgTheme.accentColor.copy(alpha = 0.6f))
                )

                Spacer(Modifier.height(12.dp))

                // Author & Book metadata badge
                Text(
                    text = if (authorName.isNotBlank()) "— $authorName" else "— বিশিষ্ট চিন্তক",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = bgTheme.secondaryColor
                )

                if (bookTitle.isNotBlank()) {
                    Text(
                        text = "📖 $bookTitle",
                        style = MaterialTheme.typography.labelMedium,
                        color = bgTheme.secondaryColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (showWatermark) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Dynamic Book Reader App",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = bgTheme.secondaryColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

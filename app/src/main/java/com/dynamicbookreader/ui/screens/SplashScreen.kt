package com.dynamicbookreader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicbookreader.viewmodel.BookUiState
import com.dynamicbookreader.viewmodel.BookViewModel
import kotlinx.coroutines.delay

/**
 * Splash screen: fades in the book title from the JSON, then
 * auto-navigates to Home once the data is loaded.
 */
@Composable
fun SplashScreen(
    viewModel: BookViewModel,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Animation values
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // Animate in
        alpha.animateTo(1f, animationSpec = tween(900, easing = EaseOutCubic))
        scale.animateTo(1f, animationSpec = tween(900, easing = EaseOutCubic))
    }

    // Navigate when data is ready (or after minimum display time)
    LaunchedEffect(uiState) {
        if (uiState !is BookUiState.Loading) {
            delay(1_400)       // minimum splash display time
            onNavigateToHome()
        }
    }

    // Background: use primary colour for a branded look
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            // Decorative ornament
            Text(
                text = "☪",
                fontSize = 56.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
            )

            Spacer(Modifier.height(24.dp))

            // Dynamic book title from JSON
            val titleText = when (val state = uiState) {
                is BookUiState.Success -> state.bookData.bookTitle
                else -> "লোড হচ্ছে…"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle / branding line
            Text(
                text = "সীলমোহরকৃত জান্নাত",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(60.dp))

            // Subtle loading dots
            LoadingDots(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.60f))
        }

        // Version label at bottom
        Text(
            text = "ভার্সন ১.০",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

// ── Animated loading dots ─────────────────────────────────────────────────────

@Composable
private fun LoadingDots(color: androidx.compose.ui.graphics.Color) {
    val dotCount = 3
    val alphas = (0 until dotCount).map { i ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(i * 200L)
            while (true) {
                anim.animateTo(1f, tween(400))
                anim.animateTo(0.2f, tween(400))
            }
        }
        anim.value
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        alphas.forEach { a ->
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(a)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Menu tab — entry point for Settings, Contact, Privacy Policy, About,
 * and any other app-level (non-book) options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onSettingsClick: () -> Unit,
    onContactClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("মেনু") })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 8.dp)
        ) {
            MenuSectionLabel("সাধারণ")
            MenuItemRow(
                icon = Icons.Default.Settings,
                title = "সেটিংস",
                subtitle = "পাঠ পছন্দ ও থিম নিয়ন্ত্রণ করুন",
                onClick = onSettingsClick
            )

            Spacer(Modifier.height(16.dp))
            MenuSectionLabel("সহায়তা ও তথ্য")
            MenuItemRow(
                icon = Icons.Default.ContactMail,
                title = "যোগাযোগ করুন",
                subtitle = "মতামত বা প্রশ্ন পাঠান",
                onClick = onContactClick
            )
            MenuItemRow(
                icon = Icons.Default.PrivacyTip,
                title = "প্রাইভেসি পলিসি",
                subtitle = "আপনার তথ্যের গোপনীয়তা নীতি",
                onClick = onPrivacyPolicyClick
            )
            MenuItemRow(
                icon = Icons.Default.Info,
                title = "সম্পর্কে",
                subtitle = "অ্যাপ ও সংস্করণ তথ্য",
                onClick = onAboutClick
            )
        }
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun MenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

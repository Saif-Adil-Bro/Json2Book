package com.dynamicbookreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Privacy Policy — static content. Since the app is 100% local (all data
 * from bundled JSON assets, no network calls, no accounts), the policy is
 * intentionally short and honest about what is/isn't collected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রাইভেসি পলিসি") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp)
        ) {
            PolicySection(
                title = "তথ্য সংগ্রহ",
                body = "এই অ্যাপটি সম্পূর্ণভাবে স্থানীয়ভাবে (অফলাইনে) কাজ করে। আমরা আপনার কোনো ব্যক্তিগত তথ্য সংগ্রহ, সংরক্ষণ বা কোনো সার্ভারে পাঠাই না।"
            )
            PolicySection(
                title = "স্থানীয় সংরক্ষণ",
                body = "আপনার পড়ার অগ্রগতি, ফন্ট সাইজ, থিম এবং অন্যান্য পছন্দ শুধুমাত্র আপনার ডিভাইসে (DataStore-এর মাধ্যমে) সংরক্ষিত থাকে। এই তথ্য কখনো আপনার ডিভাইসের বাইরে যায় না।"
            )
            PolicySection(
                title = "ইন্টারনেট ব্যবহার",
                body = "এই অ্যাপ চালাতে কোনো ইন্টারনেট সংযোগের প্রয়োজন নেই। সব বইয়ের তথ্য অ্যাপের সাথেই বান্ডল করা থাকে।"
            )
            PolicySection(
                title = "তৃতীয় পক্ষের সেবা",
                body = "এই অ্যাপ কোনো তৃতীয় পক্ষের বিজ্ঞাপন নেটওয়ার্ক, অ্যানালিটিক্স, বা ট্র্যাকিং সেবা ব্যবহার করে না।"
            )
            PolicySection(
                title = "পরিবর্তন",
                body = "এই প্রাইভেসি পলিসি ভবিষ্যতে আপডেট হতে পারে। কোনো পরিবর্তন হলে তা এই পৃষ্ঠায় প্রতিফলিত হবে।"
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

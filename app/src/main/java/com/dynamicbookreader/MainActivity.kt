package com.dynamicbookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.dynamicbookreader.ui.AppNavigation
import com.dynamicbookreader.viewmodel.BookUiState
import com.dynamicbookreader.viewmodel.BookViewModel

class MainActivity : ComponentActivity() {

    // Scoped to Activity lifecycle, survives config changes
    private val viewModel: BookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ splash screen — keeps the launch screen
        // visible until the book data is ready
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value is BookUiState.Loading
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            AppNavigation(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

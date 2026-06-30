package com.dynamicbookreader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dynamicbookreader.ui.screens.HomeScreen
import com.dynamicbookreader.ui.screens.ReadingScreen
import com.dynamicbookreader.ui.screens.SplashScreen
import com.dynamicbookreader.ui.theme.DynamicBookReaderTheme
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Central navigation graph.
 *
 * All screens share the same [BookViewModel]; the ViewModel
 * acts as the single source of truth and survives configuration changes.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: BookViewModel
) {
    val readingTheme by viewModel.readingTheme.collectAsState(initial = ReadingTheme.DAY)

    DynamicBookReaderTheme(readingTheme = readingTheme) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { it / 6 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) +
                        slideOutHorizontally(animationSpec = tween(200)) { -it / 6 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { -it / 6 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) +
                        slideOutHorizontally(animationSpec = tween(200)) { it / 6 }
            }
        ) {
            // Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home / Chapter List
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onChapterClick = { chapter ->
                        navController.navigate(
                            Screen.Reading.createRoute(chapter.chapterNo)
                        )
                    },
                    onContinueReadingClick = { chapterNo ->
                        navController.navigate(
                            Screen.Reading.createRoute(chapterNo)
                        )
                    }
                )
            }

            // Reading
            composable(
                route = Screen.Reading.route,
                arguments = listOf(
                    androidx.navigation.navArgument("chapterNo") {
                        type = androidx.navigation.NavType.IntType
                    }
                )
            ) { backStackEntry ->
                val chapterNo = backStackEntry.arguments?.getInt("chapterNo") ?: 1
                ReadingScreen(
                    chapterNo = chapterNo,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

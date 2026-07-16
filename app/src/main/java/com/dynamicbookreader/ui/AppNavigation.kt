package com.dynamicbookreader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dynamicbookreader.ui.components.BottomNavBar
import com.dynamicbookreader.ui.screens.*
import com.dynamicbookreader.ui.theme.DynamicBookReaderTheme
import com.dynamicbookreader.ui.theme.ReadingTheme
import com.dynamicbookreader.viewmodel.BookViewModel

/**
 * Central navigation graph.
 *
 * All screens share the same [BookViewModel]; the ViewModel acts as the
 * single source of truth and survives configuration changes.
 *
 * ── Structure ─────────────────────────────────────────────────────────────
 * A single [NavHost] hosts every destination. The bottom navigation bar is
 * shown only for the 3 tab routes ([bottomNavRoutes]); full-screen
 * destinations like Reading, AuthorDetail, and the Menu sub-pages render
 * without it, so they can use the entire screen (matching how a
 * distraction-free reader or a settings page should behave).
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: BookViewModel
) {
    val readingTheme by viewModel.readingTheme.collectAsState(initial = ReadingTheme.DAY)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavRoutes

    DynamicBookReaderTheme(readingTheme = readingTheme) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = fadeIn(animationSpec = tween(200)) +
                            slideInVertically(animationSpec = tween(200)) { it },
                    exit = fadeOut(animationSpec = tween(150)) +
                            slideOutVertically(animationSpec = tween(150)) { it }
                ) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                // Standard single-top bottom-nav behavior:
                                // avoid stacking duplicate tab destinations,
                                // preserve each tab's own back stack/state.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { scaffoldPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(
                    bottom = if (showBottomNav) scaffoldPadding.calculateBottomPadding() else 0.dp
                ),
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
                // ── Splash ───────────────────────────────────────────────────
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

                // ── Tab: Home ────────────────────────────────────────────────
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onChapterClick = { chapter ->
                            navController.navigate(Screen.Reading.createRoute(chapter.chapterNo))
                        },
                        onChapterSubheadingClick = { chapter, headingText ->
                            navController.navigate(
                                Screen.Reading.createRouteWithTarget(chapter.chapterNo, headingText)
                            )
                        },
                        onContinueReadingClick = { chapterNo ->
                            navController.navigate(Screen.Reading.createRoute(chapterNo))
                        },
                        onAuthorReadMoreClick = {
                            navController.navigate(Screen.AuthorDetail.route)
                        }
                    )
                }

                // ── Tab: Search ──────────────────────────────────────────────
                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = viewModel,
                        onChapterClick = { chapter ->
                            navController.navigate(Screen.Reading.createRoute(chapter.chapterNo))
                        }
                    )
                }

                // ── Tab: Menu ────────────────────────────────────────────────
                composable(Screen.Menu.route) {
                    MenuScreen(
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onContactClick = { navController.navigate(Screen.Contact.route) },
                        onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                        onAboutClick = { navController.navigate(Screen.About.route) }
                    )
                }

                // ── Full-screen: Reading ─────────────────────────────────────
                composable(
                    route = Screen.Reading.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("chapterNo") {
                            type = androidx.navigation.NavType.IntType
                        },
                        androidx.navigation.navArgument("targetHeading") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val chapterNo = entry.arguments?.getInt("chapterNo") ?: 1
                    val targetHeadingEncoded = entry.arguments?.getString("targetHeading")
                    val targetHeading = targetHeadingEncoded?.let {
                        java.net.URLDecoder.decode(it, "UTF-8")
                    }
                    ReadingScreen(
                        chapterNo = chapterNo,
                        targetHeading = targetHeading,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Full-screen: Author detail ───────────────────────────────
                composable(Screen.AuthorDetail.route) {
                    AuthorDetailScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Full-screen: Menu sub-pages ──────────────────────────────
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Contact.route) {
                    ContactScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.PrivacyPolicy.route) {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

package com.dynamicbookreader.ui

/**
 * Sealed class for type-safe navigation routes.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Reading : Screen("reading/{chapterNo}") {
        fun createRoute(chapterNo: Int) = "reading/$chapterNo"
    }
}

package com.dynamicbookreader.ui

/**
 * Sealed class for type-safe navigation routes.
 *
 * ── Structure ─────────────────────────────────────────────────────────────
 * Splash -> MainTabs (bottom nav: Home / Search / Menu)
 *
 * Full-screen destinations that sit ON TOP of the tab scaffold (Reading,
 * AuthorDetail, and the individual Menu sub-pages) are pushed onto the same
 * outer NavHost so they can cover the bottom nav bar entirely — matching
 * how a distraction-free reading screen or a settings page should behave.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")

    // ── Bottom nav tabs ────────────────────────────────────────────────────
    object Home : Screen("home")
    object Search : Screen("search")
    object Menu : Screen("menu")

    // ── Full-screen destinations (no bottom nav) ──────────────────────────
    object Reading : Screen("reading/{chapterNo}") {
        fun createRoute(chapterNo: Int) = "reading/$chapterNo"
    }

    object AuthorDetail : Screen("author_detail")

    // ── Menu sub-pages ─────────────────────────────────────────────────────
    object Settings : Screen("menu/settings")
    object Contact : Screen("menu/contact")
    object PrivacyPolicy : Screen("menu/privacy")
    object About : Screen("menu/about")
}

/** Routes that should show the bottom navigation bar. */
val bottomNavRoutes = setOf(Screen.Home.route, Screen.Search.route, Screen.Menu.route)

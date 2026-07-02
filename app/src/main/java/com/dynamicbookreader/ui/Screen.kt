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
    object Reading : Screen("reading/{chapterNo}?targetHeading={targetHeading}") {
        /** Open a chapter at its top. */
        fun createRoute(chapterNo: Int) = "reading/$chapterNo"

        /**
         * Open a chapter and immediately scroll to a specific heading —
         * used by the Home screen's "sub-sections" list and the in-Reading
         * table of contents. [headingText] must be the *exact* heading line
         * as it appears in the chapter's confirmed ToC (see
         * ChapterContentParser) — it's URL-encoded here since heading text
         * can contain parentheses, colons, and Arabic diacritics.
         */
        fun createRouteWithTarget(chapterNo: Int, headingText: String): String {
            val encoded = java.net.URLEncoder.encode(headingText, "UTF-8")
            return "reading/$chapterNo?targetHeading=$encoded"
        }
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

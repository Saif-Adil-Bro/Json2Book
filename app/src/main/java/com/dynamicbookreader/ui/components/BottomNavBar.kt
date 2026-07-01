package com.dynamicbookreader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dynamicbookreader.ui.Screen

/**
 * Bottom navigation bar with 3 tabs: Home, Search, Menu.
 *
 * @param currentRoute the currently active [Screen] route, used to
 *   highlight the selected tab and avoid re-navigating to the same tab.
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onTabSelected(Screen.Home) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Home.route)
                        Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "হোম"
                )
            },
            label = { Text("হোম") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Search.route,
            onClick = { onTabSelected(Screen.Search) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Search.route)
                        Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "সার্চ"
                )
            },
            label = { Text("সার্চ") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Menu.route,
            onClick = { onTabSelected(Screen.Menu) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Menu.route)
                        Icons.Filled.Menu else Icons.Outlined.Menu,
                    contentDescription = "মেনু"
                )
            },
            label = { Text("মেনু") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

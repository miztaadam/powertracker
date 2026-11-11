package com.powertracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.powertracker.AppContainer
import com.powertracker.ui.coach.CoachScreen
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.progress.LiftHistoryScreen
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Filled.FitnessCenter),
    COACH("coach", "Coach", Icons.Filled.Bolt),
    PROGRESS("progress", "Progress", Icons.Filled.Insights),
    PLAN("plan", "Plan", Icons.Filled.CalendarMonth),
}

object Routes {
    const val SETTINGS = "settings"
    const val LIFT_HISTORY = "liftHistory/{lift}"
    fun liftHistory(lift: String) = "liftHistory/$lift"
}

@Composable
fun AppShell(container: AppContainer) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                PtBottomBar(route) { tab ->
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Tab.TODAY.route) { TodayScreen() }
            composable(Tab.COACH.route) { CoachScreen() }
            composable(Tab.PROGRESS.route) { ProgressScreen() }
            composable(Tab.PLAN.route) {
                PlanScreen(container.training, container.settings, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.LIFT_HISTORY) { entry ->
                LiftHistoryScreen(entry.arguments?.getString("lift").orEmpty())
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container.settings, container.backup, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** The four-tab bottom bar. The current tab gets the volt indicator; everything else stays quiet. */
@Composable
fun PtBottomBar(currentRoute: String?, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Column {
        HorizontalDivider(color = pt.hairline)
        NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
            for (tab in Tab.entries) {
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = pt.onAccent,
                        indicatorColor = pt.accent,
                        selectedTextColor = scheme.onSurface,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

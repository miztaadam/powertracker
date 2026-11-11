package com.powertracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.powertracker.ui.today.TodayScreen

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Filled.FitnessCenter),
    COACH("coach", "Coach", Icons.Filled.Lightbulb),
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
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    for (tab in Tab.entries) {
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding)) {
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

package com.powertracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.powertracker.AppContainer
import com.powertracker.data.AdviceRepository
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.ui.coach.CoachScreen
import com.powertracker.ui.library.ExercisesScreen
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.progress.LiftHistoryScreen
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen
import com.powertracker.ui.today.rememberRestTimerHooks
import com.powertracker.ui.today.rememberTodayViewModel

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Filled.FitnessCenter),
    COACH("coach", "Coach", Icons.Filled.Bolt),
    PROGRESS("progress", "Progress", Icons.Filled.Insights),
    EXERCISES("exercises", "Exercises", Icons.AutoMirrored.Filled.FormatListBulleted),
    PLAN("plan", "Plan", Icons.Filled.CalendarMonth),
}

object Routes {
    const val SETTINGS = "settings"
    const val LIFT_HISTORY = "liftHistory/{lift}"
    fun liftHistory(lift: String) = "liftHistory/$lift"
}

@Composable
fun AppShell(container: AppContainer) = AppShell(container.training, container.advice, container.settings, container.backup)

/** The tabs and the screens behind them, over the app's repositories. */
@Composable
fun AppShell(training: TrainingRepository, advice: AdviceRepository, settings: SettingsRepository, backup: BackupService) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    fun openTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (Tab.entries.any { it.route == route }) PtBottomBar(route, ::openTab)
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Tab.TODAY.route) {
                val rest = rememberRestTimerHooks(settings)
                TodayScreen(
                    training, advice, settings,
                    onOpenCoach = { openTab(Tab.COACH) },
                    onSetLogged = rest.onSetLogged,
                    restTimer = rest.restTimer,
                )
            }
            composable(Tab.COACH.route) { CoachScreen(training, advice, settings) }
            composable(Tab.PROGRESS.route) {
                ProgressScreen(training, settings, onOpenLift = { nav.navigate(Routes.liftHistory(it.name)) })
            }
            composable(Tab.EXERCISES.route) { entry ->
                // The Today tab's view model, from its place at the bottom of the back stack: "Add to today"
                // goes to the session being logged there, and sees when it is finished.
                val todayEntry = remember(entry) { nav.getBackStackEntry(Tab.TODAY.route) }
                ExercisesScreen(
                    training, settings,
                    todayViewModel = rememberTodayViewModel(training, advice, settings, owner = todayEntry),
                    onOpenPlan = { openTab(Tab.PLAN) },
                    onOpenToday = { openTab(Tab.TODAY) },
                )
            }
            composable(Tab.PLAN.route) {
                PlanScreen(training, settings, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.LIFT_HISTORY) { entry ->
                LiftHistoryScreen(training, entry.arguments?.getString("lift").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(settings, backup, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** The five-tab bottom bar. The current tab gets the accent indicator; everything else stays quiet. */
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
                    modifier = Modifier.testTag("tab-${tab.route}"),
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

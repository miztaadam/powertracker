package com.powertracker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.powertracker.data.PlanSnapshot
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.powertracker.ui.guide.GuideExit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import com.powertracker.ui.checkin.CheckInScreen
import com.powertracker.ui.coach.CoachScreen
import com.powertracker.ui.guide.GuideScreen
import com.powertracker.ui.library.ExercisesScreen
import com.powertracker.ui.motion.ModeSwitchHost
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.plan.builder.PlanBuilderScreen
import com.powertracker.ui.plan.templates.TemplatesScreen
import com.powertracker.ui.plan.templates.planSetText
import com.powertracker.ui.plan.PLAN_SAVED
import com.powertracker.ui.progress.ExerciseHistoryScreen
import com.powertracker.ui.progress.LiftHistoryScreen
import com.powertracker.ui.progress.WorkoutHistoryScreen
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen
import com.powertracker.ui.today.rememberRestTimerHooks
import com.powertracker.ui.today.rememberTodayViewModel
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.components.TAB_PRESS_SCALE
import com.powertracker.ui.components.pressScale

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
    const val EXERCISE_HISTORY = "exerciseHistory/{id}"
    fun exerciseHistory(id: Long) = "exerciseHistory/$id"
    /** Every past workout (1.20), opened from Progress's History button. */
    const val WORKOUT_HISTORY = "workoutHistory"
    /** The bodybuilding side's plan builder (stage 2). */
    const val PLAN_BUILDER = "planBuilder"
    /** A side's plan templates (1.14): the mode's name, POWERLIFTING or BODYBUILDING. */
    const val TEMPLATES = "templates/{mode}"
    fun templates(mode: TrainingMode) = "templates/${mode.name}"
    /** The bodybuilding side's check-in (stage 3). */
    const val CHECK_IN = "checkIn"
    /** The guide (1.16): full screen, no tab bar. Opened by the first launch, or from Settings as a replay. */
    const val GUIDE = "guide"
}

/**
 * [guideOnFirstLaunch] turns on the guide's first-launch gate (1.16). Off unless asked for: MainActivity asks; every
 * test that draws the shell keeps it off, as its empty database is exactly a new user who has not seen the guide.
 */
@Composable
fun AppShell(container: AppContainer, guideOnFirstLaunch: Boolean = false) =
    AppShell(container.training, container.advice, container.settings, container.backup, guideOnFirstLaunch)

/** The tabs and the screens behind them, over the app's repositories. */
@Composable
fun AppShell(
    training: TrainingRepository,
    advice: AdviceRepository,
    settings: SettingsRepository,
    backup: BackupService,
    guideOnFirstLaunch: Boolean = false,
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val scope = rememberCoroutineScope()

    // The first-launch gate: read "seen" first, and only if not seen ask whether this is a new user (no session and no
    // plan item on either side); then open the guide, as a first launch. Decided once per start of the shell (kept
    // across a rotation); while it decides, nothing of the app shows, so Today never flashes before the guide.
    var gate by rememberSaveable { mutableStateOf(if (guideOnFirstLaunch) Gate.DECIDING else Gate.SHUT) }
    // The guide on screen is the first launch's (picks the side, writes "seen" on any exit), not a replay.
    var firstLaunchGuide by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (gate != Gate.DECIDING) return@LaunchedEffect
        val open = !settings.guideSeen.first() && training.isNewUser()
        // Navigation is the main thread's, once the host has its graph (its first entry, Today, is there).
        withContext(Dispatchers.Main.immediate) {
            if (open) {
                nav.currentBackStackEntryFlow.first()
                firstLaunchGuide = true
                nav.navigate(Routes.GUIDE) { launchSingleTop = true }
            }
            gate = Gate.SHUT
        }
    }

    fun openTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    // "My goals have changed": on the bodybuilding side it runs the plan builder again; on the powerlifting side it opens Plan.
    val mode by remember(settings) { settings.settings.map { it.trainingMode } }.collectAsState(initial = null)
    fun checkIn() = nav.navigate(Routes.CHECK_IN) { launchSingleTop = true }
    // Plan's snackbar after a template or builder save: set here, shown and cleared by Plan. [planUndo] goes with it,
    // set on every save (null when the side had no plan, so an older save's Undo never rides on a newer message).
    var planMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var planUndo by remember { mutableStateOf<PlanSnapshot?>(null) }
    fun goalsChanged() = if (mode == TrainingMode.BODYBUILDING) nav.navigate(Routes.PLAN_BUILDER) { launchSingleTop = true } else openTab(Tab.PLAN)


    /**
     * The guide has been left by [exit]. A first launch's guide writes "seen" here, whichever way it was left (Skip,
     * Back, Done or a plan button); a replay writes nothing. Each exit leaves the guide first, so Back from what it
     * opens never returns to it: the template list and the builder sit over Plan.
     */
    fun leaveGuide(exit: GuideExit, side: TrainingMode) {
        if (firstLaunchGuide) {
            firstLaunchGuide = false
            scope.launch { settings.markGuideSeen() }
        }
        nav.popBackStack(Routes.GUIDE, inclusive = true)
        // To Plan: back down to it when the guide was opened over it (a replay from Settings), else its tab.
        fun toPlan() {
            if (!nav.popBackStack(Tab.PLAN.route, inclusive = false)) openTab(Tab.PLAN)
        }
        when (exit) {
            GuideExit.SKIPPED, GuideExit.DONE -> Unit
            GuideExit.SEE_PLAN -> toPlan()
            GuideExit.CHOOSE_TEMPLATE -> {
                toPlan()
                nav.navigate(Routes.templates(side)) { launchSingleTop = true }
            }
            GuideExit.BUILD_PLAN -> {
                toPlan()
                nav.navigate(Routes.PLAN_BUILDER) { launchSingleTop = true }
            }
        }
    }

    // The mode switch animations draw over everything here, tab bar included (1.15).
    ModeSwitchHost {
    Box {
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
                    onOpenPlan = ::goalsChanged,
                )
            }
            composable(Tab.COACH.route) { CoachScreen(training, advice, settings, onOpenPlan = ::goalsChanged, onCheckIn = ::checkIn) }
            composable(Tab.PROGRESS.route) {
                ProgressScreen(
                    training, settings,
                    onOpenLift = { nav.navigate(Routes.liftHistory(it.name)) },
                    onOpenExercise = { nav.navigate(Routes.exerciseHistory(it)) },
                    onCheckIn = ::checkIn,
                    // Single top: a double tap opens one History, not two.
                    onOpenHistory = { nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true } },
                )
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
                PlanScreen(
                    training, settings, onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenBuilder = { nav.navigate(Routes.PLAN_BUILDER) { launchSingleTop = true } },
                    onOpenTemplates = { side -> nav.navigate(Routes.templates(side)) { launchSingleTop = true } },
                    message = planMessage,
                    undo = planUndo,
                    onMessageShown = {
                        planMessage = null
                        planUndo = null
                    },
                )
            }
            composable(Routes.TEMPLATES) { entry ->
                val side = entry.arguments?.getString("mode")?.let { name -> TrainingMode.entries.firstOrNull { it.name == name } }
                if (side == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    TemplatesScreen(
                        side, training, settings,
                        onClose = { nav.popBackStack() },
                        // Back to the Plan tab with the new week, and the snackbar there.
                        onSavedUndo = { planUndo = it },
                        onSaved = { name ->
                            planMessage = planSetText(name)
                            nav.popBackStack()
                            openTab(Tab.PLAN)
                        },
                    )
                }
            }
            composable(Routes.PLAN_BUILDER) {
                PlanBuilderScreen(
                    training, settings,
                    onClose = { nav.popBackStack() },
                    // Back to the Plan tab, wherever the builder was opened from, to see the new plan.
                    onSavedUndo = { planUndo = it },
                    onSaved = {
                        planMessage = PLAN_SAVED
                        nav.popBackStack()
                        openTab(Tab.PLAN)
                    },
                )
            }
            composable(Routes.CHECK_IN) {
                CheckInScreen(
                    training, advice, settings,
                    onClose = { nav.popBackStack() },
                    // To Coach, wherever the check-in was opened from, to see the suggestions.
                    onSeeCoach = {
                        nav.popBackStack()
                        openTab(Tab.COACH)
                    },
                )
            }
            composable(Routes.LIFT_HISTORY) { entry ->
                LiftHistoryScreen(training, entry.arguments?.getString("lift").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.EXERCISE_HISTORY) { entry ->
                ExerciseHistoryScreen(training, entry.arguments?.getString("id")?.toLongOrNull() ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(Routes.WORKOUT_HISTORY) {
                WorkoutHistoryScreen(training, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(settings, backup, onBack = { nav.popBackStack() }, onOpenGuide = { nav.navigate(Routes.GUIDE) { launchSingleTop = true } })
            }
            composable(Routes.GUIDE) {
                val side = mode ?: TrainingMode.POWERLIFTING
                val first = firstLaunchGuide
                // Screen 8 asks whether the side now on has a plan: read only while the guide is open.
                val plans by remember(training) { training.plans() }.collectAsState(initial = null)
                GuideScreen(
                    side, firstLaunch = first, hasPlan = plans?.of(side)?.isNotEmpty() == true, motionOn = LocalMotion.current,
                    // The first launch's pick sets the real side, the same setting as the Settings switch, but dates
                    // no side as left (a new lifter has left nothing), with no animation, sound or vibration: the mode
                    // switch's controller is never asked. The side already on is never written.
                    onPickSide = { pick -> if (first && pick != mode) scope.launch { settings.pickFirstSide(pick) } },
                    onClose = { exit -> leaveGuide(exit, side) },
                )
            }
        }
    }
        // While the gate decides, and until the first launch's guide is up, a blank screen in the app's background.
        if (gate == Gate.DECIDING || (firstLaunchGuide && route != Routes.GUIDE)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("guide-gate"))
        }
    }
    }
}

/** The first-launch gate: deciding whether the guide opens, or shut. */
private enum class Gate { DECIDING, SHUT }

/** The five-tab bottom bar. The current tab gets the accent indicator; everything else stays quiet. */
@Composable
fun PtBottomBar(currentRoute: String?, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Column {
        SoftDivider()
        NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
            for (tab in Tab.entries) {
                // The icon sinks while its tab is pressed: further than a button, as it is small.
                val interaction = remember { MutableInteractionSource() }
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.testTag("tab-${tab.route}"),
                    icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.pressScale(interaction, pressedScale = TAB_PRESS_SCALE)) },
                    interactionSource = interaction,
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

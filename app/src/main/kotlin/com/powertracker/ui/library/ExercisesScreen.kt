package com.powertracker.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Exercise
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.plan.PlanAdd
import com.powertracker.ui.plan.PlanViewModel
import com.powertracker.ui.plan.dayName
import com.powertracker.ui.theme.Space
import com.powertracker.ui.today.TodayAdd
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import com.powertracker.ui.components.rememberHaptics

/** What the tab says when an add could not be stored. */
const val SAVE_FAILED = "Couldn't save — try again."

/** The days "Add to plan" offers: the lifter's training days in weekday order, or the whole week when none are set. */
fun planDayChoices(trainingDays: Set<DayOfWeek>): List<DayOfWeek> = trainingDays.sorted().ifEmpty { DayOfWeek.entries.toList() }

/** How many exercises there are to browse: the catalogue, and the lifter's own that it does not have. */
fun exerciseCount(exercises: List<Exercise>): Int {
    val catalogue = Library.all().mapTo(HashSet()) { it.name.lowercase() }
    return catalogue.size + exercises.count { !it.archived && it.name.lowercase() !in catalogue }
}

/**
 * The Exercises tab: the catalogue and the lifter's own exercises to browse, each opening its form
 * guide with "Add to plan" and, during a session, "Add to today". Both go the way Plan and Today
 * add an exercise themselves; [todayViewModel] is the Today tab's own, so the session is the one
 * being logged there.
 */
@Composable
fun ExercisesScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    todayViewModel: TodayViewModel,
    onOpenPlan: () -> Unit,
    onOpenToday: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(training, settings, today)),
) {
    val plan by viewModel.state.collectAsStateWithLifecycle()
    val session by todayViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // The exercise whose sheet is open, and whether the sheet is on its guide or on the day to add it to.
    var open by rememberSaveable(stateSaver = PickerEntry.Saver) { mutableStateOf<PickerEntry?>(null) }
    var choosingDay by rememberSaveable { mutableStateOf(false) }
    // A sheet answers once: a second tap landing before it closes (a double tap) adds nothing more and says nothing more.
    val answered = remember { mutableStateOf(false) }
    // Add to plan (on the day picked) and Add to today tick once the add is stored; not when it was already there or failed.
    val haptics = rememberHaptics()
    fun once(add: () -> Unit) {
        if (!answered.value) {
            answered.value = true
            add()
        }
    }

    fun close() {
        open = null
        choosingDay = false
    }

    /** One message at a time: the newest replaces the one on screen. [action] runs if its label is tapped. */
    fun say(message: String, actionLabel: String? = null, action: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
            if (snackbar.showSnackbar(message, actionLabel, duration = duration) == SnackbarResult.ActionPerformed) action()
        }
    }

    ExercisesContent(
        exercises = plan.exercises,
        recentIds = plan.recentIds,
        onOpen = {
            open = it
            choosingDay = false
            answered.value = false
        },
        snackbar = snackbar,
    )

    open?.let { entry ->
        GuideSheet(onDismiss = ::close) {
            if (choosingDay) {
                DayChooserContent(
                    exerciseName = entry.name,
                    days = planDayChoices(plan.trainingDays),
                    onPick = { day ->
                        once {
                            viewModel.addOnce(day, entry) { result ->
                                close()
                                when (result) {
                                    PlanAdd.ADDED -> {
                                        haptics.tick()
                                        say("Added to ${dayName(day)}", "Open Plan", onOpenPlan)
                                    }
                                    PlanAdd.ALREADY_THERE -> say("Already in your plan on ${dayName(day)}.")
                                    PlanAdd.FAILED -> say(SAVE_FAILED)
                                }
                            }
                        }
                    },
                    onClose = ::close,
                    modifier = Modifier.navigationBarsPadding(),
                )
            } else {
                FormGuideContent(entry.name, onClose = ::close, modifier = Modifier.navigationBarsPadding()) {
                    GuideActions(
                        inSession = !session.loading && session.inSession,
                        onAddToPlan = { choosingDay = true },
                        onAddToToday = {
                            once {
                                // The view model looks at the session again now: it may have been finished since this button was drawn.
                                todayViewModel.addToOpenSession(entry.catalogueName, entry.exerciseId) { result ->
                                    close()
                                    when (result) {
                                        TodayAdd.ADDED -> {
                                            haptics.tick()
                                            say("Added to today", "Open Today", onOpenToday)
                                        }
                                        TodayAdd.ALREADY_THERE -> say("Already in today's session.")
                                        TodayAdd.REFUSED -> say(SAVE_FAILED)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** The tab without its view models or sheets: the header with the count, and the browser on its Muscles drop-down. */
@Composable
fun ExercisesContent(
    /** The lifter's stored exercises. */
    exercises: List<Exercise>,
    /** Stored exercise ids, most recently used first. */
    recentIds: List<Long>,
    /** A row or its info button was tapped: both open the guide. */
    onOpen: (PickerEntry) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    browser: ExerciseBrowserState = rememberExerciseBrowserState(MuscleFilter.DROPDOWN),
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenHeader("Exercises", eyebrow = "${exerciseCount(exercises)} exercises", modeLabel = true) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExerciseBrowser(
            state = browser,
            exercises = exercises,
            recentIds = recentIds,
            onRow = onOpen,
            onInfo = onOpen,
            rowLabel = { "Open $it" },
            modifier = Modifier.padding(padding),
            showThumbnails = true,
        )
    }
}

/** What the guide sheet offers on the Exercises tab: the plan always, today's session while one is open. */
@Composable
fun GuideActions(inSession: Boolean, onAddToPlan: () -> Unit, onAddToToday: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        if (inSession) SecondaryButton("Add to today", onAddToToday, Modifier.weight(1f))
        PrimaryButton("Add to plan", onAddToPlan, Modifier.weight(1f))
    }
}

/** The second step of "Add to plan", in the guide's sheet: which day [exerciseName] goes on. */
@Composable
fun DayChooserContent(exerciseName: String, days: List<DayOfWeek>, onPick: (DayOfWeek) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.xl, end = Space.s, bottom = Space.xl)
            .testTag("day-chooser"),
    ) {
        GuideHeader("Add to plan", exerciseName, closeLabel = "Close", onClose = onClose)
        Column(Modifier.padding(top = Space.m, end = Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text("Which day?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (day in days) SecondaryButton(dayName(day), { onPick(day) }, Modifier.fillMaxWidth().testTag("day-${day.name}"))
        }
    }
}

package com.powertracker.ui.plan.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.repsText
import com.powertracker.domain.plan.DraftExercise
import com.powertracker.domain.plan.DraftSession
import com.powertracker.domain.plan.GroupVolume
import com.powertracker.domain.plan.LeftOut
import com.powertracker.domain.plan.PlanDraft
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.library.ExerciseBrowser
import com.powertracker.ui.library.FormGuideSheet
import com.powertracker.ui.library.GuideHeader
import com.powertracker.ui.library.GuideSheet
import com.powertracker.ui.library.MuscleFilter
import com.powertracker.ui.library.MuscleOptions
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.library.rememberExerciseBrowserState
import com.powertracker.ui.plan.dayName
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// The builder's words (stage 2 plan, Task 2). The web app's builder uses the same.
const val BUILDER_TITLE = "Build my plan"
const val DAYS_HEADING = "How many days a week?"
const val DO_NOW_HEADING = "Which exercises do you do now?"
const val WOULD_LIKE_HEADING = "Which exercises would you like to do?"
const val CHOOSE_FOR_ME = "Choose for me"
const val BODY_PARTS_HEADING = "Which body parts do you most want to improve?"
const val BODY_PARTS_LINE = "Up to 3. They get more sets and go first in their sessions."
const val TOO_MANY_PRIORITIES = "That's 3 already. Untick one to choose another."
const val DRAFT_HEADING = "Your draft week"
const val DRAFT_LINE = "Swap any exercise, or ask for another draft. Nothing is saved until you use this plan."
const val WEEKLY_SETS = "Weekly sets"
const val UNDER_GUIDELINE = "Under 10"
const val GUIDELINE = "10 or more a week is the guideline for growth. Make a body part a priority to give it more."
const val LEFT_OUT = "Left out"
const val SWAP = "Swap"
const val NO_ALTERNATIVES = "Nothing else in the library fits this place."
const val ANOTHER_DRAFT = "Another draft"
const val USE_THIS_PLAN = "Use this plan"
const val REPLACE_WARNING = "This replaces your bodybuilding plan. Your log is kept."
const val REPLACE = "Replace"
const val SAVE_FAILED = "The plan could not be saved. Nothing was changed."

// The ways in, on the Plan screen.
const val BUILD_MY_PLAN = "Build my plan"
const val REBUILD_MY_PLAN = "Rebuild my plan"
const val PLAN_OPTIONS = "Plan options"

fun chosenText(count: Int): String = "$count chosen"
fun daysText(count: Int): String = "$count days"
fun chooseDaysText(count: Int): String = "Choose $count days."
fun stepText(step: BuilderStep): String = "Step ${step.ordinal + 1} of ${BuilderStep.entries.size}"

/** "3 × 8–12 @ 8", as a Plan row prints it. */
fun prescriptionText(e: DraftExercise): String = "${e.sets} × ${repsText(e.repsLow, e.repsHigh)} @ ${formatRpe(e.rpe)}"

/** What the builder's content can ask of its view model. */
data class PlanBuilderActions(
    val onDayCount: (Int) -> Unit = {},
    val onDay: (DayOfWeek) -> Unit = {},
    val isTicked: (PickerEntry) -> Boolean = { false },
    val onToggle: (PickerEntry) -> Unit = {},
    val onPriority: (MuscleGroup) -> Unit = {},
    val onNext: () -> Unit = {},
    /** A step back; on the first step, leaving. */
    val onBack: () -> Unit = {},
    val onAnotherDraft: () -> Unit = {},
    val alternatives: (DayOfWeek, String) -> List<LibraryExercise> = { _, _ -> emptyList() },
    val onSwap: (day: DayOfWeek, from: String, to: String) -> Unit = { _, _, _ -> },
    val onUse: () -> Unit = {},
    val onConfirmReplace: () -> Unit = {},
    val onCancelReplace: () -> Unit = {},
)

/**
 * The bodybuilding plan builder, over the whole screen. [onClose] leaves it with nothing changed (back from the first
 * step, or the app is on the powerlifting side, where the builder is not offered); [onSaved] follows "Use this plan".
 */
@Composable
fun PlanBuilderScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    viewModel: PlanBuilderViewModel = viewModel(factory = PlanBuilderViewModel.factory(training, settings, today)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    val saved by rememberUpdatedState(onSaved)
    LaunchedEffect(state.loading, state.available) { if (!state.loading && !state.available) close() }
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    val actions = remember(viewModel) {
        PlanBuilderActions(
            onDayCount = viewModel::chooseDayCount,
            onDay = viewModel::toggleDay,
            isTicked = viewModel::isTicked,
            onToggle = viewModel::toggle,
            onPriority = viewModel::togglePriority,
            onNext = viewModel::next,
            onBack = { if (!viewModel.back()) close() },
            onAnotherDraft = viewModel::anotherDraft,
            alternatives = viewModel::alternatives,
            onSwap = viewModel::swap,
            onUse = viewModel::use,
            onConfirmReplace = viewModel::confirmReplace,
            onCancelReplace = viewModel::cancelReplace,
        )
    }
    BackHandler(enabled = !state.saved) { actions.onBack() }
    PlanBuilderContent(state, actions)
}

/** The builder without its view model: the step's content under a header, Back and Next at the bottom. */
@Composable
fun PlanBuilderContent(state: PlanBuilderState, actions: PlanBuilderActions) {
    val scheme = MaterialTheme.colorScheme
    if (state.loading || !state.available) {
        Box(Modifier.fillMaxSize())
        return
    }
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            ScreenHeader(
                BUILDER_TITLE,
                eyebrow = stepText(state.step),
                modeLabel = true,
                navigation = { IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = { BuilderBar(state, actions) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.step) {
                BuilderStep.DAYS -> DaysStep(state, actions)
                BuilderStep.EXERCISES -> ExercisesStep(state, actions)
                BuilderStep.BODY_PARTS -> BodyPartsStep(state, actions)
                BuilderStep.DRAFT -> state.draft?.let { DraftStep(it, state, actions) }
            }
        }
    }
    if (state.confirming) {
        AlertDialog(
            onDismissRequest = actions.onCancelReplace,
            text = { Text(REPLACE_WARNING, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { PrimaryButton(REPLACE, actions.onConfirmReplace, compact = true) },
            dismissButton = { QuietButton("Cancel", actions.onCancelReplace) },
        )
    }
}

@Composable
private fun BuilderBar(state: PlanBuilderState, actions: PlanBuilderActions) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = PowertrackerTheme.colors.hairline)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                if (state.step == BuilderStep.DRAFT) {
                    SecondaryButton(ANOTHER_DRAFT, actions.onAnotherDraft, Modifier.weight(1f).testTag("builder-another"), enabled = !state.saving)
                    PrimaryButton(USE_THIS_PLAN, actions.onUse, Modifier.weight(1f).testTag("builder-use"), enabled = !state.saving)
                } else {
                    SecondaryButton("Back", actions.onBack, Modifier.weight(1f).testTag("builder-back"))
                    val label = when {
                        state.step == BuilderStep.EXERCISES && state.nothingTicked -> CHOOSE_FOR_ME
                        state.step == BuilderStep.BODY_PARTS && state.priorities.isEmpty() -> "Skip"
                        else -> "Next"
                    }
                    PrimaryButton(label, actions.onNext, Modifier.weight(1f).testTag("builder-next"), enabled = state.canNext)
                }
            }
        }
    }
}

@Composable
private fun StepHeading(text: String, line: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = Space.s)) {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
        if (line != null) {
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.xs))
        }
    }
}

@Composable
private fun DaysStep(state: PlanBuilderState, actions: PlanBuilderActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        StepHeading(DAYS_HEADING)
        SegmentedChoice(listOf(daysText(3), daysText(4)), selectedIndex = state.dayCount - 3, onSelect = { actions.onDayCount(it + 3) })
        Column {
            SectionHeader("Which days", Modifier.padding(top = Space.s))
            PtCard(contentPadding = PaddingValues(horizontal = Space.s, vertical = Space.s)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    for (day in DayOfWeek.entries) {
                        ChoiceChip(
                            text = day.toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                            selected = day in state.days,
                            onClick = { actions.onDay(day) },
                            modifier = Modifier.weight(1f).testTag("builder-day-${day.name}"),
                        )
                    }
                }
            }
            val line = if (state.days.size == state.dayCount) state.days.sorted().joinToString(", ") { dayName(it) } else chooseDaysText(state.dayCount)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.s, start = Space.xs))
        }
    }
}

@Composable
private fun ExercisesStep(state: PlanBuilderState, actions: PlanBuilderActions) {
    val browser = rememberExerciseBrowserState(MuscleFilter.DROPDOWN)
    var guideFor by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        StepHeading(
            if (state.nothingTicked) WOULD_LIKE_HEADING else DO_NOW_HEADING,
            chosenText(state.chosenCount),
            Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.s),
        )
        ExerciseBrowser(
            state = browser,
            exercises = state.exercises,
            recentIds = state.recentIds,
            onRow = actions.onToggle,
            onInfo = { guideFor = it.name },
            rowLabel = { it },
            modifier = Modifier.weight(1f),
            ticked = { entry -> state.ticked.containsKey(pickKey(entry)) },
        )
    }
    guideFor?.let { name -> FormGuideSheet(name, onDismiss = { guideFor = null }) }
}

@Composable
private fun BodyPartsStep(state: PlanBuilderState, actions: PlanBuilderActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.s)) {
        StepHeading(BODY_PARTS_HEADING, BODY_PARTS_LINE)
        PtCard(Modifier.padding(top = Space.s), contentPadding = PaddingValues(start = Space.m, end = Space.m, top = Space.s, bottom = Space.s)) {
            MuscleOptions(state.targetGroups, state.priorities.toSet(), actions.onPriority)
        }
        if (state.priorityRefused) {
            Text(
                TOO_MANY_PRIORITIES,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.m, start = Space.xs).testTag("priority-refused"),
            )
        }
    }
}

@Composable
private fun DraftStep(draft: PlanDraft, state: PlanBuilderState, actions: PlanBuilderActions) {
    var swapping by remember { mutableStateOf<Pair<DayOfWeek, String>?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("draft-list"),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        item(key = "heading") { StepHeading(DRAFT_HEADING, DRAFT_LINE, Modifier.testTag("heading")) }
        if (state.failed) {
            item(key = "failed") { Text(SAVE_FAILED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
        items(draft.sessions, key = { it.day }) { session ->
            SessionCard(session, Modifier.testTag("session-${session.day.name}"), onSwap = { name -> swapping = session.day to name })
        }
        item(key = "volume") { VolumeCard(draft.volume, Modifier.testTag("volume")) }
        if (draft.leftOut.isNotEmpty()) item(key = "left-out") { LeftOutCard(draft.leftOut) }
    }
    swapping?.let { (day, name) ->
        val options = remember(draft, day, name) { actions.alternatives(day, name) }
        GuideSheet(onDismiss = { swapping = null }) {
            Column(Modifier.fillMaxWidth().padding(start = Space.xl, end = Space.s, bottom = Space.xl).testTag("alternatives")) {
                GuideHeader("$SWAP $name", dayName(day), closeLabel = "Close", onClose = { swapping = null })
                if (options.isEmpty()) {
                    Text(NO_ALTERNATIVES, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.m))
                } else {
                    LazyColumn(Modifier.padding(top = Space.s, end = Space.l)) {
                        items(options, key = { it.name }) { option ->
                            AlternativeRow(option) {
                                actions.onSwap(day, name, option.name)
                                swapping = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: DraftSession, modifier: Modifier = Modifier, onSwap: (String) -> Unit) {
    val sets = session.exercises.sumOf { it.sets }
    PtCard(modifier, contentPadding = PaddingValues(start = Space.l, end = Space.s, top = Space.m, bottom = Space.s), spacing = 0.dp) {
        Column(Modifier.padding(end = Space.s, bottom = Space.xs)) {
            Text("${dayName(session.day)} · ${session.title}", style = MaterialTheme.typography.headlineMedium)
            Text("${session.exercises.size} exercises · $sets sets", style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted)
        }
        for (exercise in session.exercises) {
            HorizontalDivider(Modifier.padding(end = Space.s), color = PowertrackerTheme.colors.hairline)
            DraftRow(exercise, onSwap = { onSwap(exercise.name) })
        }
    }
}

@Composable
private fun DraftRow(exercise: DraftExercise, onSwap: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val muted = PowertrackerTheme.colors.muted
    val prescription = buildAnnotatedString {
        append("${exercise.sets} × ${repsText(exercise.repsLow, exercise.repsHigh)}")
        withStyle(SpanStyle(color = muted)) { append(" @ ") }
        append(formatRpe(exercise.rpe))
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = Space.s)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(prescription, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
        }
        QuietButton(SWAP, onSwap, Modifier.testTag("swap"))
    }
}

@Composable
private fun VolumeCard(volume: List<GroupVolume>, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionHeader(WEEKLY_SETS, Modifier.padding(top = Space.s))
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.s), spacing = 0.dp) {
            volume.forEachIndexed { i, v ->
                if (i > 0) HorizontalDivider(color = PowertrackerTheme.colors.hairline)
                VolumeRow(v)
            }
            Text(GUIDELINE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.m, bottom = Space.s))
        }
    }
}

@Composable
private fun VolumeRow(v: GroupVolume) {
    val scheme = MaterialTheme.colorScheme
    val spoken = "${v.group.label}, ${v.weeklySets} sets" + if (v.short) ", ${UNDER_GUIDELINE.lowercase()}" else ""
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .testTag(if (v.short) "short-${v.group.name}" else "volume-${v.group.name}")
            .clearAndSetSemantics { text = AnnotatedString(spoken) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(v.group.label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
        if (v.short) {
            Text(
                UNDER_GUIDELINE,
                style = MaterialTheme.typography.labelMedium,
                color = PowertrackerTheme.colors.muted,
                modifier = Modifier.padding(end = Space.m),
            )
        }
        Text(v.weeklySets.toString(), style = MaterialTheme.typography.titleMedium, color = if (v.short) scheme.onSurfaceVariant else scheme.onSurface)
    }
}

@Composable
private fun LeftOutCard(leftOut: List<LeftOut>) {
    Column {
        SectionHeader(LEFT_OUT, Modifier.padding(top = Space.s))
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.s), spacing = 0.dp) {
            leftOut.forEachIndexed { i, l ->
                if (i > 0) HorizontalDivider(color = PowertrackerTheme.colors.hairline)
                Column(Modifier.fillMaxWidth().padding(vertical = Space.s)) {
                    Text(l.name, style = MaterialTheme.typography.titleMedium)
                    Text(l.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AlternativeRow(option: LibraryExercise, onPick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClickLabel = "Swap in ${option.name}", onClick = onPick).padding(vertical = Space.s).testTag("alternative"),
        ) {
            Text(option.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            if (option.muscles.isNotEmpty()) Text(option.muscles.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        HorizontalDivider(color = PowertrackerTheme.colors.hairline)
    }
}

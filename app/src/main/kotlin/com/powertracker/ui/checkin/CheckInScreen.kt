package com.powertracker.ui.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.AdviceRepository
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.GroupNote
import com.powertracker.domain.coach.NoteReason
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.library.MuscleOptions
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.LocalDate
import com.powertracker.ui.components.PtIconButton

// The check-in's words (stage 3 plan, Task 2). The web app's check-in uses the same.
const val CHECK_IN_TITLE = "Check-in"
const val LAGGING_HEADING = "Which body parts are lagging?"
const val LAGGING_LINE = "Leave a body part unticked if it is on track."
const val GROWING_HEADING = "Which are growing well?"
const val GROWING_LINE = "Body parts marked lagging can't also be growing well."
const val SAVED_HEADING = "Check-in saved"
const val MORE_WORK_SUGGESTED = "More work suggested on Coach"
const val SEE_COACH = "See Coach"
const val SAVE_FAILED = "The check-in could not be saved. Nothing was changed."

/** "Step 1 of 2": the two questions; the Saved step has no number. */
fun checkInStepText(step: CheckInStep): String? = when (step) {
    CheckInStep.LAGGING -> "Step 1 of 2"
    CheckInStep.GROWING -> "Step 2 of 2"
    CheckInStep.SAVED -> null
}

/** Why a lagging body part got no suggestion, as the Saved step says it. */
fun noteText(note: GroupNote): String {
    val group = note.group.label
    return when (note.reason) {
        NoteReason.AT_CEILING -> "$group already gets ${note.weeklySets} sets a week, the most the coach adds."
        NoteReason.NO_EXERCISE -> "No other ${group.lowercase()} exercise uses the equipment in your plan."
        NoteReason.FULL -> "Every session that could take one already has ${PhysiqueCoach.MAX_EXERCISES} exercises."
        NoteReason.LATER -> "The coach adds to three body parts at a time. It will look at ${group.lowercase()} again at your next check-in."
        NoteReason.WOULD_PASS_CEILING -> "More ${group.lowercase()} work would take a body part past ${PhysiqueCoach.WEEKLY_CEILING} sets a week."
    }
}

/** What the check-in's content can ask of its view model. */
data class CheckInActions(
    val onLagging: (MuscleGroup) -> Unit = {},
    val onGrowing: (MuscleGroup) -> Unit = {},
    val onNext: () -> Unit = {},
    /** A step back; on the first step, leaving. */
    val onBack: () -> Unit = {},
    /** Leaves with nothing stored (before Save). */
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onSeeCoach: () -> Unit = {},
    val onDone: () -> Unit = {},
)

/**
 * The bodybuilding check-in, over the whole screen. [onClose] leaves it (from the first step, after Save with Done, or
 * when the app is on the powerlifting side, where there are no check-ins); [onSeeCoach] follows "See Coach".
 */
@Composable
fun CheckInScreen(
    training: TrainingRepository,
    advice: AdviceRepository,
    settings: SettingsRepository,
    onClose: () -> Unit,
    onSeeCoach: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    viewModel: CheckInViewModel = viewModel(factory = CheckInViewModel.factory(training, advice, settings, today)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    val coach by rememberUpdatedState(onSeeCoach)
    LaunchedEffect(state.loading, state.available) { if (!state.loading && !state.available) close() }
    val actions = remember(viewModel) {
        CheckInActions(
            onLagging = viewModel::toggleLagging,
            onGrowing = viewModel::toggleGrowing,
            onNext = viewModel::next,
            onBack = { if (!viewModel.back()) close() },
            onClose = { close() },
            onSave = viewModel::save,
            onSeeCoach = { coach() },
            onDone = { close() },
        )
    }
    BackHandler { actions.onBack() }
    CheckInContent(state, actions)
}

/** The check-in without its view model: the step under a header, its buttons at the bottom. */
@Composable
fun CheckInContent(state: CheckInState, actions: CheckInActions) {
    val scheme = MaterialTheme.colorScheme
    if (state.loading || !state.available) {
        Box(Modifier.fillMaxSize())
        return
    }
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            ScreenHeader(
                CHECK_IN_TITLE,
                eyebrow = checkInStepText(state.step),
                modeLabel = true,
                navigation = if (state.step == CheckInStep.SAVED) null else {
                    { PtIconButton(onClick = actions.onClose) { Icon(Icons.Filled.Close, "Close") } }
                },
            )
        },
        bottomBar = { CheckInBar(state, actions) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.s),
        ) {
            when (state.step) {
                CheckInStep.LAGGING -> {
                    StepHeading(LAGGING_HEADING, LAGGING_LINE)
                    Groups(state.lagging, actions.onLagging)
                }
                CheckInStep.GROWING -> {
                    StepHeading(GROWING_HEADING, GROWING_LINE)
                    Groups(state.growingWell, actions.onGrowing, locked = state.lagging)
                }
                CheckInStep.SAVED -> Saved(state)
            }
            if (state.failed) {
                Text(SAVE_FAILED, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.padding(top = Space.m, start = Space.xs))
            }
        }
    }
}

@Composable
private fun StepHeading(text: String, line: String? = null) {
    Column(Modifier.padding(bottom = Space.s)) {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
        if (line != null) {
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.xs))
        }
    }
}

/** The 18 body parts as tick boxes, the Exercises tab's Muscles list. */
@Composable
private fun Groups(ticked: Set<MuscleGroup>, onToggle: (MuscleGroup) -> Unit, locked: Set<MuscleGroup> = emptySet()) {
    PtCard(Modifier.padding(top = Space.s), contentPadding = PaddingValues(start = Space.m, end = Space.m, top = Space.s, bottom = Space.s)) {
        MuscleOptions(MuscleGroup.entries, ticked, onToggle, locked = locked)
    }
}

@Composable
private fun Saved(state: CheckInState) {
    val pt = PowertrackerTheme.colors
    StepHeading(SAVED_HEADING)
    if (state.lines.isEmpty()) return
    PtCard(Modifier.padding(top = Space.s), contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
        state.lines.forEachIndexed { i, line ->
            if (i > 0) SoftDivider()
            Column(Modifier.fillMaxWidth().padding(vertical = Space.m).semantics(mergeDescendants = true) {}.testTag("saved-${line.group.name}")) {
                Text(line.group.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(line.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CheckInBar(state: CheckInState, actions: CheckInActions) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding()) {
            SoftDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                when (state.step) {
                    CheckInStep.LAGGING -> PrimaryButton("Next", actions.onNext, Modifier.weight(1f).testTag("checkin-next"))
                    CheckInStep.GROWING -> {
                        SecondaryButton("Back", actions.onBack, Modifier.weight(1f).testTag("checkin-back"), enabled = !state.saving)
                        PrimaryButton("Save", actions.onSave, Modifier.weight(1f).testTag("checkin-save"), enabled = !state.saving)
                    }
                    CheckInStep.SAVED -> if (state.hasAdvice) {
                        SecondaryButton("Done", actions.onDone, Modifier.weight(1f).testTag("checkin-done"))
                        PrimaryButton(SEE_COACH, actions.onSeeCoach, Modifier.weight(1f).testTag("checkin-coach"))
                    } else {
                        PrimaryButton("Done", actions.onDone, Modifier.weight(1f).testTag("checkin-done"))
                    }
                }
            }
        }
    }
}

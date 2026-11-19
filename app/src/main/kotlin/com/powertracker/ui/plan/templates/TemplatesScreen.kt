package com.powertracker.ui.plan.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.PlanSnapshot
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanDraft
import com.powertracker.domain.plan.PlanTemplate
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.plan.builder.CLOSE
import com.powertracker.ui.plan.builder.DraftLeftOutCard
import com.powertracker.ui.plan.builder.DraftSessionCard
import com.powertracker.ui.plan.builder.DraftSwapSheet
import com.powertracker.ui.plan.builder.DraftVolumeCard
import com.powertracker.ui.plan.builder.REPLACE
import com.powertracker.ui.plan.builder.SAVE_FAILED
import com.powertracker.ui.plan.builder.USE_THIS_PLAN
import com.powertracker.ui.plan.builder.labelFitsHalf
import com.powertracker.ui.plan.builder.WHICH_DAYS_HEADING
import com.powertracker.ui.plan.builder.WeekdayChips
import com.powertracker.ui.plan.builder.chooseDaysText
import com.powertracker.ui.plan.builder.chosenDaysText
import com.powertracker.ui.plan.dayName
import com.powertracker.ui.theme.LocalTrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.DayOfWeek

// The templates' words (1.14 plan, Task 3). The web app uses the same.
const val CHOOSE_A_TEMPLATE = "Choose a template"
const val REPLACE_KEPT = "Your history and bests are kept."
const val CANCEL = "Cancel"

/** "3 days a week · New to powerlifting: 2–3 days a week", under a template's name. */
fun templateDaysLine(t: PlanTemplate): String = "${t.dayCount} days a week · ${t.suits}"

/** "Upper A · Lower A · Upper B · Lower B": the template's day titles in order. */
fun templateDayTitles(t: PlanTemplate): String = t.sessions.joinToString(" · ") { it.title }

/** The confirm's title: "Replace your powerlifting plan?" or "Replace your bodybuilding plan?". */
fun replaceTitle(mode: TrainingMode): String = "Replace your ${if (mode == TrainingMode.BODYBUILDING) "bodybuilding" else "powerlifting"} plan?"

/** The snackbar on Plan after a save: "Plan set: 4-day upper/lower." */
fun planSetText(name: String): String = "Plan set: $name."

/** A draft day's name: its weekday once the weekdays match the template, else "Day 1", "Day 2", …. */
fun templateDayName(index: Int, day: DayOfWeek, daysMatch: Boolean): String = if (daysMatch) dayName(day) else "Day ${index + 1}"

/** A draft day's heading: "Monday · Squat & bench" once the weekdays match the template, else "Day 1 · Squat & bench". */
fun templateSessionHeading(index: Int, day: DayOfWeek, title: String, daysMatch: Boolean): String =
    "${templateDayName(index, day, daysMatch)} · $title"

/** What the templates' content can ask of its view model. */
data class TemplatesActions(
    val onChoose: (String) -> Unit = {},
    val onDay: (DayOfWeek) -> Unit = {},
    val alternatives: (DayOfWeek, String) -> List<LibraryExercise> = { _, _ -> emptyList() },
    val onSwap: (day: DayOfWeek, from: String, to: String) -> Unit = { _, _, _ -> },
    val onUse: () -> Unit = {},
    val onConfirmReplace: () -> Unit = {},
    val onCancelReplace: () -> Unit = {},
    /** From the draft, back to the list; from the list, leaving. */
    val onBack: () -> Unit = {},
    /** Leaves from either step, with nothing saved. */
    val onClose: () -> Unit = {},
)

/**
 * Choosing a plan template for [mode]'s side, over the whole screen. [onClose] leaves with nothing changed; [onSaved]
 * follows Use this plan, with the template's name for Plan's snackbar; just before it, [onSavedUndo] gets what that
 * snackbar's Undo puts back, or null when the side had no plan (1.14.1).
 */
@Composable
fun TemplatesScreen(
    mode: TrainingMode,
    training: TrainingRepository,
    settings: SettingsRepository,
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    onSavedUndo: (PlanSnapshot?) -> Unit = {},
    viewModel: TemplatesViewModel = viewModel(key = "templates-${mode.name}", factory = TemplatesViewModel.factory(mode, training, settings)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    val saved by rememberUpdatedState(onSaved)
    val savedUndo by rememberUpdatedState(onSavedUndo)
    LaunchedEffect(state.saved) {
        if (state.saved) state.chosen?.let {
            savedUndo(state.undo)
            saved(it.name)
        }
    }
    val actions = remember(viewModel) {
        TemplatesActions(
            onChoose = viewModel::choose,
            onDay = viewModel::toggleDay,
            alternatives = viewModel::alternatives,
            onSwap = viewModel::swap,
            onUse = viewModel::use,
            onConfirmReplace = viewModel::confirmReplace,
            onCancelReplace = viewModel::cancelReplace,
            onBack = { if (!viewModel.back()) close() },
            onClose = { close() },
        )
    }
    BackHandler(enabled = !state.saved) { actions.onBack() }
    TemplatesContent(state, actions)
}

/** The templates without their view model: the list, or the chosen template's draft with Back and Use this plan. */
@Composable
fun TemplatesContent(state: TemplatesState, actions: TemplatesActions) {
    val scheme = MaterialTheme.colorScheme
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    val chosen = state.chosen
    val draft = state.draft
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            // The mode label is the side these templates are for, as on the web, not whichever side the app is on.
            CompositionLocalProvider(LocalTrainingMode provides state.mode) {
                ScreenHeader(
                    if (state.step == TemplateStep.DRAFT && chosen != null) chosen.name else CHOOSE_A_TEMPLATE,
                    eyebrow = if (state.step == TemplateStep.DRAFT) CHOOSE_A_TEMPLATE else null,
                    modeLabel = true,
                    navigation = { PtIconButton(onClick = actions.onClose) { Icon(Icons.Filled.Close, CLOSE) } },
                    // "5-day body-part split" is a line and a half at 320 dp with large text.
                    titleMaxLines = Int.MAX_VALUE,
                )
            }
        },
        bottomBar = { if (state.step == TemplateStep.DRAFT) DraftBar(state, actions) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.step == TemplateStep.DRAFT && chosen != null && draft != null) {
                TemplateDraft(chosen, draft, state, actions)
            } else {
                TemplateList(state.templates, actions)
            }
        }
    }
    if (state.confirming) {
        AlertDialog(
            onDismissRequest = actions.onCancelReplace,
            title = { Text(replaceTitle(state.mode)) },
            text = { Text(REPLACE_KEPT, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { PrimaryButton(REPLACE, actions.onConfirmReplace, Modifier.testTag("templates-replace"), compact = true) },
            dismissButton = { QuietButton(CANCEL, actions.onCancelReplace) },
        )
    }
}

@Composable
private fun TemplateList(templates: List<PlanTemplate>, actions: TemplatesActions) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("template-list"),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        items(templates, key = { it.id }) { template -> TemplateRow(template) { actions.onChoose(template.id) } }
    }
}

/** One template in the list: its name, "n days a week · who it suits", and its days' titles. The whole card opens it. */
@Composable
private fun TemplateRow(template: PlanTemplate, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    PtCard(
        Modifier.fillMaxWidth().testTag("template-${template.id}"),
        spacing = Space.xs,
        onClickLabel = "Open ${template.name}",
        onClick = onClick,
    ) {
        Text(template.name, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, modifier = Modifier.semantics { heading() })
        Text(templateDaysLine(template), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text(templateDayTitles(template), style = MaterialTheme.typography.bodyMedium, color = PowertrackerTheme.colors.muted)
    }
}

@Composable
private fun TemplateDraft(template: PlanTemplate, draft: PlanDraft, state: TemplatesState, actions: TemplatesActions) {
    // The day, its index in the week (for "Day n" while the count is off) and the exercise being swapped.
    var swapping by remember { mutableStateOf<Triple<DayOfWeek, Int, String>?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.failed) { if (state.failed) listState.animateScrollToItem(0) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("draft-list"),
        state = listState,
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        if (state.failed) {
            item(key = "failed") {
                // Said as soon as it shows, as the web's alert is.
                Text(
                    SAVE_FAILED,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("save-failed").semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
        }
        item(key = "days") {
            Column {
                Text(
                    WHICH_DAYS_HEADING,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = Space.s).semantics { heading() },
                )
                WeekdayChips(state.days, actions.onDay, tagPrefix = "template-day-")
                val line = if (state.daysMatch) chosenDaysText(state.days) else chooseDaysText(template.dayCount)
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Read out when it changes, as the web's status line is.
                    modifier = Modifier.padding(top = Space.s, start = Space.xs).testTag("days-line").semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        itemsIndexed(draft.sessions, key = { i, _ -> "session-$i" }) { i, session ->
            DraftSessionCard(
                session,
                Modifier.testTag("session-$i"),
                headingText = templateSessionHeading(i, session.day, session.title, state.daysMatch),
                fixedReps = state.fixedReps,
                canSwap = { TemplatesViewModel.canSwap(it.name) },
                onSwap = { name -> swapping = Triple(session.day, i, name) },
            )
        }
        // The bodybuilding draft shows each body part's weekly sets, as the builder's does; powerlifting has none.
        if (state.mode == TrainingMode.BODYBUILDING) item(key = "volume") { DraftVolumeCard(draft.volume, Modifier.testTag("volume")) }
        if (draft.leftOut.isNotEmpty()) item(key = "left-out") { DraftLeftOutCard(draft.leftOut) }
    }
    swapping?.let { (day, index, name) ->
        val options = remember(draft, day, name) { actions.alternatives(day, name) }
        DraftSwapSheet(
            day, name, options,
            // The sheet names the day as its card does.
            dayLabel = templateDayName(index, day, state.daysMatch),
            onPick = { to ->
                actions.onSwap(day, name, to)
                swapping = null
            },
            onDismiss = { swapping = null },
        )
    }
}

@Composable
private fun DraftBar(state: TemplatesState, actions: TemplatesActions) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding()) {
            SoftDivider()
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m)) {
                // Half each, as the builder's bar, while "Use this plan" fits half; else (320 dp with large text) Back
                // takes its own width and Use this plan the rest.
                val halves = labelFitsHalf(USE_THIS_PLAN, maxWidth)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    SecondaryButton("Back", actions.onBack, (if (halves) Modifier.weight(1f) else Modifier).testTag("templates-back"), enabled = !state.saving)
                    PrimaryButton(USE_THIS_PLAN, actions.onUse, Modifier.weight(1f).testTag("templates-use"), enabled = state.canUse)
                }
            }
        }
    }
}

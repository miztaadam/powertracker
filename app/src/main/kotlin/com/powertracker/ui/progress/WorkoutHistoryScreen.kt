package com.powertracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.TrainingRepository
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.PastSets
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** Every past workout, newest first, from both sides (1.20): opened from Progress's History button. Read-only. */
@Composable
fun WorkoutHistoryScreen(training: TrainingRepository, onBack: () -> Unit, today: () -> LocalDate = ::systemToday) {
    val viewModel: WorkoutHistoryViewModel = viewModel(factory = WorkoutHistoryViewModel.factory(training))
    val state by viewModel.state.collectAsStateWithLifecycle()
    WorkoutHistoryContent(state, onBack, today())
}

private val WORKOUT_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/**
 * The History screen without its view model. The list is lazy: only the workouts on screen are drawn, so a long log
 * needs no "Show older workouts" (the web app draws twenty at a time instead). [today] (1.22) is the day it is, for the
 * calendar: it has no default, so no test or picture reads the real clock by leaving it out.
 */
@Composable
fun WorkoutHistoryContent(state: WorkoutHistoryState, onBack: () -> Unit, today: LocalDate) {
    val total = state.workouts.size
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                "History",
                eyebrow = if (state.loading) null else count(total, "workout"),
                navigation = { PtIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        // The sides are each item's own: the calendar reaches closer to the screen's edge than the cards do, so that
        // its seven columns are each a full touch target on a phone (360 dp: 50 dp a column; 320 dp: 45).
        val gutter = Modifier.padding(horizontal = Space.gutter)
        LazyColumn(
            Modifier.padding(padding).fillMaxSize().testTag("workout-list"),
            state = list,
            contentPadding = PaddingValues(bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            if (!state.loading && total == 0) {
                item(key = "empty") {
                    Box(gutter) { EmptyState(Icons.Filled.FitnessCenter, "No workouts yet", "Every workout you log on Today will be listed here, with each exercise and its sets.") }
                }
            }
            // With nothing logged (or nothing read yet) there is nothing to mark: no calendar.
            if (total > 0) {
                item(key = "calendar") {
                    WorkoutCalendar(
                        state.workouts, today,
                        // The workout's card to the top of the list, at once. The calendar is the list's first item.
                        onOpen = { workout -> state.workouts.indexOf(workout).takeIf { it >= 0 }?.let { index -> scope.launch { list.scrollToItem(index + 1) } } },
                        modifier = Modifier.padding(horizontal = Space.xs),
                    )
                }
            }
            items(state.workouts, key = { it.sessionId }) { workout -> Box(gutter) { WorkoutCard(workout) } }
        }
    }
}

/** One workout: its date, its side and counts, then each exercise with every set of it. */
@Composable
private fun WorkoutCard(workout: Workout) {
    val scheme = MaterialTheme.colorScheme
    PtCard(
        Modifier.testTag("workout-${workout.sessionId}"),
        contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.m, bottom = Space.m),
        spacing = Space.xs,
    ) {
        Text(
            WORKOUT_DATE.format(workout.date.toJavaLocalDate()),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        // Wraps: a summary with a bodyweight is longer than the card. Balanced, as the web's: no one word left alone on
        // the second line.
        Text(
            workout.summary.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(lineBreak = LineBreak.Heading),
            color = PowertrackerTheme.colors.muted,
            modifier = Modifier.padding(bottom = Space.xs),
        )
        workout.exercises.forEachIndexed { i, exercise ->
            if (i > 0) SoftDivider(Modifier.padding(vertical = Space.xs))
            Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.semantics { heading() })
                PastSets(exercise.sets)
            }
        }
    }
}

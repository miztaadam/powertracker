package com.powertracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.MainLift
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.components.StatTile
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Every session of one lift, newest first. [lift] is the route argument: a [MainLift] name. */
@Composable
fun LiftHistoryScreen(
    training: TrainingRepository,
    lift: String,
    onBack: () -> Unit,
    today: () -> LocalDate = LocalDate::now,
) {
    val mainLift = MainLift.entries.firstOrNull { it.name == lift } ?: MainLift.SQUAT
    val viewModel: LiftHistoryViewModel = viewModel(key = mainLift.name, factory = LiftHistoryViewModel.factory(training, mainLift, today))
    val state by viewModel.state.collectAsStateWithLifecycle()
    LiftHistoryContent(mainLift, state, onBack)
}

private val SESSION_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
private val BEST_DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

/** The lift history screen without its view model. */
@Composable
fun LiftHistoryContent(lift: MainLift, state: LiftHistoryState, onBack: () -> Unit) {
    val count = state.sessions.size
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                "${lift.label} history",
                eyebrow = when {
                    state.loading -> null
                    count == 1 -> "1 session"
                    else -> "$count sessions"
                },
                navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            if (!state.loading && state.sessions.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Filled.FitnessCenter,
                        "No ${lift.label.lowercase()} sessions yet",
                        "Every set you log for this lift and its variations will be listed here.",
                    )
                }
            }
            if (state.currentE1rmKg != null || state.best != null) {
                item(key = "summary") { Summary(state) }
            }
            items(state.sessions, key = { it.sessionId }) { session -> SessionCard(session) }
        }
    }
}

@Composable
private fun Summary(state: LiftHistoryState) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        val tile = Modifier.weight(1f).fillMaxHeight()
        if (state.currentE1rmKg != null) {
            StatTile("Current e1RM", formatKg(state.currentE1rmKg.toHalfKg()), tile, unit = "kg", note = "Best of the last 4 weeks")
        }
        if (state.best != null) {
            StatTile("Best e1RM", formatKg(state.best.e1rmKg.toHalfKg()), tile, unit = "kg", note = BEST_DATE.format(state.best.date), noteTone = Tone.SUCCESS)
        }
    }
}

@Composable
private fun SessionCard(session: HistorySession) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    PtCard(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.m, bottom = Space.s), spacing = Space.xs) {
        Text(
            SESSION_DATE.format(session.date),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        session.groups.forEachIndexed { i, group ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = Space.xs), color = pt.hairline)
            Column {
                if (group.isVariation) {
                    Text(group.exerciseName, style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.xs))
                }
                for (row in group.sets) {
                    SetRow(
                        index = row.index,
                        weightKg = row.set.weightKg,
                        reps = row.set.reps,
                        rpe = row.set.rpe,
                        state = if (row.set.failed) SetState.FAILED else SetState.DONE,
                        isTopSet = row.set.isTopSet,
                        isPr = row.isPr,
                    )
                }
            }
        }
    }
}

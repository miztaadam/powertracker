package com.powertracker.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.AdviceRepository
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The coach's open advice, most urgent first. Nothing changes until the lifter taps Apply. */
@Composable
fun CoachScreen(
    training: TrainingRepository,
    advice: AdviceRepository,
    settings: SettingsRepository,
    today: () -> LocalDate = ::systemToday,
) {
    val viewModel: CoachViewModel = viewModel(factory = CoachViewModel.factory(training, advice, settings, today))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            // The newest confirmation replaces the one on screen instead of queueing behind it.
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(message) }
        }
    }
    CoachContent(state, onApply = viewModel::apply, onDismiss = viewModel::dismiss, snackbar = snackbar)
}

/** The Coach screen without its view model. */
@Composable
fun CoachContent(
    state: CoachState,
    onApply: (adviceId: Long) -> Unit,
    onDismiss: (adviceId: Long) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val count = state.cards.size
    val eyebrow = when {
        state.loading -> null
        count == 0 -> "Nothing open"
        count == 1 -> "1 open suggestion"
        else -> "$count open suggestions"
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenHeader("Coach", eyebrow = eyebrow) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            if (!state.loading && state.cards.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Filled.Bolt,
                        "No advice right now. Keep logging.",
                        "The coach looks at your training each time you finish a session.",
                        Modifier.animateItem(),
                    )
                }
            }
            items(state.cards, key = { it.advice.id }) { card ->
                CoachAdviceCard(card, onApply = { onApply(card.advice.id) }, onDismiss = { onDismiss(card.advice.id) }, Modifier.animateItem())
            }
        }
    }
}

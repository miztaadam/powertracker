package com.powertracker.ui.plan.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanDraft
import com.powertracker.domain.plan.PlanTemplate
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.ui.plan.builder.setSideTrainingDays
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

/** The templates screen's two steps: the side's list, then the chosen template's draft. */
enum class TemplateStep { LIST, DRAFT }

data class TemplatesState(
    /** The side the templates are for; the one whose plan and training days a save replaces. */
    val mode: TrainingMode,
    val loading: Boolean = true,
    val step: TemplateStep = TemplateStep.LIST,
    /** [mode]'s templates, fewest days first. */
    val templates: List<PlanTemplate> = PlanTemplates.forMode(mode),
    val chosen: PlanTemplate? = null,
    /** The ticked weekdays; the draft is laid on them once there are exactly [PlanTemplate.dayCount]. */
    val days: Set<DayOfWeek> = emptySet(),
    /**
     * The chosen template's week, with any swaps. Its sessions sit on the last weekdays that matched the template's
     * count: while the ticked count is off, the screen heads them "Day 1", "Day 2", … instead.
     */
    val draft: PlanDraft? = null,
    /** The side's training days in Settings, where the weekday choice starts from. */
    val sideDays: Set<DayOfWeek> = emptySet(),
    /** The `showStrictPress` setting when the screen opened: the powerlifting templates include strict press only with it on. */
    val strictPress: Boolean = false,
    /** The side already has plan items: Use this plan asks first. */
    val hasPlan: Boolean = false,
    val confirming: Boolean = false,
    val saving: Boolean = false,
    /** Stored, plan and days: the screen returns to Plan. */
    val saved: Boolean = false,
    /** The save failed; nothing was stored, the draft stays and Use this plan can be tried again. */
    val failed: Boolean = false,
) {
    /** Exactly the template's number of weekdays is ticked. */
    val daysMatch: Boolean get() = chosen != null && days.size == chosen.dayCount
    val canUse: Boolean get() = step == TemplateStep.DRAFT && draft != null && daysMatch && !saving && !saved

    /** Powerlifting items have fixed reps: the draft shows "3 × 5 @ 8" and saves repsHigh null. */
    val fixedReps: Boolean get() = mode == TrainingMode.POWERLIFTING
}

/**
 * Choosing a plan template (1.14) for [mode]'s side: the list, then a draft of the chosen one ([PlanTemplates.draft]) whose
 * weekdays can be changed and whose exercises can be swapped ([PlanTemplates.alternatives]). Nothing is stored until Use
 * this plan; then the side's whole plan is replaced ([replacePlan], catalogue items by name) and [setDays] sets that
 * side's training days to the chosen weekdays, both or neither (see [store]). A side with a plan is asked first. A test can
 * make either write fail.
 */
class TemplatesViewModel(
    private val mode: TrainingMode,
    private val training: TrainingRepository,
    private val settings: SettingsRepository,
    private val setDays: suspend (TrainingMode, Set<DayOfWeek>) -> Unit = { side, days -> setSideTrainingDays(settings, side, days) },
    private val replacePlan: suspend (TrainingMode, List<Pair<PlanItem, String?>>) -> Unit = { side, items -> training.replacePlan(side, items) },
) : ViewModel() {
    private val _state = MutableStateFlow(TemplatesState(mode))
    val state: StateFlow<TemplatesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update { it.copy(strictPress = s.showStrictPress) }
            combine(training.plan(mode), settings.settings) { plan, st -> plan.isNotEmpty() to st.daysOf(mode) }
                .collect { (hasPlan, days) -> _state.update { it.copy(loading = false, hasPlan = hasPlan, sideDays = days) } }
        }
    }

    private fun com.powertracker.data.AppSettings.daysOf(side: TrainingMode) =
        if (side == TrainingMode.BODYBUILDING) bodybuildingTrainingDays else trainingDays

    /** Opens [id]'s draft on the suggested weekdays: the side's own days when they are as many, else a fixed spread. */
    fun choose(id: String) {
        val s = _state.value
        val template = PlanTemplates.byId(id)?.takeIf { it.mode == mode } ?: return
        if (s.loading || s.saving) return
        val days = PlanTemplates.suggestedDays(template.dayCount, s.sideDays)
        val draft = PlanTemplates.draft(template, days, s.strictPress)
        _state.update { it.copy(step = TemplateStep.DRAFT, chosen = template, days = days.toSet(), draft = draft, confirming = false, failed = false) }
    }

    /**
     * Ticks or unticks [day]. Once exactly the template's count is ticked, the draft's sessions move onto those days in
     * week order, keeping every swap.
     */
    fun toggleDay(day: DayOfWeek) {
        val s = _state.value
        val template = s.chosen ?: return
        if (s.step != TemplateStep.DRAFT || s.saving) return
        val days = if (day in s.days) s.days - day else s.days + day
        val draft = s.draft?.let { d -> if (days.size == template.dayCount) onDays(d, days) else d }
        _state.update { it.copy(days = days, draft = draft, failed = false) }
    }

    /** What [name] on [day] can be swapped for: none for a main lift ([canSwap]), else [PlanTemplates.alternatives]. */
    fun alternatives(day: DayOfWeek, name: String): List<LibraryExercise> {
        val s = _state.value
        val template = s.chosen ?: return emptyList()
        val draft = s.draft ?: return emptyList()
        if (!canSwap(name)) return emptyList()
        return PlanTemplates.alternatives(template, draft, day, name)
    }

    /** [from] on [day] becomes [to], with the same sets, reps and RPE; only to one of [alternatives]. */
    fun swap(day: DayOfWeek, from: String, to: String) {
        val draft = _state.value.draft ?: return
        if (_state.value.saving) return
        if (alternatives(day, from).none { Library.normalise(it.name) == Library.normalise(to) }) return
        val swapped = runCatching { PlanTemplates.swap(draft, day, from, to) }.getOrNull() ?: return
        _state.update { it.copy(draft = swapped) }
    }

    /** Use this plan: asks first when the side has a plan, else saves. Nothing happens unless [TemplatesState.canUse]. */
    fun use() {
        val s = _state.value
        if (!s.canUse) return
        if (s.hasPlan) _state.update { it.copy(confirming = true) } else save()
    }

    fun confirmReplace() {
        if (!_state.value.confirming) return
        _state.update { it.copy(confirming = false) }
        save()
    }

    fun cancelReplace() = _state.update { it.copy(confirming = false) }

    /** From the draft, back to the list (true); from the list, false: back leaves the screen. Nothing is stored. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step == TemplateStep.LIST) return false
        if (s.saving) return true
        _state.update { it.copy(step = TemplateStep.LIST, chosen = null, days = emptySet(), draft = null, confirming = false, failed = false) }
        return true
    }

    private fun save() {
        val s = _state.value
        val draft = s.draft ?: return
        if (!s.canUse) return
        _state.update { it.copy(saving = true, failed = false) }
        // Every template exercise is a catalogue exercise: saved by name, its row resolved in the transaction.
        val items = draft.sessions.flatMap { session ->
            session.exercises.mapIndexed { order, e ->
                PlanItem(0, session.day, 0, e.sets, e.repsLow, e.rpe, order, mode, if (s.fixedReps) null else e.repsHigh) to e.name
            }
        }
        val days = s.days
        viewModelScope.launch {
            try {
                // Not cut short by leaving the screen mid-save: the writes finish, or are undone, together.
                withContext(NonCancellable) { store(items, days) }
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing was changed (see store): the draft stays with the error, and trying again saves both.
                _state.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    /**
     * Replaces the side's plan and sets its days, all or nothing. The days are set last, inside the plan's transaction:
     * a failed days write rolls the plan back, and a failed plan write never reaches the days. If the transaction still
     * fails once the days are stored (a store that wrote and then threw, or a failed commit), the side's days are put
     * back as they were.
     */
    private suspend fun store(items: List<Pair<PlanItem, String?>>, days: Set<DayOfWeek>) {
        val before = settings.settings.first().daysOf(mode)
        try {
            training.inTransaction {
                replacePlan(mode, items)
                setDays(mode, days)
            }
        } catch (e: Exception) {
            if (settings.settings.first().daysOf(mode) != before) runCatching { setDays(mode, before) }
            throw e
        }
    }

    companion object {
        private val MAIN_LIFTS = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT, Category.STRICT_PRESS)

        /** A main lift (squat, bench, deadlift, strict press) has no Swap; everything else in a template has one. */
        fun canSwap(name: String): Boolean = Library.find(name)?.category !in MAIN_LIFTS

        /** [draft]'s sessions, in order, on [days] in week order. The volume is unchanged: each group keeps its day count. */
        fun onDays(draft: PlanDraft, days: Set<DayOfWeek>): PlanDraft {
            val week = days.sortedBy { it.ordinal }
            require(week.size == draft.sessions.size) { "days: ${week.size}; the draft has ${draft.sessions.size}" }
            return draft.copy(sessions = draft.sessions.mapIndexed { i, session -> session.copy(day = week[i]) })
        }

        fun factory(mode: TrainingMode, training: TrainingRepository, settings: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { TemplatesViewModel(mode, training, settings) } }
    }
}

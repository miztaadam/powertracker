package com.powertracker.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AdviceRepository
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.TrainingMode
import com.powertracker.platform.systemToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class CheckInStep { LAGGING, GROWING, SAVED }

/** What the Saved step says about one lagging body part. */
data class SavedLine(val group: MuscleGroup, val text: String)

data class CheckInState(
    val loading: Boolean = true,
    /** Only the bodybuilding side has check-ins. */
    val available: Boolean = false,
    val step: CheckInStep = CheckInStep.LAGGING,
    val lagging: Set<MuscleGroup> = emptySet(),
    val growingWell: Set<MuscleGroup> = emptySet(),
    val saving: Boolean = false,
    /** After Save: one line per lagging group, in [MuscleGroup] order. */
    val lines: List<SavedLine> = emptyList(),
    /** After Save: the coach made at least one suggestion, so the Saved step offers Coach. */
    val hasAdvice: Boolean = false,
    /** Save failed; nothing was stored. */
    val failed: Boolean = false,
)

/**
 * A bodybuilding check-in (stage 3): the lifter marks the lagging body parts, then the ones growing well; every other
 * one stays on track. Nothing is stored until Save, which stores the check-in, puts the earlier open more-work
 * suggestions away and stores the coach's new ones, all in one transaction.
 */
class CheckInViewModel(
    private val training: TrainingRepository,
    private val advice: AdviceRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {
    private val local = MutableStateFlow(CheckInState(loading = false))

    val state: StateFlow<CheckInState> = combine(local, settings.settings) { l, s ->
        // Once saved, a switch of side does not take the answer away.
        l.copy(available = s.trainingMode == TrainingMode.BODYBUILDING || l.step == CheckInStep.SAVED)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckInState())

    /** A group marked lagging cannot also be growing well. */
    fun toggleLagging(group: MuscleGroup) = local.update {
        if (it.step != CheckInStep.LAGGING) return@update it
        if (group in it.lagging) it.copy(lagging = it.lagging - group) else it.copy(lagging = it.lagging + group, growingWell = it.growingWell - group)
    }

    fun toggleGrowing(group: MuscleGroup) = local.update {
        if (it.step != CheckInStep.GROWING || group in it.lagging) return@update it
        if (group in it.growingWell) it.copy(growingWell = it.growingWell - group) else it.copy(growingWell = it.growingWell + group)
    }

    fun next() = local.update { if (it.step == CheckInStep.LAGGING) it.copy(step = CheckInStep.GROWING) else it }

    /** A step back; false on the first step (and once saved), where Back leaves the screen. */
    fun back(): Boolean {
        if (local.value.step != CheckInStep.GROWING || local.value.saving) return false
        local.update { it.copy(step = CheckInStep.LAGGING) }
        return true
    }

    fun save() {
        val current = local.value
        if (current.step != CheckInStep.GROWING || current.saving) return
        local.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                val date = today()
                val checkIn = CheckIn(date, current.lagging, current.growingWell - current.lagging)
                val outcome = Coaching.afterCheckIn(
                    checkIn, training.history().first(), training.plan(TrainingMode.BODYBUILDING).first(), date, advice.all(),
                )
                training.inTransaction {
                    training.saveCheckIn(checkIn)
                    for (old in outcome.dismissed) advice.setStatus(old.id, AdviceStatus.DISMISSED)
                    if (outcome.advice.isNotEmpty()) advice.insert(outcome.advice)
                }
                val suggested = outcome.advice.mapNotNull { (it.payload as? AdvicePayload.MoreWork)?.group }.toSet()
                val notes = outcome.notes.associateBy { it.group }
                val lines = MuscleGroup.entries.filter { it in checkIn.lagging }.mapNotNull { group ->
                    when {
                        group in suggested -> SavedLine(group, MORE_WORK_SUGGESTED)
                        else -> notes[group]?.let { SavedLine(group, noteText(it)) }
                    }
                }
                local.update { it.copy(step = CheckInStep.SAVED, saving = false, lines = lines, hasAdvice = outcome.advice.isNotEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction left everything as it was: the lifter can try again.
                local.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    companion object {
        fun factory(
            training: TrainingRepository,
            advice: AdviceRepository,
            settings: SettingsRepository,
            today: () -> LocalDate = ::systemToday,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { CheckInViewModel(training, advice, settings, today) } }
    }
}

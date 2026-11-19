package com.powertracker.ui.plan

import com.powertracker.data.PlanSnapshot
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.plan.builder.setSideTrainingDays
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek

// Undo after a whole-plan replace (1.14.1). The web app says the same.

/** The builder's snackbar after a save; a template's is [com.powertracker.ui.plan.templates.planSetText]. */
const val PLAN_SAVED = "Plan saved."

/** Said when an Undo fails: nothing was changed. */
const val UNDO_FAILED = "Couldn't undo. Your new plan is kept."

private fun daysOf(s: com.powertracker.data.AppSettings, mode: TrainingMode) =
    if (mode == TrainingMode.BODYBUILDING) s.bodybuildingTrainingDays else s.trainingDays

/**
 * Puts [snapshot]'s plan back on its side: the items with their ids, the skip links and the training days, all or nothing
 * ([TrainingRepository.restorePlan]). The days are set last, inside the plan's transaction, as the saves set them; if the
 * transaction still fails once they are stored, they are put back as they were. A stale Undo (the side's plan has changed
 * since the save, a later save took a snapshot, or it was already undone) changes nothing and returns false. Throws if
 * the undo failed, and can then be tried again.
 */
suspend fun undoPlanReplace(
    training: TrainingRepository,
    settings: SettingsRepository,
    snapshot: PlanSnapshot,
    setDays: suspend (TrainingMode, Set<DayOfWeek>) -> Unit = { side, days -> setSideTrainingDays(settings, side, days) },
    restorePlan: suspend (TrainingMode, PlanSnapshot) -> Boolean = training::restorePlan,
): Boolean {
    val mode = snapshot.mode
    val before = daysOf(settings.settings.first(), mode)
    try {
        val restored = training.inTransaction {
            restorePlan(mode, snapshot).also { restored -> if (restored) setDays(mode, snapshot.days) }
        }
        // Committed: Undo after Undo does nothing.
        if (restored) training.undone(snapshot)
        return restored
    } catch (e: Exception) {
        if (daysOf(settings.settings.first(), mode) != before) runCatching { setDays(mode, before) }
        throw e
    }
}

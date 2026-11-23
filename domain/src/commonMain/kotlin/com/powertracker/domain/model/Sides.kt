package com.powertracker.domain.model

/** The same exercises and baselines, with only the sessions logged on [mode]. */
fun TrainingHistory.on(mode: TrainingMode): TrainingHistory = copy(sessions = sessions.filter { it.mode == mode })

/** The plan items of [mode]. */
fun List<PlanItem>.on(mode: TrainingMode): List<PlanItem> = filter { it.mode == mode }

/**
 * The advice the [mode] side shows and counts: every deload and easing-back-in, whichever side it was made on,
 * because those belong to the lifter; the rest only where its side is [mode].
 */
fun List<Advice>.forSide(mode: TrainingMode): List<Advice> =
    filter { it.type == AdviceType.DELOAD || it.type == AdviceType.RE_ENTRY || it.mode == mode }

/** Reps as the apps print them: "5" for fixed reps, "8–12" (en dash) for a range. */
fun repsText(reps: Int, repsHigh: Int?): String = if (repsHigh == null) "$reps" else "$reps–$repsHigh"

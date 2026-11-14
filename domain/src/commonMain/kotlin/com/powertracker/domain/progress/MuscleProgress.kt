package com.powertracker.domain.progress

import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.MuscleRating
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.plan.PlanBuilder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * One body part on the bodybuilding Progress screen: its [ratings] at its last four check-ins (oldest first, with
 * dates), the counted sets done in the 7 days ending today ([setsDone]), its weekly sets in the plan ([setsPlanned]),
 * and of the plan's exercises whose main group it is, how many are going up ([exercisesUp]) out of those with enough
 * sessions to tell ([exercisesTracked]).
 */
data class MuscleRow(
    val group: MuscleGroup,
    val ratings: List<Pair<LocalDate, MuscleRating>>,
    val setsDone: Int,
    val setsPlanned: Int,
    val exercisesUp: Int,
    val exercisesTracked: Int,
)

/** The bodybuilding side's body-part Progress rows (stage 3 plan, "Progress rows"). */
object MuscleProgress {
    private const val CHECK_INS = 4
    private const val DAYS = 7

    /**
     * One row per group, in [MuscleGroup] order, for every group with weekly sets in the bodybuilding plan, or counted
     * sets done (bodybuilding sessions, missed sets left out) in the 7 days ending [today], or a rating other than on
     * track at the latest check-in on or before [today].
     */
    fun rows(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, today: LocalDate): List<MuscleRow> {
        val items = plan.on(TrainingMode.BODYBUILDING)
        val recent = checkIns.filter { it.date <= today }.sortedBy { it.date }.takeLast(CHECK_INS)
        val latest = recent.lastOrNull()
        val from = today.minus(DAYS - 1, DateTimeUnit.DAY)
        val doneSets = history.sessions
            .filter { it.mode == TrainingMode.BODYBUILDING && it.date in from..today }
            .flatMap { s -> s.sets.filter { !it.failed } }
        val trends = BodybuildingProgress.rows(history, items, today)
        return MuscleGroup.entries.mapNotNull { group ->
            val planned = PhysiqueCoach.weeklySets(items, history.exercises, group)
            val done = doneSets.count { set -> history.exercises[set.exerciseId]?.let { MuscleGroups.works(it.muscles, group) } == true }
            val rated = latest != null && latest.rating(group) != MuscleRating.ON_TRACK
            if (planned == 0 && done == 0 && !rated) return@mapNotNull null
            val own = trends.filter { t -> history.exercises[t.exerciseId]?.let { PlanBuilder.mainGroup(it.muscles) } == group && t.goingUp != null }
            MuscleRow(
                group, recent.map { it.date to it.rating(group) }, done, planned,
                own.count { it.goingUp == true }, own.size,
            )
        }
    }
}

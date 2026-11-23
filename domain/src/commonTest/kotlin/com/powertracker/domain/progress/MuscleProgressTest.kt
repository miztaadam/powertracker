package com.powertracker.domain.progress

import com.powertracker.domain.coach.PhysiqueFixtures
import com.powertracker.domain.coach.PhysiqueFixtures.idOf
import com.powertracker.domain.coach.PhysiqueFixtures.item
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MuscleRating
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

/** The bodybuilding side's body-part Progress rows (stage 3 plan, "Progress rows"). */
class MuscleProgressTest {
    private val today = LocalDate(2026, 11, 14)
    private fun ago(days: Int) = today.minus(days, DateTimeUnit.DAY)

    private fun set(name: String, kg: Double, reps: Int, failed: Boolean = false) = LoggedSet(idOf(name), kg, reps, 8.0, failed, null, false)

    private fun session(daysAgo: Int, vararg sets: LoggedSet, mode: TrainingMode = TrainingMode.BODYBUILDING) =
        LoggedSession(100L - daysAgo, ago(daysAgo), null, null, sets.toList(), mode)

    // Monday: Cable crossover (pecs, front delts) 3 sets, Lateral raise (side delts) 3 sets.
    private val plan = listOf(item(1, MONDAY, "Cable crossover", 3, 0), item(2, MONDAY, "Lateral raise", 3, 1))

    @Test
    fun aRowForEveryGroupPlannedDoneOrRatedInGroupOrder() {
        // Planned: chest, front delts, side delts. Done only: traps (a shrug not in the plan). Rated only: obliques.
        val h = PhysiqueFixtures.history(listOf(session(2, set("Barbell shrug", 60.0, 10))))
        val checkIns = listOf(CheckIn(ago(1), setOf(MuscleGroup.OBLIQUES), emptySet()))
        val rows = MuscleProgress.rows(h, plan, checkIns, today)
        assertEquals(
            listOf(MuscleGroup.CHEST, MuscleGroup.TRAPS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.OBLIQUES),
            rows.map { it.group },
        )
        assertEquals(MuscleRow(MuscleGroup.TRAPS, listOf(ago(1) to MuscleRating.ON_TRACK), 1, 0, 0, 0), rows[1])
        assertEquals(MuscleRow(MuscleGroup.OBLIQUES, listOf(ago(1) to MuscleRating.LAGGING), 0, 0, 0, 0), rows[4])
        assertEquals(3, rows[0].setsPlanned)
        // A group rated on track at an earlier check-in only, with nothing planned or done, has no row.
        val older = listOf(CheckIn(ago(40), setOf(MuscleGroup.OBLIQUES), emptySet()), CheckIn(ago(1), emptySet(), emptySet()))
        assertEquals(listOf(MuscleGroup.CHEST, MuscleGroup.TRAPS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS), MuscleProgress.rows(h, plan, older, today).map { it.group })
    }

    @Test
    fun ratingsAreTheLastFourCheckInsOldestFirst() {
        val checkIns = listOf(
            CheckIn(ago(10), setOf(MuscleGroup.CHEST), emptySet()),
            CheckIn(ago(130), emptySet(), setOf(MuscleGroup.CHEST)),
            CheckIn(ago(40), emptySet(), setOf(MuscleGroup.CHEST)),
            CheckIn(ago(100), setOf(MuscleGroup.CHEST), emptySet()),
            CheckIn(ago(70), emptySet(), emptySet()),
        )
        val chest = MuscleProgress.rows(PhysiqueFixtures.history(), plan, checkIns, today).first { it.group == MuscleGroup.CHEST }
        assertEquals(
            listOf(ago(100) to MuscleRating.LAGGING, ago(70) to MuscleRating.ON_TRACK, ago(40) to MuscleRating.GROWING_WELL, ago(10) to MuscleRating.LAGGING),
            chest.ratings,
        )
    }

    @Test
    fun setsDoneAreCountedPerGroupOverTheSevenDaysEndingTodayWithoutMissedSets() {
        val h = PhysiqueFixtures.history(
            listOf(
                session(7, set("Cable crossover", 20.0, 10)), // eight days ago counting today: out
                session(6, set("Cable crossover", 20.0, 10), set("Cable crossover", 20.0, 9)),
                session(0, set("Cable crossover", 20.0, 10), set("Cable crossover", 20.0, 6, failed = true), set("Lateral raise", 10.0, 12)),
                session(3, set("Cable crossover", 20.0, 10), mode = TrainingMode.POWERLIFTING),
            ),
        )
        val rows = MuscleProgress.rows(h, plan, emptyList(), today).associateBy { it.group }
        assertEquals(3, rows.getValue(MuscleGroup.CHEST).setsDone)
        assertEquals(3, rows.getValue(MuscleGroup.FRONT_DELTS).setsDone)
        assertEquals(1, rows.getValue(MuscleGroup.SIDE_DELTS).setsDone)
        assertEquals(emptyList(), rows.getValue(MuscleGroup.CHEST).ratings)
    }

    @Test
    fun goingUpCountsThePlansExercisesOfThatMainGroup() {
        val plan = plan + item(3, MONDAY, "Pec deck", 3, 2) + item(4, MONDAY, "DB fly", 3, 3)
        val h = PhysiqueFixtures.history(
            listOf(
                session(9, set("Cable crossover", 20.0, 10), set("Pec deck", 50.0, 10), set("Lateral raise", 10.0, 12)),
                session(2, set("Cable crossover", 22.5, 8), set("Pec deck", 50.0, 9), set("Lateral raise", 10.0, 13), set("DB fly", 14.0, 10)),
            ),
        )
        val rows = MuscleProgress.rows(h, plan, emptyList(), today).associateBy { it.group }
        // Chest: crossover up, pec deck not, DB fly too new to tell.
        assertEquals(1, rows.getValue(MuscleGroup.CHEST).exercisesUp)
        assertEquals(2, rows.getValue(MuscleGroup.CHEST).exercisesTracked)
        // Front delts are no exercise's main group here.
        assertEquals(0, rows.getValue(MuscleGroup.FRONT_DELTS).exercisesTracked)
        assertEquals(1, rows.getValue(MuscleGroup.SIDE_DELTS).exercisesUp)
        assertEquals(1, rows.getValue(MuscleGroup.SIDE_DELTS).exercisesTracked)
    }
}

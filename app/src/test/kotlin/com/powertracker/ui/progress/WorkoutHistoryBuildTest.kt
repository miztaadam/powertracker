package com.powertracker.ui.progress

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The History list (1.20), the same list as the web app's `buildWorkoutHistory`. */
class WorkoutHistoryBuildTest {
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val curl = Exercise(2, "Curl", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)
    // Archived: its sets stay in the log under its name.
    private val pulldown = Exercise(3, "Lat pulldown", Category.ACCESSORY, null, emptyList(), isBuiltIn = true, archived = true)
    private val exercises = listOf(squat, curl, pulldown).associateBy { it.id }

    private fun set(e: Exercise, kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, unit: LoadUnit = LoadUnit.KG) =
        LoggedSet(e.id, kg, reps, rpe, failed, null, isTopSet = false, unit = unit)

    private fun session(id: Long, date: String, sets: List<LoggedSet>, mode: TrainingMode = POWERLIFTING, bodyweightKg: Double? = null) =
        LoggedSession(id, LocalDate.parse(date), bodyweightKg, null, sets, mode)

    private fun build(vararg sessions: LoggedSession) = WorkoutHistoryViewModel.build(TrainingHistory(sessions.toList(), exercises, emptyList()))

    @Test
    fun workouts_newestFirst_fromBothSides_withTheirSideCountsAndBodyweight() {
        val state = build(
            session(1, "2026-11-02", listOf(set(squat, 140.0, 5))),
            session(3, "2026-11-09", listOf(set(squat, 145.0, 5), set(squat, 145.0, 4)), POWERLIFTING, 82.5),
            session(2, "2026-11-04", listOf(set(curl, 20.0, 12), set(squat, 100.0, 10)), BODYBUILDING),
        )
        assertFalse(state.loading)
        assertEquals(listOf(3L, 2L, 1L), state.workouts.map { it.sessionId })
        assertEquals(
            listOf("Powerlifting · 1 exercise · 2 sets · 82.5 kg bodyweight", "Bodybuilding · 2 exercises · 2 sets", "Powerlifting · 1 exercise · 1 set"),
            state.workouts.map { it.summary },
        )
        assertEquals(listOf("Curl", "Squat"), state.workouts[1].exercises.map { it.name })
    }

    @Test
    fun aSessionWithNoSets_isNoWorkout() {
        val state = build(session(1, "2026-11-02", emptyList()), session(2, "2026-11-04", listOf(set(squat, 140.0, 5))), session(3, "2026-11-09", emptyList()))
        assertEquals(listOf(2L), state.workouts.map { it.sessionId })
        assertEquals(emptyList<Workout>(), build().workouts)
    }

    @Test
    fun anExerciseComeBackTo_isOneGroup_inTheOrderFirstLogged_withItsSetsInOrder() {
        val state = build(session(1, "2026-11-02", listOf(set(squat, 100.0, 5), set(curl, 20.0, 12), set(squat, 120.0, 5), set(curl, 20.0, 11), set(squat, 140.0, 3))))
        val workout = state.workouts.single()
        assertEquals(listOf("Squat" to listOf(100.0, 120.0, 140.0), "Curl" to listOf(20.0, 20.0)), workout.exercises.map { e -> e.name to e.sets.map { it.weightKg } })
        assertEquals(5, workout.setCount)
    }

    @Test
    fun everyKindOfSet_isCarriedAsLogged_andAnArchivedExerciseKeepsItsName() {
        val sets = listOf(set(pulldown, 60.0, 12), set(pulldown, 7.5, 10, 9.0, unit = LoadUnit.PLATES), set(pulldown, 8.0, 6, rpe = null), set(pulldown, 8.0, 4, 10.0, failed = true, unit = LoadUnit.PLATES))
        val state = build(session(1, "2026-11-02", sets, BODYBUILDING))
        assertEquals(listOf(WorkoutExercise(3, "Lat pulldown", sets)), state.workouts.single().exercises)
    }

    @Test
    fun aSetOfAnExerciseWithNoRow_stillShows() {
        val state = build(session(1, "2026-11-02", listOf(LoggedSet(99, 50.0, 5, 8.0, false, null, isTopSet = false))))
        assertEquals(listOf(Triple(99L, "Exercise", 1)), state.workouts.single().exercises.map { Triple(it.exerciseId, it.name, it.sets.size) })
    }

    @Test
    fun twoSessionsOnOneDay_theLaterOneFirst() {
        val state = build(session(1, "2026-11-02", listOf(set(squat, 100.0, 5))), session(2, "2026-11-02", listOf(set(curl, 20.0, 12))))
        assertEquals(listOf(2L, 1L), state.workouts.map { it.sessionId })
    }
}

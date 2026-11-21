package com.powertracker.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.progress.ExerciseHistoryContent
import com.powertracker.ui.progress.ExerciseHistoryViewModel
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plate sets on Progress (bodybuilding rows) and in an exercise's history (1.17, Task 7). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class PlatesProgressTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11) // a Wednesday

    private val lat = Exercise(1, "Lat pulldown", Category.ACCESSORY, null, emptyList(), isBuiltIn = true, plateCount = PlateCount.WHOLE)
    private val curl = Exercise(2, "DB curl", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)

    private fun set(exercise: Exercise, weight: Double, reps: Int, unit: LoadUnit = LoadUnit.KG) =
        LoggedSet(exercise.id, weight, reps, 8.0, failed = false, missPoint = null, isTopSet = false, unit = unit)

    private fun session(id: Long, daysAgo: Int, vararg sets: LoggedSet, mode: TrainingMode = TrainingMode.BODYBUILDING) =
        LoggedSession(id, today.minus(daysAgo, DateTimeUnit.DAY), null, null, sets.toList(), mode = mode)

    private fun exists(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun bodybuildingProgress_printsPlate9Times10() {
        val history = TrainingHistory(
            sessions = listOf(
                session(1, 14, set(lat, 8.0, 10, LoadUnit.PLATES), set(curl, 12.0, 10)),
                session(2, 7, set(lat, 9.0, 10, LoadUnit.PLATES), set(curl, 14.0, 9)),
            ),
            exercises = mapOf(1L to lat, 2L to curl),
            baselines = emptyList(),
        )
        val plan = listOf(
            PlanItem(1, DayOfWeek.MONDAY, 1, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
            PlanItem(2, DayOfWeek.MONDAY, 2, 3, 8, 8.0, 1, TrainingMode.BODYBUILDING, 12),
        )
        val state = ProgressViewModel.buildBodybuilding(history, plan, today)
        compose.setContent { PowertrackerTheme { ProgressContent(state, onRange = {}, onOpenLift = {}) } }
        compose.waitForIdle()

        assertTrue(exists("plate 8 × 10 · plate 9 × 10"))
        assertTrue("the kg row is as before", exists("12 × 10 · 14 × 9"))
        assertTrue(compose.onAllNodes(hasContentDescription("Lat pulldown. plate 8 × 10, plate 9 × 10. Going up")).fetchSemanticsNodes().isNotEmpty())    }

    @Test
    fun exerciseHistory_printsEachSetInItsOwnUnit() {
        val history = TrainingHistory(
            sessions = listOf(
                session(1, 14, set(lat, 60.0, 10), mode = TrainingMode.POWERLIFTING),
                session(2, 7, set(lat, 7.0, 10, LoadUnit.PLATES)),
            ),
            exercises = mapOf(1L to lat),
            baselines = emptyList(),
        )
        val state = ExerciseHistoryViewModel.build(history, lat.id)
        assertTrue("no record", state.sessions.flatMap { s -> s.groups.flatMap { it.sets } }.none { it.isPr })
        compose.setContent { PowertrackerTheme { ExerciseHistoryContent(state, onBack = {}) } }
        compose.waitForIdle()

        val rows = compose.onAllNodes(hasText("Set 1:", substring = true)).fetchSemanticsNodes()
            .map { it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString() }
        // Newest first: the plate session, then the kg one.
        assertEquals(listOf("Set 1: plate 7 × 10 at RPE 8, done", "Set 1: 60 kg × 10 @ 8, done"), rows)
    }
}

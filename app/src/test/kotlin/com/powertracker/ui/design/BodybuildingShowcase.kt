package com.powertracker.ui.design

import androidx.compose.runtime.Composable
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

// The bodybuilding side's mockups (bodybuilding mode spec §7.3 to §7.7), as the owner's option C: the same lifter on
// Wednesday 11 November, a physique day of 3 × 8–12 @ 8.

private fun bbExercise(id: Long, name: String, category: Category = Category.ACCESSORY) = Exercise(id, name, category, null, emptyList(), isBuiltIn = true)

private fun bbItem(id: Long, day: DayOfWeek, order: Int, sets: Int = 3, low: Int = 8, high: Int = 12) =
    PlanItem(id, day, id, sets, low, 8.0, order, TrainingMode.BODYBUILDING, high)

private fun bbLogged(id: Long, exerciseId: Long, kg: Double, reps: Int) = StoredSet(id, 1, LoggedSet(exerciseId, kg, reps, 8.0, failed = false, missPoint = null, isTopSet = false))

private fun bbToday(id: Long, name: String, weightKg: Double, done: List<StoredSet> = emptyList(), suggestion: Advice? = null) = TodayItem(
    exercise = bbExercise(id, name),
    planned = bbItem(id, DayOfWeek.WEDNESDAY, id.toInt()),
    suggestion = suggestion,
    sets = done,
    editor = SetEditor(weightKg, 10, 8.0),
    targetSets = 3,
    perSide = null,
    lastWeightKg = weightKg,
)

/** Today mid-session, as the mockup: the incline press's third set, the coach's range target, two sets logged. */
@Composable
fun TodayBodybuildingShowcase() {
    val target = Advice(
        AdviceType.TARGET, null, SHOWCASE_DATE, "Last time: 26 × 11, 10, 9. Beat it. Coach suggests 26 × 8–12 @ 8.", 0,
        AdvicePayload.Target(1, 26.0, 8, 8.0, repsHigh = 12), id = 1, mode = TrainingMode.BODYBUILDING,
    )
    val items = listOf(
        bbToday(1, "Incline DB press", 26.0, listOf(bbLogged(1, 1, 26.0, 11), bbLogged(2, 1, 26.0, 10)), target),
        bbToday(2, "Lat pulldown", 60.0),
        bbToday(3, "Cable crossover", 15.0),
        bbToday(4, "Leg press", 140.0),
        bbToday(5, "Lateral raise", 10.0),
    )
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = items, mode = TrainingMode.BODYBUILDING), TodayActions())
}

private fun bbDay(day: DayOfWeek, vararg names: String) = DaySection(
    day,
    names.mapIndexed { i, name -> PlanRow(bbItem(day.isoDayNumber * 10L + i, day, i, high = if (name == "Lateral raise") 15 else 12), name) },
)

/** The bodybuilding side's own plan: three full-body days of 3 × 8–12 @ 8. */
@Composable
fun PlanBodybuildingShowcase() {
    val state = PlanUiState(
        sections = listOf(
            bbDay(DayOfWeek.MONDAY, "Incline DB press", "Lat pulldown", "Leg press"),
            bbDay(DayOfWeek.WEDNESDAY, "Incline DB press", "Lat pulldown", "Cable crossover", "Leg press", "Lateral raise"),
            bbDay(DayOfWeek.FRIDAY, "Bench", "Seated cable row", "Lateral raise"),
        ),
        mainExercises = listOf(bbExercise(1, "Squat", Category.SQUAT), bbExercise(2, "Bench", Category.BENCH), bbExercise(3, "Deadlift", Category.DEADLIFT)),
        mode = TrainingMode.BODYBUILDING,
    )
    PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {})
}

/** Back on the bodybuilding side after more than a month: the welcome-back card, then a readiness deload. */
@Composable
fun CoachBodybuildingShowcase() {
    val deload = Advice(
        AdviceType.DELOAD, null, SHOWCASE_DATE,
        "Signs of fatigue: low readiness. Take a deload week: about 60% of the volume at about 90% of the load.", 1,
        AdvicePayload.Deload(), id = 1, mode = TrainingMode.BODYBUILDING,
    )
    CoachContent(
        CoachState(loading = false, cards = listOf(CoachCard(deload, CardDetail.Deload(volumePct = 60, loadPct = 90))), welcomeBack = true),
        onApply = {}, onDismiss = {},
    )
}

/** Progress on the bodybuilding side: each planned exercise's best set from its last four sessions, and its trend. */
@Composable
fun ProgressBodybuildingShowcase() {
    fun best(daysAgo: Int, kg: Double, reps: Int) = SHOWCASE_DATE.minus(daysAgo, DateTimeUnit.DAY) to LoggedSet(0, kg, reps, 8.0, false, null, false)
    val trends = listOf(
        ExerciseTrend(1, "Incline DB press", listOf(best(21, 24.0, 12), best(14, 26.0, 9), best(7, 26.0, 10), best(0, 26.0, 11)), goingUp = true),
        ExerciseTrend(2, "Lat pulldown", listOf(best(21, 55.0, 11), best(14, 55.0, 12), best(7, 60.0, 9), best(0, 60.0, 9)), goingUp = false),
        ExerciseTrend(3, "Cable crossover", listOf(best(14, 12.5, 12), best(7, 15.0, 8), best(0, 15.0, 10)), goingUp = true),
        ExerciseTrend(4, "Leg press", listOf(best(7, 140.0, 10), best(0, 140.0, 12)), goingUp = true),
        ExerciseTrend(5, "Lateral raise", listOf(best(0, 10.0, 13)), goingUp = null),
        ExerciseTrend(6, "Bench", emptyList(), goingUp = null),
        ExerciseTrend(7, "Seated cable row", emptyList(), goingUp = null),
    )
    ProgressContent(
        ProgressState(loading = false, hasData = true, mode = TrainingMode.BODYBUILDING, trends = trends, trendLifts = mapOf(6L to MainLift.BENCH)),
        onRange = {}, onOpenLift = {},
    )
}

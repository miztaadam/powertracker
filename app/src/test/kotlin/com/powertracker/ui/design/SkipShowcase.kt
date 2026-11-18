package com.powertracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.data.StoredSet
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.SkipMenuItem
import com.powertracker.ui.today.SkipReasonContent
import com.powertracker.ui.today.SkippedToday
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import kotlinx.datetime.DayOfWeek

// Skipping a planned exercise (1.13): Today's row menu, the reason sheet and the "Skipped today" line, and the coach's
// SKIP cards, on both sides. The menu and the sheet are their own windows, which a capture cannot see: their real
// content is drawn in stand-ins over the screen.

private fun skipItem(id: Long, name: String, category: Category, mode: TrainingMode, weightKg: Double, done: List<StoredSet> = emptyList()): TodayItem {
    val bodybuilding = mode == TrainingMode.BODYBUILDING
    val plan = PlanItem(id, DayOfWeek.WEDNESDAY, id, 3, if (bodybuilding) 8 else 5, 8.0, id.toInt(), mode, if (bodybuilding) 12 else null)
    return TodayItem(
        exercise = Exercise(id, name, category, null, emptyList(), isBuiltIn = true),
        planned = plan,
        suggestion = null,
        sets = done,
        editor = SetEditor(weightKg, plan.reps, 8.0),
        targetSets = 3,
        perSide = null,
        lastWeightKg = weightKg,
    )
}

private fun done(id: Long, exerciseId: Long, kg: Double, reps: Int) = StoredSet(id, 1, LoggedSet(exerciseId, kg, reps, 8.0, failed = false, missPoint = null, isTopSet = false))

/** The session on [mode]'s side: the first exercise under way, the rest still to do. */
private fun skipItems(mode: TrainingMode): List<TodayItem> = if (mode == TrainingMode.BODYBUILDING) {
    listOf(
        skipItem(1, "Incline DB press", Category.ACCESSORY, mode, 26.0, listOf(done(1, 1, 26.0, 11))),
        skipItem(2, "Lat pulldown", Category.ACCESSORY, mode, 60.0),
        skipItem(3, "Cable crossover", Category.ACCESSORY, mode, 15.0),
        skipItem(4, "Leg press", Category.ACCESSORY, mode, 140.0),
    )
} else {
    listOf(
        skipItem(1, "Squat", Category.SQUAT, mode, 155.0, listOf(done(1, 1, 150.0, 5))),
        skipItem(2, "Bench", Category.BENCH, mode, 100.0),
        skipItem(3, "Barbell row", Category.ACCESSORY, mode, 80.0),
        skipItem(4, "Triceps pushdown", Category.ACCESSORY, mode, 30.0),
    )
}

@Composable
private fun SkipToday(mode: TrainingMode, items: List<TodayItem> = skipItems(mode), skipped: List<SkippedToday> = emptyList()) {
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = items, mode = mode, skippedToday = skipped), TodayActions())
}

/** The row menu open on the second exercise: its one item, Skip today. */
@Composable
fun TodaySkipMenuShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    Box(Modifier.fillMaxSize()) {
        SkipToday(mode)
        // Where the menu drops from the second row's ⋮.
        Surface(
            Modifier.align(Alignment.TopEnd).padding(top = 588.dp, end = 24.dp).width(200.dp),
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(Modifier.padding(vertical = 8.dp)) { SkipMenuItem {} }
        }
    }
}

/** The reason sheet, over Today, for the second exercise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaySkipSheetShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    val scheme = MaterialTheme.colorScheme
    val name = skipItems(mode)[1].exercise.name
    Box(Modifier.fillMaxSize()) {
        SkipToday(mode)
        Box(Modifier.fillMaxSize().background(scheme.scrim.copy(alpha = 0.32f)))
        Surface(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = scheme.surface,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle(color = scheme.outline)
                SkipReasonContent(name, onReason = {})
            }
        }
    }
}

/** Two exercises skipped: they have left the list, and the quiet line at its end offers Bring back. */
@Composable
fun TodaySkippedShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    val all = skipItems(mode)
    val left = listOf(all[0], all[3])
    SkipToday(mode, left, skipped = listOf(SkippedToday(all[1].exercise.id, all[1].exercise.name, 1), SkippedToday(all[2].exercise.id, all[2].exercise.name, 2)))
}

/** Coach with a SKIP card that offers swaps (pain, an accessory) and a main lift's pain card, Dismiss only. */
@Composable
fun CoachSkipShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    val bodybuilding = mode == TrainingMode.BODYBUILDING
    val accessory = if (bodybuilding) "Lateral raise" else "Barbell row"
    val alternatives = if (bodybuilding) listOf("Cable lateral raise", "Machine lateral raise", "Upright row") else listOf("Chest-supported row", "Seated cable row", "One-arm DB row")
    val swap = AdvicePayload.Skipped(1, 3, accessory, SkipReason.PAIN, alternatives)
    val hold = AdvicePayload.Skipped(2, 2, if (bodybuilding) "Bench" else "Squat", SkipReason.PAIN, emptyList())
    val swapCard = Advice(
        AdviceType.SKIP, null, SHOWCASE_DATE,
        "You skipped $accessory because of pain. Try one of these instead. Its weight won't go up until you do $accessory again. If the pain persists or gets worse, see a physician.",
        3, swap, id = 1, mode = mode,
    )
    val holdCard = Advice(
        AdviceType.SKIP, if (bodybuilding) MainLift.BENCH else MainLift.SQUAT, SHOWCASE_DATE,
        "You skipped ${hold.name} because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician.",
        3, hold, id = 2, mode = mode,
    )
    CoachContent(
        CoachState(loading = false, cards = listOf(CoachCard(swapCard, CardDetail.Skip(swap)), CoachCard(holdCard, CardDetail.Skip(hold)))),
        onApply = {}, onDismiss = {},
    )
}

package com.powertracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MenuDefaults
import com.powertracker.ui.today.PlateMenuItem
import com.powertracker.ui.today.SkipMenuItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.today.PlateCountContent
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import com.powertracker.ui.today.plateNote
import kotlinx.datetime.DayOfWeek

// Counting plates on a stack machine (1.17): Today with a plate-counted Lat pulldown, as the design's sketch has it (the
// coach's chip, one plate set logged, the PLATE stepper), and the plate sheet. The sheet is its own window, which a
// capture cannot see: its real content is drawn in a stand-in over the screen.

private fun platesItem(id: Long, name: String, mode: TrainingMode, weight: Double?, done: List<StoredSet> = emptyList(), suggestion: Advice? = null, count: PlateCount = PlateCount.OFF): TodayItem {
    val bodybuilding = mode == TrainingMode.BODYBUILDING
    val plan = PlanItem(id, DayOfWeek.WEDNESDAY, id, 3, 8, 8.0, id.toInt(), mode, if (bodybuilding) 12 else null)
    val unit = if (count == PlateCount.OFF) LoadUnit.KG else LoadUnit.PLATES
    return TodayItem(
        exercise = Exercise(id, name, Category.ACCESSORY, null, emptyList(), isBuiltIn = true, plateCount = count),
        planned = plan,
        suggestion = suggestion,
        sets = done,
        editor = SetEditor(weight, if (bodybuilding) 12 else 9, 8.0, unit),
        targetSets = 3,
        perSide = null,
        lastWeightKg = weight,
        unit = unit,
        plateCount = count,
        plateOffered = true,
    ).withWarmUp()
}

private fun plateSet(id: Long, exerciseId: Long, plate: Double, reps: Int) =
    StoredSet(id, 1, LoggedSet(exerciseId, plate, reps, 8.0, failed = false, missPoint = null, isTopSet = false, unit = LoadUnit.PLATES))

private fun platesItems(mode: TrainingMode): List<TodayItem> {
    val bodybuilding = mode == TrainingMode.BODYBUILDING
    val target = if (bodybuilding) {
        AdvicePayload.Target(1, 7.0, 8, 8.0, repsHigh = 12, unit = LoadUnit.PLATES)
    } else {
        AdvicePayload.Target(1, 7.0, 9, 8.0, unit = LoadUnit.PLATES)
    }
    val reason = if (bodybuilding) {
        "Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 7 × 8–12 @ 8."
    } else {
        "Last time: plate 7 × 8 @ 8. Coach suggests plate 7 × 9 @ 8."
    }
    val advice = Advice(AdviceType.TARGET, null, SHOWCASE_DATE, reason, 0, target, id = 1, mode = mode)
    return listOf(
        platesItem(1, "Lat pulldown", mode, 7.0, listOf(plateSet(1, 1, 7.0, if (bodybuilding) 12 else 9)), advice, PlateCount.WHOLE),
        platesItem(2, "Seated cable row", mode, 55.0),
        platesItem(3, "Leg press", mode, 140.0),
        platesItem(4, "Cable crossover", mode, 15.0),
    )
}

/** Today with Lat pulldown counted in plates, on [mode]'s side; each machine's ⋮ offers the plate item. */
@Composable
fun TodayPlatesShowcase(mode: TrainingMode) {
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = platesItems(mode), mode = mode), TodayActions(onCountPlates = { _, _ -> }))
}

/**
 * Where the setting is: Today with Lat pulldown still in kg (the coach's kg target, the kg field), its ⋮ menu open
 * with Skip today and Count plates on this machine. The menu is its own window, so its real items are drawn in a
 * stand-in where it drops from the card's ⋮.
 */
@Composable
fun TodayPlatesMenuShowcase() {
    val mode = TrainingMode.POWERLIFTING
    val target = AdvicePayload.Target(1, 62.5, 8, 8.0)
    val advice = Advice(AdviceType.TARGET, null, SHOWCASE_DATE, "Last time: 60 × 8 @ 8. Coach suggests 62.5 × 8 @ 8.", 0, target, id = 1, mode = mode)
    val items = listOf(platesItem(1, "Lat pulldown", mode, 60.0, suggestion = advice)) + platesItems(mode).drop(1)
    Box(Modifier.fillMaxSize()) {
        TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = items, mode = mode), TodayActions(onCountPlates = { _, _ -> }))
        Surface(
            Modifier.align(Alignment.TopEnd).padding(top = 150.dp, end = 24.dp).width(280.dp),
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                SkipMenuItem {}
                PlateMenuItem {}
            }
        }
    }
}

/** The plate sheet over Today, just after Whole plates was chosen for Lat pulldown, which had kg sets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateSheetShowcase() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        TodayPlatesShowcase(TrainingMode.POWERLIFTING)
        Box(Modifier.fillMaxSize().background(scheme.scrim.copy(alpha = 0.32f)))
        Surface(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = scheme.surface,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle(color = scheme.outline)
                PlateCountContent("Lat pulldown", PlateCount.WHOLE, plateNote(LoadUnit.KG, LoadUnit.PLATES, hasSetsInOpenedUnit = true), onChoose = {}, onDone = {})
            }
        }
    }
}

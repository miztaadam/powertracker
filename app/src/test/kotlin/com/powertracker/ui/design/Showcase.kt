package com.powertracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.plan.PlanItemFields
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.data.AppSettings
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.coach.WarmUp
import com.powertracker.domain.history.LastTime
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.ui.PtBottomBar
import com.powertracker.ui.Tab
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.RatioUi
import com.powertracker.ui.components.ChartSeries
import com.powertracker.ui.components.LineChart
import com.powertracker.ui.components.NumberDisplay
import com.powertracker.ui.components.NumberSize
import com.powertracker.ui.components.Pill
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.RatioBar
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.Sparkline
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.today.FinishSummary
import com.powertracker.ui.today.RestTimerUi
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

// Mockups for owner approval: real components, hard-coded sample data. One lifter, one story:
// squat e1RM 190, bench 122.5 (stalled), deadlift 250, on Wednesday 11 November.

internal val SQUAT = listOf(172.5f, 175f, 175f, 177.5f, 180f, 180f, 182.5f, 185f, 185f, 187.5f, 187.5f, 190f)
internal val BENCH = listOf(115f, 117.5f, 119f, 120f, 121f, 122.5f, 122f, 122.5f, 122.5f, 122f, 122.5f, 122.5f)
internal val DEADLIFT = listOf(232.5f, 235f, 237.5f, 237.5f, 240f, 242.5f, 242.5f, 245f, 247.5f, 247.5f, 250f, 250f)

/** Theme, background, a stand-in status bar, and the real bottom bar when [tab] is given. */
@Composable
fun ShowcaseFrame(dark: Boolean, tab: Tab?, mode: TrainingMode = TrainingMode.POWERLIFTING, content: @Composable () -> Unit) {
    // Screenshots are taken with motion off (stage 2), whatever LocalMotion's default.
    CompositionLocalProvider(LocalMotion provides false) {
        PowertrackerTheme(mode = mode, darkTheme = dark) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Row(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("9:41", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                Box(Modifier.weight(1f)) { content() }
                if (tab != null) PtBottomBar(tab.route) {}
            }
        }
    }
}

internal val SHOWCASE_DATE: LocalDate = LocalDate(2026, 11, 11)

private fun logged(id: Long, kg: Double, reps: Int, rpe: Double, failed: Boolean = false, miss: MissPoint? = null) =
    StoredSet(id, 1, LoggedSet(1, kg, reps, rpe, failed, miss, isTopSet = false))

private fun todayItem(
    id: Long,
    name: String,
    category: Category,
    sets: Int,
    reps: Int,
    rpe: Double,
    weightKg: Double,
    done: List<StoredSet> = emptyList(),
    suggestion: Advice? = null,
) = TodayItem(
    exercise = exercise(id, name, category),
    planned = PlanItem(id, DayOfWeek.WEDNESDAY, id, sets, reps, rpe, id.toInt()),
    suggestion = suggestion,
    sets = done,
    editor = SetEditor(weightKg, reps, rpe),
    targetSets = sets,
    perSide = PlateLoader.perSide(weightKg, changePlates = false),
    lastWeightKg = weightKg,
).withWarmUp()

/** The warm-up checklist (1.23) as the view model puts it on a card: before the first set, for the weight in the field. */
internal fun TodayItem.withWarmUp(): TodayItem =
    copy(warmUpOpen = sets.isEmpty(), warmUp = if (sets.isEmpty()) WarmUp.sets(exercise, editor.weightKg, planned?.reps ?: editor.reps, changePlates = false) else emptyList())

private fun todayItems(squatSets: List<StoredSet>, suggest: Boolean) = listOf(
    todayItem(
        1, "Squat", Category.SQUAT, 5, 5, 8.0, 155.0, squatSets,
        suggestion = if (suggest) {
            Advice(AdviceType.TARGET, MainLift.SQUAT, SHOWCASE_DATE, "Last time: 150 × 5 @ 7. Coach suggests 155 × 5 @ 8.", 0, AdvicePayload.Target(1, 155.0, 5, 8.0), id = 1)
        } else {
            null
        },
        // Last week's squats (1.20), worked up to the top set the coach's "Last time: 150 × 5 @ 7" names, then two
        // lighter ones: with three sets logged today the fourth is the marked row, with its "Use set 4" button (1.21).
    ).copy(lastTime = LastTime(SHOWCASE_DATE.minus(7, DateTimeUnit.DAY), listOf(120.0 to 5.0, 140.0 to 6.0, 150.0 to 7.0, 130.0 to 6.0, 130.0 to 6.5).map { (kg, rpe) -> LoggedSet(1, kg, 5, rpe, failed = false, missPoint = null, isTopSet = false) })),
    todayItem(2, "Bench", Category.BENCH, 4, 6, 7.5, 100.0),
    todayItem(3, "Barbell row", Category.ACCESSORY, 3, 8, 8.0, 80.0),
    todayItem(4, "Triceps pushdown", Category.ACCESSORY, 3, 12, 8.0, 30.0),
)

/** The real Today screen mid-session: third squat set missed, coach chip still open, rest timer running. */
@Composable
fun TodayShowcase() {
    val squatSets = listOf(logged(1, 150.0, 5, 7.5), logged(2, 155.0, 5, 8.5), logged(3, 155.0, 3, 10.0, failed = true, miss = MissPoint.FLOOR))
    TodayContent(
        TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = todayItems(squatSets, suggest = true)),
        TodayActions(),
        restTimer = RestTimerUi(108, 180, {}, {}, {}),
    )
}

/**
 * Today with the session just started (1.23): no squat set logged yet, so the warm-up checklist is on the card, worked
 * out from the 155 kg in the field, its first set ticked.
 */
@Composable
fun TodayWarmUpShowcase() {
    val items = todayItems(emptyList(), suggest = false).toMutableList()
    items[0] = items[0].copy(warmUpDone = setOf(items[0].warmUp.first()))
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = items), TodayActions())
}

/** Before the session: the plan for the day and the optional check-in. */
@Composable
fun TodayStartShowcase() {
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, items = todayItems(emptyList(), suggest = true)), TodayActions())
}

/** The planned sets are done: the primary button moves on to the next exercise. */
@Composable
fun TodayNextShowcase() {
    val squatSets = listOf(logged(1, 150.0, 5, 7.5), logged(2, 155.0, 5, 8.0), logged(3, 155.0, 5, 8.5), logged(4, 155.0, 5, 9.0), logged(5, 155.0, 5, 9.5))
    val items = todayItems(squatSets, suggest = false).toMutableList()
    // With a set logged the bench has no warm-up checklist, as the view model has it.
    items[1] = items[1].copy(sets = listOf(logged(6, 100.0, 6, 7.5))).withWarmUp()
    TodayContent(TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = items), TodayActions())
}

/** After Finish. */
@Composable
fun TodaySummaryShowcase() {
    TodayContent(
        TodayState(SHOWCASE_DATE, loading = false, sessionId = 1, items = todayItems(emptyList(), suggest = false), summary = FinishSummary(setCount = 15, exerciseCount = 4, newNotes = 2)),
        TodayActions(),
    )
}

/** The real Coach screen with one card of each of the three commonest kinds. */
@Composable
fun CoachShowcase() {
    val stall = Advice(
        AdviceType.STALL, MainLift.BENCH, SHOWCASE_DATE,
        "Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps.", 3,
        AdvicePayload.Accessories(listOf(AccessoryPrescription("Close-grip bench", 3, 3, 5), AccessoryPrescription("JM press", 3, 8, 10), AccessoryPrescription("Board press", 3, 3, 5))),
        id = 1,
    )
    val deload = Advice(
        AdviceType.DELOAD, null, SHOWCASE_DATE,
        "Signs of fatigue: RPE rising at the same load on squat and deadlift; 3 failed reps. Take a deload week: about 60% of the volume at about 90% of the load.", 2,
        AdvicePayload.Deload(), id = 2,
    )
    val weak = Advice(
        AdviceType.WEAK_POINT, MainLift.SQUAT, SHOWCASE_DATE,
        "Your squat is low relative to your deadlift (76% vs a typical 80–90%). Prioritise squat.", 4,
        AdvicePayload.WeakPoint(MainLift.SQUAT, null), id = 3,
    )
    CoachContent(
        CoachState(
            loading = false,
            cards = listOf(
                CoachCard(deload, CardDetail.Deload(volumePct = 60, loadPct = 90)),
                CoachCard(stall, CardDetail.Stall(BENCH.takeLast(6), 122.5, (stall.payload as AdvicePayload.Accessories).items)),
                CoachCard(
                    weak,
                    CardDetail.WeakPoint(
                        RatioUi(MainLift.SQUAT, MainLift.DEADLIFT, 76, 80, 90), null, emptyList(),
                        listOf(AccessoryPrescription("Pause squat", 3, 3, 5), AccessoryPrescription("Front squat", 3, 3, 5)),
                    ),
                ),
            ),
        ),
        onApply = {}, onDismiss = {},
    )
}

/** A quiet week: nothing open. */
@Composable
fun CoachEmptyShowcase() {
    CoachContent(CoachState(loading = false), onApply = {}, onDismiss = {})
}

/** The two rarer cards: back after a break, and a weak point found from where reps are missed. */
@Composable
fun CoachMoreShowcase() {
    val back = Advice(
        AdviceType.RE_ENTRY, null, SHOWCASE_DATE,
        "Welcome back — it's been 21 days since your previous session. Take a lighter week: about 85% of your usual loads.", 21,
        AdvicePayload.ReEntry(), id = 1,
    )
    val weak = Advice(
        AdviceType.WEAK_POINT, MainLift.DEADLIFT, SHOWCASE_DATE, "You've missed deadlift off the floor 3 times in the last 4 weeks.", 3,
        AdvicePayload.WeakPoint(MainLift.DEADLIFT, MissPoint.FLOOR), id = 2,
    )
    CoachContent(
        CoachState(
            loading = false,
            cards = listOf(
                CoachCard(back, CardDetail.ReEntry(85)),
                CoachCard(
                    weak,
                    CardDetail.WeakPoint(
                        null, MissPoint.FLOOR, listOf("Quads", "Lats", "Position"),
                        listOf(AccessoryPrescription("Deficit deadlift", 3, 3, 5), AccessoryPrescription("Paused deadlift", 3, 3, 5)),
                    ),
                ),
            ),
        ),
        onApply = {}, onDismiss = {},
    )
}

private fun exercise(id: Long, name: String, category: Category = Category.ACCESSORY) = Exercise(id, name, category, null, emptyList(), isBuiltIn = true)

private fun day(day: DayOfWeek, vararg rows: Triple<String, Pair<Int, Int>, Double>) = DaySection(
    day,
    rows.mapIndexed { i, (name, dose, rpe) -> PlanRow(PlanItem(day.isoDayNumber * 10L + i, day, i.toLong(), dose.first, dose.second, rpe, i), name) },
)

@Composable
fun PlanShowcase(knownBests: Boolean = false) {
    val main = listOf(exercise(1, "Squat", Category.SQUAT), exercise(2, "Bench", Category.BENCH), exercise(3, "Deadlift", Category.DEADLIFT))
    val state = if (knownBests) {
        // 1.16.1: the strict press switch on, a best for each lift (typed or lifted), and one lift without one yet.
        PlanUiState(
            sections = listOf(
                day(DayOfWeek.MONDAY, Triple("Squat", 5 to 5, 8.0), Triple("Strict press", 3 to 5, 8.0)),
                day(DayOfWeek.THURSDAY, Triple("Deadlift", 3 to 3, 8.0), Triple("Bench", 4 to 6, 7.5)),
            ),
            mainExercises = main + exercise(4, "Strict press", Category.STRICT_PRESS),
            bests = mapOf(1L to 182.5, 2L to 120.0, 4L to 72.5),
        )
    } else {
        PlanUiState(
            sections = listOf(
                day(DayOfWeek.MONDAY, Triple("Squat", 5 to 5, 8.0), Triple("Bench", 4 to 6, 7.5), Triple("Barbell row", 3 to 8, 8.0)),
                day(DayOfWeek.WEDNESDAY, Triple("Deadlift", 3 to 3, 8.0), Triple("Close-grip bench", 3 to 5, 8.0)),
                day(DayOfWeek.FRIDAY, Triple("Bench", 3 to 5, 8.0), Triple("Pause squat", 3 to 4, 7.5)),
            ),
            mainExercises = main,
        )
    }
    PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {})
}

/**
 * Plan's add form (1.12): picking an exercise on a day opens the sets/reps form with the ready-made numbers. The real
 * [PlanItemFields] in a stand-in for the AlertDialog window (which this capture cannot see), drawn from
 * [AlertDialogDefaults] over the Plan screen.
 */
@Composable
fun PlanAddFormShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    val bodybuilding = mode == TrainingMode.BODYBUILDING
    DialogShowcase(
        behind = { if (bodybuilding) PlanBodybuildingShowcase() else PlanShowcase() },
        title = "Add to Monday",
        confirmText = "Add",
    ) {
        if (bodybuilding) {
            PlanItemFields("Incline DB press", 3, 8, 12, 8.0, {}, {}, {}, {}, {})
        } else {
            PlanItemFields("Squat", 3, 5, null, 8.0, {}, {}, {}, {}, {})
        }
    }
}

/** A stand-in for an AlertDialog window: its shape, colours, padding and type, centred over [behind] under a scrim. */
@Composable
internal fun DialogShowcase(behind: @Composable () -> Unit, title: String, confirmText: String, text: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        behind()
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)))
        Surface(
            Modifier.align(Alignment.Center).padding(horizontal = 16.dp).widthIn(min = 280.dp, max = 560.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(Modifier.padding(24.dp)) {
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) { Text(title, Modifier.padding(bottom = 16.dp)) }
                }
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { Box(Modifier.padding(bottom = 24.dp)) { text() } }
                }
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietButton("Cancel", {})
                    PrimaryButton(confirmText, {}, compact = true)
                }
            }
        }
    }
}

@Composable
fun SettingsShowcase(strictPress: Boolean = false, bodybuilding: Boolean = false) {
    SettingsContent(
        AppSettings(
            changePlates = true,
            showStrictPress = strictPress,
            trainingMode = if (bodybuilding) TrainingMode.BODYBUILDING else TrainingMode.POWERLIFTING,
        ),
        onUpdate = {}, onExport = {}, onRestore = {}, onBack = {},
    )
}

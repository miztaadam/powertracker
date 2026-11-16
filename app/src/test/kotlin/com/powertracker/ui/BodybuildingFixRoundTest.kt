package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.domain.model.on
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.progress.ChartRange
import com.powertracker.ui.progress.LiftHistoryViewModel
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The fixes after the platform reviews (rulings A to F), so both apps behave and read the same. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class BodybuildingFixRoundTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var bench: Exercise
    private lateinit var squat: Exercise
    private lateinit var curl: Exercise
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            val exercises = training.exercises().first()
            bench = exercises.first { it.name == "Bench" }
            squat = exercises.first { it.name == "Squat" }
            curl = exercises.first { it.name == "Curl" }
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    private fun set(exercise: Exercise, kg: Double, reps: Int) = LoggedSet(exercise.id, kg, reps, 8.0, failed = false, missPoint = null, isTopSet = false)

    private fun session(daysAgo: Int, mode: TrainingMode, vararg sets: LoggedSet) = runBlocking {
        val id = training.startSession(today.minus(daysAgo, DateTimeUnit.DAY), null, null, mode)
        sets.forEach { training.logSet(id, it) }
    }

    /** Benching 100 × 5 on the powerlifting side, and a heavier-looking 120 × 10 on the bodybuilding side. */
    private fun benchOnBothSides() {
        session(7, POWERLIFTING, set(bench, 100.0, 5), set(squat, 140.0, 5))
        session(3, BODYBUILDING, set(bench, 120.0, 10), set(squat, 180.0, 10))
    }

    private fun history() = runBlocking { training.history().first() }

    // A. Powerlifting figures read powerlifting sessions only.

    @Test
    fun progress_powerliftingFigures_ignoreBodybuildingSessions() {
        benchOnBothSides()
        val all = history()
        val state = ProgressViewModel.build(all, ChartRange.MONTHS_6, today, showStrictPress = false)
        assertEquals(ProgressViewModel.build(all.on(POWERLIFTING), ChartRange.MONTHS_6, today, showStrictPress = false), state)
        assertEquals(100.0, state.prs.single { it.lift == MainLift.BENCH }.pr.weightKg, 0.0)
        assertEquals(HistoryStats(all.on(POWERLIFTING)).currentE1rm(MainLift.BENCH, today)!!, state.lifts.single { it.lift == MainLift.BENCH }.e1rmKg!!, 0.5)
    }

    @Test
    fun progress_aLogOfOnlyBodybuildingSessions_hasNoPowerliftingFigures() {
        session(3, BODYBUILDING, set(bench, 120.0, 10))
        assertFalse(ProgressViewModel.build(history(), ChartRange.MONTHS_6, today, showStrictPress = false).hasData)
    }

    @Test
    fun liftHistory_showsPowerliftingSessionsOnly_withTheirRecords() {
        benchOnBothSides()
        val all = history()
        val state = LiftHistoryViewModel.build(all, MainLift.BENCH, today)
        assertEquals(listOf(100.0), state.sessions.flatMap { s -> s.groups.flatMap { g -> g.sets.map { it.set.weightKg } } })
        assertEquals(100.0, state.best!!.weightKg, 0.0)
        assertEquals(HistoryStats(all.on(POWERLIFTING)).currentE1rm(MainLift.BENCH, today), state.currentE1rmKg)
    }

    @Test
    fun coach_stallCardFigures_readPowerliftingSessionsOnly() {
        benchOnBothSides()
        runBlocking { advice.insert(listOf(Advice(AdviceType.STALL, MainLift.BENCH, today, "stalled", 3, AdvicePayload.Accessories(emptyList())))) }
        val vm = CoachViewModel(training, advice, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }
        await("card") { !vm.state.value.loading && vm.state.value.cards.isNotEmpty() }
        val stall = vm.state.value.cards.single().detail as CardDetail.Stall
        assertEquals(HistoryStats(history().on(POWERLIFTING)).currentE1rm(MainLift.BENCH, today), stall.e1rmKg)
    }

    // B. On the bodybuilding side every Progress row opens the exercise history, the main lifts' too.

    @Test
    fun progress_bodybuildingSide_benchRow_opensTheExerciseHistory_notTheLiftHistory() {
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, 3, 8, 8.0, 0, BODYBUILDING, 12)) }
        session(2, BODYBUILDING, set(bench, 60.0, 12))
        val state = ProgressViewModel.buildBodybuilding(history(), runBlocking { training.plan(BODYBUILDING).first() }, today)
        val lifts = mutableListOf<MainLift>()
        val exercises = mutableListOf<Long>()
        compose.setContent {
            PowertrackerTheme(mode = BODYBUILDING) {
                ProgressContent(state, onRange = {}, onOpenLift = { lifts += it }, onOpenExercise = { exercises += it })
            }
        }
        compose.onNodeWithText("Bench").performClick()
        assertEquals(listOf(bench.id), exercises)
        assertEquals(emptyList<MainLift>(), lifts)
    }

    // C. Best sets: " · " between them on screen, read as a list, and the row says it opens the history.

    @Test
    fun progress_bestSets_separatedByADot_readAsAList_rowOpensHistory() {
        fun best(day: Int, kg: Double, reps: Int) = LocalDate(2026, 11, day) to LoggedSet(1, kg, reps, 8.0, false, null, false)
        compose.setContent {
            PowertrackerTheme(mode = BODYBUILDING) {
                ProgressContent(
                    ProgressState(loading = false, hasData = true, mode = BODYBUILDING, trends = listOf(ExerciseTrend(1, "Incline DB press", listOf(best(1, 24.0, 12), best(8, 26.0, 9)), goingUp = true))),
                    onRange = {}, onOpenLift = {},
                )
            }
        }
        compose.onNodeWithText("24 × 12 · 26 × 9").assertExists()
        val row = compose.onNode(hasClickAction() and hasText("Incline DB press"))
        row.assert(SemanticsMatcher("click label Open history") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open history" })
        row.assert(
            SemanticsMatcher("spoken as Incline DB press. 24 × 12, 26 × 9, without the dot") { node ->
                val spoken = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(", ")
                spoken.startsWith("Incline DB press. 24 × 12, 26 × 9") && "·" !in spoken
            },
        )
    }

    // E. No powerlifting wording on the bodybuilding side.

    private fun deloadFeed(mode: TrainingMode) {
        val deload = Advice(AdviceType.DELOAD, null, today, "tired", 1, AdvicePayload.Deload(), id = 1)
        compose.setContent {
            PowertrackerTheme(mode = mode) {
                CoachContent(CoachState(loading = false, cards = listOf(CoachCard(deload, CardDetail.Deload(60, 90)))), onApply = {}, onDismiss = {})
            }
        }
    }

    @Test
    fun coach_deloadCard_onTheBodybuildingSide_saysAllExercises_andLastWeights() {
        deloadFeed(BODYBUILDING)
        compose.onNodeWithText("DELOAD · ALL EXERCISES").assertExists()
        compose.onNodeWithText("of your last weights").assertExists()
        compose.onNodeWithText("DELOAD · ALL LIFTS").assertDoesNotExist()
        compose.onNodeWithText("of your last top sets").assertDoesNotExist()
    }

    @Test
    fun coach_deloadCard_onThePowerliftingSide_isAsBefore() {
        deloadFeed(POWERLIFTING)
        compose.onNodeWithText("DELOAD · ALL LIFTS").assertExists()
        compose.onNodeWithText("of your last top sets").assertExists()
    }

    private var shownSettings = AppSettings()

    private fun settingsOn(mode: TrainingMode) {
        shownSettings = AppSettings(trainingMode = mode)
        compose.setContent {
            PowertrackerTheme(mode = mode) {
                SettingsContent(shownSettings, onUpdate = { f -> shownSettings = f(shownSettings) }, onExport = {}, onRestore = {}, onBack = {})
            }
        }
    }

    @Test
    fun settings_bodybuildingSide_hidesThePowerliftingRows_andHasOneRestRow() {
        settingsOn(BODYBUILDING)
        for (gone in listOf("Default RPE", "Planned effort for main lifts", "PROGRESS", "Strict press", "Main lift rest", "Squat, bench, deadlift", "Accessory rest", "Everything else")) {
            compose.onNodeWithText(gone).assertDoesNotExist()
        }
        compose.onNodeWithText("Rest").assertExists()
        compose.onNodeWithText("Every exercise").assertExists()
        val before = shownSettings
        compose.onNodeWithContentDescription("Increase Rest").performClick()
        // The one rest row is the accessory rest every bodybuilding exercise gets; nothing else moves.
        assertEquals(before.copy(restAccessorySec = before.restAccessorySec + 15), shownSettings)
    }

    @Test
    fun settings_powerliftingSide_keepsEveryRow() {
        settingsOn(POWERLIFTING)
        for (shown in listOf("Default RPE", "Planned effort for main lifts", "PROGRESS", "Strict press", "Main lift rest", "Squat, bench, deadlift", "Accessory rest", "Everything else")) {
            compose.onNodeWithText(shown).assertExists()
        }
        compose.onNodeWithText("Every exercise").assertDoesNotExist()
    }

    private fun planOn(mode: TrainingMode) {
        val main = listOf(Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true))
        compose.setContent {
            PowertrackerTheme(mode = mode) {
                PlanContent(
                    PlanUiState(sections = listOf(DaySection(DayOfWeek.MONDAY, emptyList())), mainExercises = main, mode = mode),
                    onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {},
                )
            }
        }
    }

    @Test
    fun plan_bodybuildingSide_hasNoKnownBests() {
        planOn(BODYBUILDING)
        compose.onNodeWithText("KNOWN BESTS").assertDoesNotExist()
        compose.onNodeWithText("Set best").assertDoesNotExist()
    }

    @Test
    fun plan_powerliftingSide_keepsKnownBests() {
        planOn(POWERLIFTING)
        compose.onNodeWithText("KNOWN BESTS").assertExists()
        compose.onNodeWithText("Set best").assertExists()
    }

    // F. The welcome-back card clears everywhere at once.

    private fun welcomeBackOnBothScreens(): Pair<TodayViewModel, CoachViewModel> {
        runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
            settings.update { it.copy(trainingMode = BODYBUILDING, bodybuildingLeftOn = today.minus(31, DateTimeUnit.DAY)) }
        }
        val todayVm = TodayViewModel(training, advice, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }
        val coachVm = CoachViewModel(training, advice, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }
        await("both cards") { todayVm.state.value.welcomeBack && coachVm.state.value.welcomeBack }
        return todayVm to coachVm
    }

    @Test
    fun welcomeBack_answeredOnToday_isGoneFromCoach() {
        val (todayVm, coachVm) = welcomeBackOnBothScreens()
        todayVm.answerWelcomeBack()
        await("gone from both") { !todayVm.state.value.welcomeBack && !coachVm.state.value.welcomeBack }
        assertTrue(runBlocking { settings.settings.first() }.bodybuildingLeftOn == null)
    }

    @Test
    fun welcomeBack_answeredOnCoach_isGoneFromToday() {
        val (todayVm, coachVm) = welcomeBackOnBothScreens()
        coachVm.answerWelcomeBack()
        await("gone from both") { !todayVm.state.value.welcomeBack && !coachVm.state.value.welcomeBack }
        assertTrue(runBlocking { settings.settings.first() }.bodybuildingLeftOn == null)
    }
}

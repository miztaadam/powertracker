package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.coach.RatioUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CoachViewModelTest {
    private val today: LocalDate = LocalDate.of(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var exercises: List<Exercise>
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            exercises = training.exercises().first()
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        db.close()
    }

    private fun id(name: String) = exercises.first { it.name == name }.id

    private fun viewModel() = CoachViewModel(training, advice, settings) { today }.also { vm ->
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.messages.collect { messages += it } }
    }

    /** View-model work resumes on the main looper, which Robolectric only runs when asked. */
    private fun CoachViewModel.await(condition: (CoachState) -> Boolean): CoachState {
        val deadline = System.currentTimeMillis() + 5_000
        while (state.value.loading || !condition(state.value)) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out; state was ${state.value}" }
        }
        return state.value
    }

    /** A card leaves the feed as soon as it is tapped; the stored result follows. */
    private fun awaitStatuses(vararg expected: AdviceStatus) {
        val deadline = System.currentTimeMillis() + 5_000
        while (stored().map { it.status } != expected.toList() || messages.size < expected.count { it == AdviceStatus.APPLIED }) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out; stored ${stored()}, messages $messages" }
        }
    }

    private fun plan() = runBlocking { training.plan().first() }

    private fun stored() = runBlocking { advice.all() }

    private fun insert(vararg items: Advice) = runBlocking { advice.insert(items.toList()) }

    private fun weakPoint(lift: MainLift, miss: MissPoint?, reason: String = "Prioritise it.") =
        Advice(AdviceType.WEAK_POINT, lift, today, reason, 4, AdvicePayload.WeakPoint(lift, miss))

    @Test
    fun cards_leaveOutTargets_andOrderByTypeThenNewest() {
        insert(
            weakPoint(MainLift.SQUAT, MissPoint.FLOOR),
            Advice(AdviceType.STALL, MainLift.BENCH, today.minusDays(3), "older", 3, AdvicePayload.Accessories(emptyList())),
            Advice(AdviceType.STALL, MainLift.SQUAT, today, "newer", 3, AdvicePayload.Accessories(emptyList())),
            Advice(AdviceType.TARGET, MainLift.SQUAT, today, "target", 0, AdvicePayload.Target(id("Squat"), 155.0, 5, 8.0)),
            Advice(AdviceType.RE_ENTRY, null, today, "back", 20, AdvicePayload.ReEntry()),
            Advice(AdviceType.DELOAD, null, today, "tired", 2, AdvicePayload.Deload()),
        )
        val state = viewModel().await { it.cards.size == 5 }
        assertEquals(
            listOf(AdviceType.DELOAD to "tired", AdviceType.RE_ENTRY to "back", AdviceType.STALL to "newer", AdviceType.STALL to "older", AdviceType.WEAK_POINT to "Prioritise it."),
            state.cards.map { it.advice.type to it.advice.reason },
        )
        assertEquals(CardDetail.Deload(volumePct = 60, loadPct = 90), state.cards[0].detail)
        assertEquals(CardDetail.ReEntry(loadPct = 85), state.cards[1].detail)
    }

    @Test
    fun weakPoint_showsRatioAgainstTypicalRange() {
        runBlocking {
            training.setBaseline(Baseline(id("Squat"), 190.0, today))
            training.setBaseline(Baseline(id("Deadlift"), 250.0, today))
        }
        insert(weakPoint(MainLift.SQUAT, null))
        val detail = viewModel().await { it.cards.size == 1 }.cards.single().detail as CardDetail.WeakPoint
        assertEquals(RatioUi(MainLift.SQUAT, MainLift.DEADLIFT, valuePct = 76, lowPct = 80, highPct = 90), detail.ratio)
        assertNull(detail.missPoint)
    }

    @Test
    fun weakPointApply_addsFirstTwoOptionsNotAlreadyPlanned() {
        runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, id("Squat"), sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            // Pause squat is the first option for squat misses out of the hole, and is already planned.
            training.savePlanItem(PlanItem(0, DayOfWeek.FRIDAY, id("Pause squat"), sets = 3, reps = 4, targetRpe = 7.5, order = 0))
            settings.update { it.copy(defaultRpe = 7.5) }
        }
        insert(weakPoint(MainLift.SQUAT, MissPoint.FLOOR))
        val vm = viewModel()
        val card = vm.await { it.cards.size == 1 }.cards.single()
        val detail = card.detail as CardDetail.WeakPoint
        assertEquals(MissPoint.FLOOR, detail.missPoint)
        assertNull(detail.ratio)
        assertEquals(
            listOf(AccessoryPrescription("Front squat", 3, 5, 5), AccessoryPrescription("Bulgarian split squat", 3, 10, 10)),
            detail.additions,
        )

        vm.apply(card.advice.id)
        vm.await { it.cards.isEmpty() }
        awaitStatuses(AdviceStatus.APPLIED)

        assertEquals(
            listOf(
                PlanItem(0, DayOfWeek.MONDAY, id("Squat"), 5, 5, 8.0, 0),
                PlanItem(0, DayOfWeek.MONDAY, id("Front squat"), 3, 5, 7.5, 1),
                PlanItem(0, DayOfWeek.MONDAY, id("Bulgarian split squat"), 3, 10, 7.5, 2),
            ),
            plan().filter { it.weekday == DayOfWeek.MONDAY }.sortedBy { it.order }.map { it.copy(id = 0) },
        )
        assertEquals(4, plan().size)
    }

    @Test
    fun stallApply_withoutTheLiftInThePlan_usesFirstTrainingDay_andSkipsWhatIsThere() {
        runBlocking {
            settings.update { it.copy(trainingDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY)) }
            training.savePlanItem(PlanItem(0, DayOfWeek.TUESDAY, id("JM press"), sets = 3, reps = 10, targetRpe = 8.0, order = 0))
        }
        val payload = AdvicePayload.Accessories(
            listOf(AccessoryPrescription("close-grip BENCH", 3, 3, 5), AccessoryPrescription("JM press", 3, 8, 10), AccessoryPrescription("No such lift", 3, 8, 10)),
        )
        insert(Advice(AdviceType.STALL, MainLift.BENCH, today, "Bench e1RM hasn't improved for 3 weeks.", 3, payload))
        val vm = viewModel()

        vm.apply(vm.await { it.cards.size == 1 }.cards.single().advice.id)
        vm.await { it.cards.isEmpty() }
        awaitStatuses(AdviceStatus.APPLIED)

        assertEquals(listOf("Added 1 exercise to Tuesday"), messages)
        assertEquals(
            listOf(PlanItem(0, DayOfWeek.TUESDAY, id("JM press"), 3, 10, 8.0, 0), PlanItem(0, DayOfWeek.TUESDAY, id("Close-grip bench"), 3, 5, 8.0, 1)),
            plan().sortedBy { it.order }.map { it.copy(id = 0) },
        )
    }

    @Test
    fun deloadApply_onlyMarksApplied() {
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, id("Squat"), sets = 5, reps = 5, targetRpe = 8.0, order = 0)) }
        insert(
            Advice(AdviceType.DELOAD, null, today, "Signs of fatigue.", 2, AdvicePayload.Deload()),
            Advice(AdviceType.RE_ENTRY, null, today, "Welcome back.", 20, AdvicePayload.ReEntry()),
        )
        val vm = viewModel()
        val cards = vm.await { it.cards.size == 2 }.cards

        vm.apply(cards[0].advice.id)
        vm.await { it.cards.size == 1 }
        awaitStatuses(AdviceStatus.APPLIED, AdviceStatus.OPEN)
        assertEquals(AdviceType.DELOAD, stored().first().type)

        vm.apply(cards[1].advice.id)
        vm.await { it.cards.isEmpty() }
        awaitStatuses(AdviceStatus.APPLIED, AdviceStatus.APPLIED)
        assertEquals(listOf("Deload week on — targets lightened for 7 days", "Easing back in for 7 days"), messages)
        assertEquals(1, plan().size)
    }

    @Test
    fun dismiss_marksDismissed() {
        insert(Advice(AdviceType.DELOAD, null, today, "Signs of fatigue.", 2, AdvicePayload.Deload()))
        val vm = viewModel()
        vm.dismiss(vm.await { it.cards.size == 1 }.cards.single().advice.id)
        vm.await { it.cards.isEmpty() }
        awaitStatuses(AdviceStatus.DISMISSED)
        assertEquals(emptyList<String>(), messages)
    }
}

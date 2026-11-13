package com.powertracker.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.forSide
import com.powertracker.platform.systemToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** One lift as a share of another, against the typical range for that pair. */
data class RatioUi(val numerator: MainLift, val denominator: MainLift, val valuePct: Int, val lowPct: Int, val highPct: Int)

/** The figures a card shows under its reason; one shape per advice type. */
sealed interface CardDetail {
    /** [trend] is the best weekly e1RM for up to the last six trained weeks, oldest first. */
    data class Stall(val trend: List<Float>, val e1rmKg: Double?, val prescriptions: List<AccessoryPrescription>) : CardDetail

    data class Deload(val volumePct: Int, val loadPct: Int) : CardDetail

    data class ReEntry(val loadPct: Int) : CardDetail

    /** [additions] is what Apply would add to the plan; [muscles] goes with [missPoint]. */
    data class WeakPoint(
        val ratio: RatioUi?,
        val missPoint: MissPoint?,
        val muscles: List<String>,
        val additions: List<AccessoryPrescription>,
    ) : CardDetail
}

data class CoachCard(val advice: Advice, val detail: CardDetail)

data class CoachState(
    val loading: Boolean = true,
    val cards: List<CoachCard> = emptyList(),
    /** The welcome-back card for the side the app is on, shown above the advice (bodybuilding mode spec §7.7). */
    val welcomeBack: Boolean = false,
)

class CoachViewModel(
    private val training: TrainingRepository,
    private val advice: AdviceRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-line confirmations for a snackbar ("Added 2 exercises to Wednesday"). */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /**
     * Advice being applied or dismissed. Its card leaves the feed at once, so it cannot be tapped
     * twice and does not redraw while its accessories land in the plan; it comes back if the work fails.
     */
    private val busy = MutableStateFlow(emptySet<Long>())

    /** The feed is the advice of the side the app is on, with every deload and easing back in. */
    val state: StateFlow<CoachState> =
        combine(combine(advice.open(), busy, ::Pair), training.history(), training.plans(), training.exercises(), settings.settings) { (open, busy), history, plans, exercises, s ->
            val mode = s.trainingMode
            val plan = plans.of(mode)
            val stats = HistoryStats(history)
            val date = today()
            val cards = open.forSide(mode)
                .filter { it.type in FEED_ORDER && it.id !in busy && !expired(it, date) }
                .sortedWith(compareBy<Advice> { FEED_ORDER.indexOf(it.type) }.thenByDescending { it.createdAt }.thenByDescending { it.id })
                .mapNotNull { a -> detail(a, stats, date, plan, exercises)?.let { CoachCard(a, it) } }
            CoachState(loading = false, cards = cards, welcomeBack = Coaching.welcomeBack(s.leftOn(mode), date, plan.isNotEmpty()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachState())

    /** The welcome-back card was answered, either way: the side the app is on is marked as not left. */
    fun answerWelcomeBack() {
        viewModelScope.launch { settings.clearLeftOn(settings.settings.first().trainingMode) }
    }

    /** An OPEN deload or re-entry stops counting after 14 days (see [Coach.openExpired]), so it leaves the feed too. */
    private fun expired(advice: Advice, date: LocalDate): Boolean = Coach.openExpired(advice.type, advice.createdAt, date)

    private fun detail(advice: Advice, stats: HistoryStats, date: LocalDate, plan: List<PlanItem>, exercises: List<Exercise>): CardDetail? =
        when (val payload = advice.payload) {
            is AdvicePayload.Accessories -> {
                val trend = advice.lift?.let { stats.weeklyTrend(it, date) }.orEmpty().takeLast(TREND_WEEKS)
                val current = advice.lift?.let { stats.currentE1rm(it, date) } ?: trend.lastOrNull()?.e1rmKg
                CardDetail.Stall(trend.map { it.e1rmKg.toFloat() }, current, payload.items)
            }
            is AdvicePayload.Deload -> CardDetail.Deload(percent(payload.volumeFactor), percent(payload.loadFactor))
            is AdvicePayload.ReEntry -> CardDetail.ReEntry(percent(payload.loadFactor))
            is AdvicePayload.WeakPoint -> CardDetail.WeakPoint(
                ratio = ratio(payload.lift, stats, date, flaggedOnly = payload.missPoint != null),
                missPoint = payload.missPoint,
                muscles = AccessoryMap.muscles(payload.lift, payload.missPoint),
                additions = weakPointAdditions(payload, plan, exercises),
            )
            is AdvicePayload.Target -> null
        }

    /**
     * The ratio that explains why [lift] was flagged: the first of its pairs that is out of range on
     * the side that makes [lift] the weak one. Unless [flaggedOnly], falls back to the lift's first
     * pair with figures, so a ratio flag always has its bar.
     */
    private fun ratio(lift: MainLift, stats: HistoryStats, date: LocalDate, flaggedOnly: Boolean): RatioUi? {
        val known = PAIRS.filter { lift == it.numerator || lift == it.denominator }.mapNotNull { pair ->
            val num = stats.currentE1rm(pair.numerator, date) ?: return@mapNotNull null
            val den = stats.currentE1rm(pair.denominator, date)?.takeIf { it > 0.0 } ?: return@mapNotNull null
            RatioUi(pair.numerator, pair.denominator, percent(num / den), percent(pair.range.start), percent(pair.range.endInclusive))
        }
        val flagged = known.firstOrNull {
            if (lift == it.numerator) it.valuePct < it.lowPct else it.valuePct > it.highPct
        }
        return flagged ?: known.firstOrNull().takeUnless { flaggedOnly }
    }

    /**
     * The first two options for the weak point that are not in the plan yet, dosed like a stall card's. An option is the
     * lifter's row of that name, else the catalogue entry, whose row Apply adds (strict press accessories are not seeded).
     */
    private fun weakPointAdditions(payload: AdvicePayload.WeakPoint, plan: List<PlanItem>, exercises: List<Exercise>): List<AccessoryPrescription> {
        val planned = plan.map { it.exerciseId }.toSet()
        return AccessoryMap.options(payload.lift, payload.missPoint)
            .mapNotNull { option ->
                val row = exercises.find(option.exerciseName)
                val name = row?.name ?: Library.find(option.exerciseName)?.name ?: return@mapNotNull null
                if (row != null && row.id in planned) null else name to option.heavy
            }
            .take(WEAK_POINT_ADDITIONS)
            .map { (name, heavy) -> if (heavy) AccessoryPrescription(name, 3, 3, 5) else AccessoryPrescription(name, 3, 8, 10) }
    }

    /** Carries out the advice: accessories go into the plan; a deload or re-entry only needs to be accepted. */
    fun apply(adviceId: Long) = once(adviceId) {
        val target = advice.open().first().firstOrNull { it.id == adviceId } ?: return@once
        val message = when (val payload = target.payload) {
            is AdvicePayload.Accessories -> addToPlan(target.lift, payload.items)
            is AdvicePayload.WeakPoint -> addToPlan(payload.lift, weakPointAdditions(payload, training.plan(settings.settings.first().trainingMode).first(), training.exercises().first()))
            is AdvicePayload.Deload -> "Deload week on — targets lightened for 7 days"
            is AdvicePayload.ReEntry -> "Easing back in for 7 days"
            is AdvicePayload.Target -> return@once
        }
        advice.setStatus(adviceId, AdviceStatus.APPLIED)
        _messages.send(message)
    }

    fun dismiss(adviceId: Long) = once(adviceId) { advice.setStatus(adviceId, AdviceStatus.DISMISSED) }

    private fun once(adviceId: Long, block: suspend () -> Unit) {
        if (adviceId in busy.value) return
        busy.update { it + adviceId }
        viewModelScope.launch {
            // Kept hidden on success: the stored status reaches the feed a moment after this returns.
            try {
                block()
            } catch (e: CancellationException) {
                busy.update { it - adviceId }
                throw e
            } catch (e: Exception) {
                // A failed write must not crash the app: the card comes back and the lifter can try again.
                busy.update { it - adviceId }
                _messages.send(UPDATE_FAILED)
            }
        }
    }

    /**
     * Adds the prescriptions to the day that trains [lift], skipping any already there and any name that is neither a
     * row nor in the catalogue. A catalogue exercise without a row gets one, in the same transaction as the plan items.
     * Returns what to tell the lifter.
     */
    private suspend fun addToPlan(lift: MainLift?, prescriptions: List<AccessoryPrescription>): String {
        val exercises = training.exercises().first()
        val current = settings.settings.first()
        // The side the app is on: its own plan and days.
        val plan = training.plan(current.trainingMode).first()
        val day = dayFor(lift, plan, exercises, current)
        val onDay = plan.filter { it.weekday == day }
        val taken = onDay.map { it.exerciseId }.toMutableSet()
        var order = (onDay.maxOfOrNull { it.order } ?: -1) + 1
        val added = training.inTransaction {
            var count = 0
            for (p in prescriptions) {
                val exerciseId = exercises.find(p.exerciseName)?.id
                    ?: Library.find(p.exerciseName)?.let { training.resolveCatalogue(it.name) }
                    ?: continue
                if (!taken.add(exerciseId)) continue
                training.savePlanItem(PlanItem(0, day, exerciseId, sets = p.sets, reps = p.repsHigh, targetRpe = current.defaultRpe, order = order++, mode = current.trainingMode))
                count++
            }
            count
        }
        val dayName = day.toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
        return when (added) {
            0 -> "Already in your plan on $dayName"
            1 -> "Added 1 exercise to $dayName"
            else -> "Added $added exercises to $dayName"
        }
    }

    /** The earliest weekday that already trains the lift; else the first training day; else Monday. */
    private fun dayFor(lift: MainLift?, plan: List<PlanItem>, exercises: List<Exercise>, settings: AppSettings): DayOfWeek {
        val mainIds = exercises.filter { it.isMainLift && it.mainLift == lift }.map { it.id }.toSet()
        return plan.filter { it.exerciseId in mainIds }.minOfOrNull { it.weekday }
            ?: settings.activeTrainingDays.minOrNull()
            ?: DayOfWeek.MONDAY
    }

    private fun List<Exercise>.find(name: String): Exercise? = firstOrNull { !it.archived && it.name.equals(name, ignoreCase = true) }

    private class LiftPair(val numerator: MainLift, val denominator: MainLift, val range: ClosedFloatingPointRange<Double>)

    companion object {
        private val FEED_ORDER = listOf(AdviceType.DELOAD, AdviceType.RE_ENTRY, AdviceType.STALL, AdviceType.WEAK_POINT)
        private const val TREND_WEEKS = 6
        private const val WEAK_POINT_ADDITIONS = 2
        private const val UPDATE_FAILED = "Couldn't update — try again"

        private val PAIRS = listOf(
            LiftPair(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
            LiftPair(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
        )

        private fun percent(factor: Double) = (factor * 100).roundToInt()

        fun factory(
            training: TrainingRepository,
            advice: AdviceRepository,
            settings: SettingsRepository,
            today: () -> LocalDate = ::systemToday,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { CoachViewModel(training, advice, settings, today) } }
    }
}

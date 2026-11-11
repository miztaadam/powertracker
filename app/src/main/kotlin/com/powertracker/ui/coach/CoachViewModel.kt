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
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
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
import java.time.DayOfWeek
import java.time.LocalDate
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

data class CoachState(val loading: Boolean = true, val cards: List<CoachCard> = emptyList())

class CoachViewModel(
    private val training: TrainingRepository,
    private val advice: AdviceRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-line confirmations for a snackbar ("Added 2 exercises to Wednesday"). */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /**
     * Advice being applied or dismissed. Its card leaves the feed at once, so it cannot be tapped
     * twice and does not redraw while its accessories land in the plan; it comes back if the work fails.
     */
    private val busy = MutableStateFlow(emptySet<Long>())

    val state: StateFlow<CoachState> =
        combine(advice.open(), training.history(), training.plan(), training.exercises(), busy) { open, history, plan, exercises, busy ->
            val stats = HistoryStats(history)
            val date = today()
            val cards = open
                .filter { it.type in FEED_ORDER && it.id !in busy }
                .sortedWith(compareBy<Advice> { FEED_ORDER.indexOf(it.type) }.thenByDescending { it.createdAt }.thenByDescending { it.id })
                .mapNotNull { a -> detail(a, stats, date, plan, exercises)?.let { CoachCard(a, it) } }
            CoachState(loading = false, cards = cards)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachState())

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

    /** The first two options for the weak point that are in the library and not in the plan yet. */
    private fun weakPointAdditions(payload: AdvicePayload.WeakPoint, plan: List<PlanItem>, exercises: List<Exercise>): List<AccessoryPrescription> {
        val planned = plan.map { it.exerciseId }.toSet()
        return AccessoryMap.options(payload.lift, payload.missPoint)
            .mapNotNull { option -> exercises.find(option.exerciseName)?.takeIf { it.id !in planned }?.let { it to option.heavy } }
            .take(WEAK_POINT_ADDITIONS)
            .map { (exercise, heavy) -> if (heavy) AccessoryPrescription(exercise.name, 3, 5, 5) else AccessoryPrescription(exercise.name, 3, 10, 10) }
    }

    /** Carries out the advice: accessories go into the plan; a deload or re-entry only needs to be accepted. */
    fun apply(adviceId: Long) = once(adviceId) {
        val target = advice.open().first().firstOrNull { it.id == adviceId } ?: return@once
        val message = when (val payload = target.payload) {
            is AdvicePayload.Accessories -> addToPlan(target.lift, payload.items)
            is AdvicePayload.WeakPoint -> addToPlan(payload.lift, weakPointAdditions(payload, training.plan().first(), training.exercises().first()))
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
            } catch (e: Exception) {
                busy.update { it - adviceId }
                throw e
            }
        }
    }

    /** Adds the prescriptions to the day that trains [lift], skipping any already there. Returns what to tell the lifter. */
    private suspend fun addToPlan(lift: MainLift?, prescriptions: List<AccessoryPrescription>): String {
        val exercises = training.exercises().first()
        val plan = training.plan().first()
        val current = settings.settings.first()
        val day = dayFor(lift, plan, exercises, current)
        val onDay = plan.filter { it.weekday == day }
        val taken = onDay.map { it.exerciseId }.toMutableSet()
        var order = (onDay.maxOfOrNull { it.order } ?: -1) + 1
        var added = 0
        for (p in prescriptions) {
            val exercise = exercises.find(p.exerciseName) ?: continue
            if (!taken.add(exercise.id)) continue
            training.savePlanItem(PlanItem(0, day, exercise.id, sets = p.sets, reps = p.repsHigh, targetRpe = current.defaultRpe, order = order++))
            added++
        }
        val dayName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
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
            ?: settings.trainingDays.minOrNull()
            ?: DayOfWeek.MONDAY
    }

    private fun List<Exercise>.find(name: String): Exercise? = firstOrNull { !it.archived && it.name.equals(name, ignoreCase = true) }

    private class LiftPair(val numerator: MainLift, val denominator: MainLift, val range: ClosedFloatingPointRange<Double>)

    companion object {
        private val FEED_ORDER = listOf(AdviceType.DELOAD, AdviceType.RE_ENTRY, AdviceType.STALL, AdviceType.WEAK_POINT)
        private const val TREND_WEEKS = 6
        private const val WEAK_POINT_ADDITIONS = 2

        private val PAIRS = listOf(
            LiftPair(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
            LiftPair(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
        )

        private fun percent(factor: Double) = (factor * 100).roundToInt()

        fun factory(
            training: TrainingRepository,
            advice: AdviceRepository,
            settings: SettingsRepository,
            today: () -> LocalDate = LocalDate::now,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { CoachViewModel(training, advice, settings, today) } }
    }
}

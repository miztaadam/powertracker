package com.powertracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.progress.Pr
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.progress.weekStart
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.platform.systemToday
import com.powertracker.ui.coach.RatioUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** How far back the charts look. [weeks] is null for everything. */
enum class ChartRange(val label: String, val weeks: Int?) {
    WEEKS_8("8 wk", 8),
    MONTHS_6("6 mo", 26),
    YEAR_1("1 yr", 52),
    ALL("All", null),
}

/** A lift's current e1RM and how it moved over the last four weeks; null where there is nothing to compare. */
data class LiftSummary(val lift: MainLift, val e1rmKg: Double?, val changeKg: Double?)

data class PrUi(val lift: MainLift, val pr: Pr)

/** [weeks] is kg lifted (weight × reps) in each of the last eight weeks, oldest first; the last one is this week. */
data class VolumeUi(val lift: MainLift, val weeks: List<Float>) {
    val thisWeekKg: Double get() = weeks.lastOrNull()?.toDouble() ?: 0.0
}

data class ProgressState(
    val loading: Boolean = true,
    /** False until a main-lift set has been completed: the screen then shows only a hint. */
    val hasData: Boolean = false,
    val range: ChartRange = ChartRange.MONTHS_6,
    val totalKg: Double? = null,
    val totalChangeKg: Double? = null,
    val lifts: List<LiftSummary> = emptyList(),
    /** The Mondays the e1RM chart spans, one per week with no gaps. */
    val weeks: List<LocalDate> = emptyList(),
    /** Weekly best e1RM per lift to the half kilo, one entry per week in [weeks]; null for a week without that lift. */
    val e1rm: Map<MainLift, List<Float?>> = emptyMap(),
    /** Squat + bench + deadlift weekly best for the weeks in range that have all three. */
    val totalTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val bodyweight: List<Pair<LocalDate, Double>> = emptyList(),
    /** Each lift's standing record, newest first. */
    val prs: List<PrUi> = emptyList(),
    val volumeFrom: LocalDate? = null,
    val volume: List<VolumeUi> = emptyList(),
    val ratios: List<RatioUi> = emptyList(),
)

/** An estimate is not worth more than half a kilo of precision. */
internal fun Double.toHalfKg(): Double = (this * 2).roundToLong() / 2.0

class ProgressViewModel(
    training: TrainingRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    private val range = MutableStateFlow(ChartRange.MONTHS_6)

    val state: StateFlow<ProgressState> =
        combine(training.history(), range) { history, range -> build(history, range, today()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun setRange(value: ChartRange) {
        range.value = value
    }

    companion object {
        private const val CHANGE_DAYS = 28L
        private const val VOLUME_WEEKS = 8

        private val RATIOS = listOf(
            Triple(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
            Triple(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
        )

        private fun percent(factor: Double) = (factor * 100).roundToInt()

        /** Everything the screen shows, from one snapshot of history. */
        internal fun build(history: TrainingHistory, range: ChartRange, today: LocalDate): ProgressState {
            val hasData = history.sessions.any { s ->
                s.sets.any { set -> !set.failed && history.exercises[set.exerciseId]?.isMainLift == true }
            }
            if (!hasData) return ProgressState(loading = false, hasData = false, range = range)

            val stats = HistoryStats(history)
            val before = today.minus(CHANGE_DAYS, DateTimeUnit.DAY)
            val current = MainLift.entries.associateWith { stats.currentE1rm(it, today) }
            val lifts = MainLift.entries.map { lift ->
                val now = current[lift]?.toHalfKg()
                val then = stats.currentE1rm(lift, before)?.toHalfKg()
                LiftSummary(lift, now, if (now != null && then != null) now - then else null)
            }
            val total = ProgressMath.total(stats, today)?.toHalfKg()
            val totalBefore = ProgressMath.total(stats, before)?.toHalfKg()

            // One continuous run of weeks, so time is to scale even when a week was missed.
            val trends = MainLift.entries.associateWith { lift -> stats.weeklyTrend(lift, today).associate { it.weekStart to it.e1rmKg } }
            val thisWeek = weekStart(today)
            val firstWeek = trends.values.flatMap { it.keys }.minOrNull() ?: thisWeek
            val rangeStart = range.weeks?.let { thisWeek.minus(it - 1, DateTimeUnit.WEEK) }
            val from = if (rangeStart != null && rangeStart > firstWeek) rangeStart else firstWeek
            val weeks = generateSequence(from) { it.plus(1, DateTimeUnit.WEEK) }.takeWhile { it <= thisWeek }.toList()

            val bodyweight = history.sessions
                .filter { it.bodyweightKg != null && (rangeStart == null || it.date >= rangeStart) && it.date <= today }
                .sortedBy { it.date }
                .associate { it.date to it.bodyweightKg!! } // the last session of a day wins
                .toList()

            val volumeWeeks = List(VOLUME_WEEKS) { thisWeek.minus(VOLUME_WEEKS - 1 - it, DateTimeUnit.WEEK) }
            val volume = MainLift.entries.map { lift ->
                val byWeek = ProgressMath.weeklyVolume(history, lift).toMap()
                VolumeUi(lift, volumeWeeks.map { (byWeek[it] ?: 0.0).toFloat() })
            }

            val ratios = RATIOS.mapNotNull { (numerator, denominator, typical) ->
                val num = current[numerator] ?: return@mapNotNull null
                val den = current[denominator]?.takeIf { it > 0.0 } ?: return@mapNotNull null
                RatioUi(numerator, denominator, percent(num / den), percent(typical.start), percent(typical.endInclusive))
            }

            return ProgressState(
                loading = false,
                hasData = true,
                range = range,
                totalKg = total,
                totalChangeKg = if (total != null && totalBefore != null) total - totalBefore else null,
                lifts = lifts,
                weeks = weeks,
                e1rm = trends.mapValues { (_, byWeek) -> weeks.map { byWeek[it]?.toHalfKg()?.toFloat() } },
                totalTrend = weeks.mapNotNull { week ->
                    val points = trends.values.map { it[week] ?: return@mapNotNull null }
                    week to points.sum()
                },
                bodyweight = bodyweight,
                prs = MainLift.entries
                    .mapNotNull { lift -> ProgressMath.prs(history, lift).lastOrNull()?.let { PrUi(lift, it) } }
                    .sortedByDescending { it.pr.date },
                volumeFrom = volumeWeeks.first(),
                volume = volume,
                ratios = ratios,
            )
        }

        fun factory(training: TrainingRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { ProgressViewModel(training, today) } }
    }
}

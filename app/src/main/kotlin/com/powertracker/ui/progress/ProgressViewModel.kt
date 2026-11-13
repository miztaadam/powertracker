package com.powertracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.progress.Pr
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.progress.weekStart
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.progress.BodybuildingProgress
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.platform.systemToday
import com.powertracker.ui.coach.RatioUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /** The strict press setting: it is a fourth lift on the screen and in the total. */
    val showStrictPress: Boolean = false,
    /** The shown lifts' current e1RMs added up; null until every one of them has one. */
    val totalKg: Double? = null,
    val totalChangeKg: Double? = null,
    val lifts: List<LiftSummary> = emptyList(),
    /** The Mondays the e1RM chart spans, one per week with no gaps. */
    val weeks: List<LocalDate> = emptyList(),
    /** Weekly best e1RM per lift to the half kilo, one entry per week in [weeks]; null for a week without that lift. */
    val e1rm: Map<MainLift, List<Float?>> = emptyMap(),
    /** The shown lifts' weekly bests added up, for the weeks in range that have every one of them. */
    val totalTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val bodyweight: List<Pair<LocalDate, Double>> = emptyList(),
    /** Each lift's standing record, newest first. */
    val prs: List<PrUi> = emptyList(),
    val volumeFrom: LocalDate? = null,
    val volume: List<VolumeUi> = emptyList(),
    val ratios: List<RatioUi> = emptyList(),
    /** The side the app is on. The charts, records and total above belong to the powerlifting side. */
    val mode: TrainingMode = TrainingMode.POWERLIFTING,
    /** The bodybuilding side: the plan's exercises with their last four sessions' best sets (spec §7.6). */
    val trends: List<ExerciseTrend> = emptyList(),
)

/** An estimate is not worth more than half a kilo of precision. */
internal fun Double.toHalfKg(): Double = (this * 2).roundToLong() / 2.0

class ProgressViewModel(
    training: TrainingRepository,
    showStrictPress: Flow<Boolean>,
    mode: Flow<TrainingMode> = flowOf(TrainingMode.POWERLIFTING),
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    private val range = MutableStateFlow(ChartRange.MONTHS_6)

    val state: StateFlow<ProgressState> =
        combine(training.history(), range, showStrictPress, mode, training.plan(TrainingMode.BODYBUILDING)) { history, range, press, side, plan ->
            when (side) {
                TrainingMode.POWERLIFTING -> build(history, range, today(), press)
                TrainingMode.BODYBUILDING -> buildBodybuilding(history, plan, today())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun setRange(value: ChartRange) {
        range.value = value
    }

    companion object {
        private const val CHANGE_DAYS = 28L
        private const val VOLUME_WEEKS = 8

        private val SBD = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)

        private val RATIOS = listOf(
            Triple(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
            Triple(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
        )

        private fun percent(factor: Double) = (factor * 100).roundToInt()

        /** The bodybuilding side's list: one row per exercise in its plan, in plan order. */
        internal fun buildBodybuilding(history: TrainingHistory, plan: List<com.powertracker.domain.model.PlanItem>, today: LocalDate): ProgressState {
            val trends = BodybuildingProgress.rows(history, plan, today)
            return ProgressState(
                loading = false,
                hasData = trends.isNotEmpty(),
                mode = TrainingMode.BODYBUILDING,
                trends = trends,
            )
        }

        /**
         * Everything the screen shows, from one snapshot of history. Strict press is a fourth lift
         * throughout when [showStrictPress] is on; when it is off its sets change nothing here.
         * The charts, records and total belong to the powerlifting side: bodybuilding sessions do not touch them
         * (bodybuilding mode spec §5.3), so they read only powerlifting sessions.
         */
        internal fun build(all: TrainingHistory, range: ChartRange, today: LocalDate, showStrictPress: Boolean): ProgressState {
            val history = all.on(TrainingMode.POWERLIFTING)
            val shown = if (showStrictPress) SBD + MainLift.STRICT_PRESS else SBD
            val hasData = history.sessions.any { s ->
                s.sets.any { set -> !set.failed && history.exercises[set.exerciseId]?.let { it.isMainLift && it.mainLift in shown } == true }
            }
            if (!hasData) return ProgressState(loading = false, hasData = false, range = range, showStrictPress = showStrictPress)

            val stats = HistoryStats(history)
            val before = today.minus(CHANGE_DAYS, DateTimeUnit.DAY)
            val current = shown.associateWith { stats.currentE1rm(it, today) }
            val lifts = shown.map { lift ->
                val now = current[lift]?.toHalfKg()
                val then = stats.currentE1rm(lift, before)?.toHalfKg()
                LiftSummary(lift, now, if (now != null && then != null) now - then else null)
            }
            val total = ProgressMath.total(stats, today, includeStrictPress = showStrictPress)?.toHalfKg()
            val totalBefore = ProgressMath.total(stats, before, includeStrictPress = showStrictPress)?.toHalfKg()

            // One continuous run of weeks, so time is to scale even when a week was missed.
            val trends = shown.associateWith { lift -> stats.weeklyTrend(lift, today).associate { it.weekStart to it.e1rmKg } }
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
            val volume = shown.map { lift ->
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
                showStrictPress = showStrictPress,
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
                prs = shown
                    .mapNotNull { lift -> ProgressMath.prs(history, lift).lastOrNull()?.let { PrUi(lift, it) } }
                    .sortedByDescending { it.pr.date },
                volumeFrom = volumeWeeks.first(),
                volume = volume,
                ratios = ratios,
            )
        }

        fun factory(training: TrainingRepository, settings: SettingsRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { ProgressViewModel(training, settings.showStrictPress, settings.settings.map { it.trainingMode }, today) } }
    }
}

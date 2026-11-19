package com.powertracker.domain.guide

import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.coach.ProgressionRule
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingMode
import kotlin.math.roundToInt

/*
 * The guide's rule table: which side's coach runs each rule, read from the callers. `Coaching` is the one entry both
 * apps call: `targetsFor` asks `Coach` (powerlifting) or `BodybuildingCoach` (bodybuilding); `evaluate` runs
 * `Coach.evaluate` for both sides and, on bodybuilding, keeps only RE_ENTRY and DELOAD, then adds
 * `PhysiqueCoach.stuck`; `checkInDue` and `afterCheckIn` are bodybuilding only; `afterSkip` (`SkipCoach`) runs for a
 * skip on either side. A rule a side's coach does not run has no sentence on that side.
 *
 * | Rule          | Code path                                              | Side(s)      | Constant(s)                                      | The guide's sentence (screen)                         |
 * |---------------|--------------------------------------------------------|--------------|--------------------------------------------------|-------------------------------------------------------|
 * | Progression   | Coach.targetsFor -> ProgressionRule.target / change    | powerlifting | ProgressionRule.STEP_KG 2.5, BIG_STEP_KG 5.0,     | 5: "Hit your target effort and the next target adds   |
 * |               |                                                        |              | MAX_INCREASE 1.05 (HARD_SCALE 0.975 and           | 2.5 kg." / "Squat or deadlift much easier: 5 kg.      |
 * |               |                                                        |              | FAILED_SCALE 0.95: "a little less", no number)    | Never more than 5% at once." / "Harder than planned:  |
 * |               |                                                        |              |                                                  | the same weight, or a little less."                   |
 * | Progression   | BodybuildingCoach.targetsFor (reps first, then weight) | bodybuilding | BodybuildingCoach.INCREASE 0.02                  | 5: "Keep the weight and beat last time's reps." /     |
 * |               |                                                        |              |                                                  | "... the weight goes up about 2%." / "Then reps start |
 * |               |                                                        |              |                                                  | again at the bottom."                                 |
 * | Deload        | Coach.evaluate -> FatigueRule; the clamp in both       | both         | AdvicePayload.Deload volumeFactor 0.6,           | 6: "Fatigue: lighter week, about 60% of sets at about |
 * |               | coaches' targetsFor; cooled down by                    |              | loadFactor 0.9; Coach.DELOAD_COOLDOWN_DAYS 21     | 90% of the weight." ("about", as the card says: sets  |
 * |               | Coach.DELOAD_COOLDOWN_DAYS                             |              | (private)                                        | round, the weight rounds down to a loadable one; 21   |
 * |               |                                                        |              |                                                  | not shown: private, it only follows an applied deload)|
 * | Easing back   | Coach.evaluate -> reEntry (HistoryStats                | both         | Coach.GAP_DAYS 14 (= HistoryStats.GAP_DAYS),     | 6: "14 days off: about 85% of the weight."            |
 * |               | .returnDateAfterGap); the first-day-back clamp in both |              | AdvicePayload.ReEntry loadFactor 0.85            |                                                       |
 * |               | coaches' targetsFor                                    |              |                                                  |                                                       |
 * | Pain hold     | SkipCoach.held, read by both coaches' targetsFor; the  | both         | none: until the exercise is next logged          | 6: "Pain skip: no added weight until done again."     |
 * |               | card from Coaching.afterSkip -> SkipCoach.afterSkip    |              |                                                  |                                                       |
 * | Skip reasons  | Coaching.afterSkip -> SkipCoach (busy, repeat cards)   | both         | none shown                                       | 4: "The coach uses the reason."                       |
 * | Stall         | Coach.evaluate -> StallRule (AdvicePayload.Accessories:| powerlifting | CoachSettings.stallWeeks (a setting, no number)  | 6, one line for both: "Stalled or lagging lift: extra |
 * |               | they reach the plan only when the card is applied)     |              |                                                  | exercises suggested." ("suggested": nothing changes   |
 * | Weak point    | Coach.evaluate -> WeakPointRule (AdvicePayload         | powerlifting | WeakPointRule ratios (not shown)                 | until the lifter applies the card; then both apps add |
 * |               | .WeakPoint: the apps add its first two accessories,    |              |                                                  | the exercises to the plan); no sentence on            |
 * |               | not yet planned, when the card is applied)             |              |                                                  | bodybuilding                                          |
 * | Swap (stuck)  | Coaching.evaluate -> PhysiqueCoach.stuck               | bodybuilding | PhysiqueCoach.STUCK_DAYS 21                      | 6: "Stuck for 21 days: swaps to try."                 |
 * | Check-in      | Coaching.checkInDue / afterCheckIn -> PhysiqueCoach    | bodybuilding | PhysiqueCoach.CHECK_IN_DAYS 28                   | 7: "Every 28 days a check-in asks which muscles lag.  |
 * |               |                                                        |              |                                                  | The coach then suggests more work for them."          |
 *
 * Screen 6's lines are in the table's order (deload, easing back, pain hold, then stall and weak point, or swap), as
 * are GuideSample.advice's cards: one line per card (powerlifting's last card is the stall one). The copy says what the coach does, not why, and names no source.
 */

enum class GuideScreenId { WELCOME, SIDE, PLAN, TODAY, TARGETS, HOLDS_BACK, MONTHS_LATER, YOUR_TURN }

/** One sentence of a screen; [constants] names each code constant a number in [text] comes from. */
data class GuideLine(val text: String, val constants: List<String>)

/** One screen's copy for one side; [example] is true when its picture shows sample numbers (labelled "Example"). */
data class GuideScreen(val id: GuideScreenId, val title: String, val lines: List<GuideLine>, val example: Boolean)

/** What each picture shows at each step, in order within each screen. The apps draw these; they don't invent steps. */
enum class GuideStep {
    WELCOME_MARK, WELCOME_SIDES,
    SIDE_LOOP,
    PLAN_EMPTY, PLAN_TEMPLATES, PLAN_DRAFT, PLAN_SWAP, PLAN_FILLED,
    TODAY_EMPTY, TODAY_WEIGHT, TODAY_REPS, TODAY_RPE, TODAY_TICKED, TODAY_SKIP,

    /** Repeated once per [GuideSample.targets] entry. */
    TARGETS_SESSION,

    /** Repeated once per [GuideSample.advice] entry. */
    HOLDS_CARD,
    MONTHS_CHART, MONTHS_BESTS, MONTHS_ENDING,
    TURN_BUTTONS,
}

/**
 * Where a picture is at one moment: step [stepIndex] ([step]) of its screen, [progress] (0 to 1, eased) through the move
 * into it, 1 while it holds; [done] once the last step has settled, which it then holds for good.
 */
data class GuideFrame(val stepIndex: Int, val step: GuideStep, val progress: Double, val done: Boolean)

/** The guide: its 8 screens, their copy for each side, each picture's steps, and their time. */
object Guide {
    /** How long a picture step holds once its move has ended, in milliseconds. */
    const val STEP_MS: Int = 900

    /**
     * A step's move, in milliseconds: the web's own layout moves (280 ms); Android's move spring settles a card-sized
     * move in about as long. Each step after the first moves for this long, then holds for [STEP_MS].
     */
    const val MOVE_MS: Int = 280

    /** A row that is new in a step fades in (or a row that has gone fades out) this long: the apps' fades. */
    const val FADE_MS: Int = 150

    /** The move's curve: the web's `cubic-bezier(0.2, 0.8, 0.2, 1)` (its layout moves), as control points. */
    private const val EASE_X1 = 0.2
    private const val EASE_Y1 = 0.8
    private const val EASE_X2 = 0.2
    private const val EASE_Y2 = 1.0

    private const val SPAN_MS = MOVE_MS + STEP_MS

    /** When step [index] starts: its move, or for the first step the picture itself. */
    fun stepStartMs(index: Int): Int = index * SPAN_MS

    /** When step [index] has settled: its move over (the first step has none). */
    fun settledMs(index: Int): Int = if (index <= 0) 0 else stepStartMs(index) + MOVE_MS

    /** When a picture of [count] steps is done: its last step settled. */
    fun doneMs(count: Int): Int = settledMs(count - 1)

    /** The move's curve at [x], 0 to 1 of its time: how far it has gone, 0 to 1. */
    fun ease(x: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        fun bezier(a: Double, b: Double, t: Double): Double {
            val u = 1 - t
            return 3 * u * u * t * a + 3 * u * t * t * b + t * t * t
        }
        // The curve's x rises with t: find the t whose x is [x] by halving.
        var lo = 0.0
        var hi = 1.0
        repeat(40) {
            val t = (lo + hi) / 2
            if (bezier(EASE_X1, EASE_X2, t) < x) lo = t else hi = t
        }
        return bezier(EASE_Y1, EASE_Y2, (lo + hi) / 2)
    }

    /** Where [id]'s picture on [mode]'s side is [tMs] after it started. */
    fun frame(id: GuideScreenId, mode: TrainingMode, tMs: Int): GuideFrame = frame(steps(id, mode), tMs)

    /** Where a picture of [steps] is [tMs] after it started. */
    fun frame(steps: List<GuideStep>, tMs: Int): GuideFrame {
        val t = maxOf(0, tMs)
        val index = minOf(t / SPAN_MS, steps.lastIndex)
        val progress = if (index == 0) 1.0 else ease((t - stepStartMs(index)).toDouble() / MOVE_MS)
        return GuideFrame(index, steps[index], progress, done = index == steps.lastIndex && t >= settledMs(index))
    }

    /** The 8 screens, in order. */
    val SCREENS: List<GuideScreenId> = GuideScreenId.entries.toList()

    // The names of the constants the copy's numbers come from.
    private const val STEP_KG = "ProgressionRule.STEP_KG"
    private const val BIG_STEP_KG = "ProgressionRule.BIG_STEP_KG"
    private const val MAX_INCREASE = "ProgressionRule.MAX_INCREASE"
    private const val BB_INCREASE = "BodybuildingCoach.INCREASE"
    private const val DELOAD_VOLUME = "AdvicePayload.Deload.volumeFactor"
    private const val DELOAD_LOAD = "AdvicePayload.Deload.loadFactor"
    private const val GAP_DAYS = "Coach.GAP_DAYS"
    private const val RE_ENTRY_LOAD = "AdvicePayload.ReEntry.loadFactor"
    private const val STUCK_DAYS = "PhysiqueCoach.STUCK_DAYS"
    private const val CHECK_IN_DAYS = "PhysiqueCoach.CHECK_IN_DAYS"

    /** A fraction as a whole percent: 0.6 -> "60". */
    private fun percent(ratio: Double): String = (ratio * 100).roundToInt().toString()

    /** A factor's rise over 1 as a whole percent: 1.05 -> "5". */
    private fun rise(factor: Double): String = ((factor - 1) * 100).roundToInt().toString()

    private fun kg(value: Double): String = ProgressionRule.fmt(value)

    private fun line(text: String, vararg constants: String) = GuideLine(text, constants.toList())

    fun screen(id: GuideScreenId, mode: TrainingMode): GuideScreen {
        val pl = mode == TrainingMode.POWERLIFTING
        return when (id) {
            GuideScreenId.WELCOME -> GuideScreen(
                id, "Welcome to Grit",
                listOf(
                    line("Two sides, powerlifting and bodybuilding, in one log."),
                    line("A coach sets each next target from what you actually lift."),
                ),
                example = false,
            )
            GuideScreenId.SIDE -> GuideScreen(
                id, "Pick your side",
                listOf(
                    line("Powerlifting is built around squat, bench and deadlift. Bodybuilding works each muscle in rep ranges."),
                    line("You can switch any time in Settings."),
                ),
                example = false,
            )
            GuideScreenId.PLAN -> GuideScreen(
                id, "Your plan",
                listOfNotNull(
                    line("Choose a template by days a week, then pick your weekdays."),
                    line("Swap an exercise you'd rather not do. Nothing is saved until you tap Use this plan."),
                    if (pl) null else line("Or tap Build my plan to make your own."),
                ),
                example = true,
            )
            GuideScreenId.TODAY -> GuideScreen(
                id, "Today",
                listOf(
                    line("Enter the weight, the reps and how hard it felt (RPE), then tick the set."),
                    line("Not doing an exercise? Choose Skip today from its menu and say why. The coach uses the reason."),
                ),
                example = true,
            )
            GuideScreenId.TARGETS -> GuideScreen(
                id, "How targets move",
                if (pl) {
                    listOf(
                        line("Hit your target effort and the next target adds ${kg(ProgressionRule.STEP_KG)} kg.", STEP_KG),
                        line(
                            "Squat or deadlift much easier: ${kg(ProgressionRule.BIG_STEP_KG)} kg. " +
                                "Never more than ${rise(ProgressionRule.MAX_INCREASE)}% at once.",
                            BIG_STEP_KG, MAX_INCREASE,
                        ),
                        line("Harder than planned: the same weight, or a little less."),
                    )
                } else {
                    listOf(
                        line("Keep the weight and beat last time's reps."),
                        line(
                            "When every set reaches the top of the range at or under target effort, " +
                                "the weight goes up about ${percent(BodybuildingCoach.INCREASE)}%.",
                            BB_INCREASE,
                        ),
                        line("Then reps start again at the bottom."),
                    )
                },
                example = true,
            )
            GuideScreenId.HOLDS_BACK -> GuideScreen(
                id, "Holding back",
                listOf(
                    line(
                        "Fatigue: lighter week, about ${percent(AdvicePayload.Deload().volumeFactor)}% of sets " +
                            "at about ${percent(AdvicePayload.Deload().loadFactor)}% of the weight.",
                        DELOAD_VOLUME, DELOAD_LOAD,
                    ),
                    line(
                        "${Coach.GAP_DAYS} days off: about ${percent(AdvicePayload.ReEntry().loadFactor)}% of the weight.",
                        GAP_DAYS, RE_ENTRY_LOAD,
                    ),
                    line("Pain skip: no added weight until done again."),
                    if (pl) {
                        line("Stalled or lagging lift: extra exercises suggested.")
                    } else {
                        line("Stuck for ${PhysiqueCoach.STUCK_DAYS} days: swaps to try.", STUCK_DAYS)
                    },
                ),
                example = true,
            )
            GuideScreenId.MONTHS_LATER -> GuideScreen(
                id, "Months later",
                if (pl) {
                    listOf(line("For example: the chart fills in, your bests climb, and your total grows."))
                } else {
                    listOf(
                        line("For example: the chart fills in and your bests climb."),
                        line(
                            "Every ${PhysiqueCoach.CHECK_IN_DAYS} days a check-in asks which muscles lag. " +
                                "The coach then suggests more work for them.",
                            CHECK_IN_DAYS,
                        ),
                    )
                },
                example = true,
            )
            GuideScreenId.YOUR_TURN -> GuideScreen(
                id, "Your turn",
                listOf(
                    line("Set up your week, then log your first session."),
                    line("You can replay this guide from Settings."),
                ),
                example = false,
            )
        }
    }

    /** What [id]'s picture shows, step by step, on [mode]'s side. */
    fun steps(id: GuideScreenId, mode: TrainingMode): List<GuideStep> = when (id) {
        GuideScreenId.WELCOME -> listOf(GuideStep.WELCOME_MARK, GuideStep.WELCOME_SIDES)
        GuideScreenId.SIDE -> listOf(GuideStep.SIDE_LOOP)
        GuideScreenId.PLAN -> listOf(GuideStep.PLAN_EMPTY, GuideStep.PLAN_TEMPLATES, GuideStep.PLAN_DRAFT, GuideStep.PLAN_SWAP, GuideStep.PLAN_FILLED)
        GuideScreenId.TODAY -> listOf(
            GuideStep.TODAY_EMPTY, GuideStep.TODAY_WEIGHT, GuideStep.TODAY_REPS, GuideStep.TODAY_RPE, GuideStep.TODAY_TICKED,
            GuideStep.TODAY_SKIP,
        )
        // Counted without working the sample out: that costs a first open (the counts match, by GuideSampleTest).
        GuideScreenId.TARGETS -> List(GuideSample.targetCount(mode)) { GuideStep.TARGETS_SESSION }
        GuideScreenId.HOLDS_BACK -> List(GuideSample.ADVICE_CARDS) { GuideStep.HOLDS_CARD }
        GuideScreenId.MONTHS_LATER -> listOf(GuideStep.MONTHS_CHART, GuideStep.MONTHS_BESTS, GuideStep.MONTHS_ENDING)
        GuideScreenId.YOUR_TURN -> listOf(GuideStep.TURN_BUTTONS)
    }
}

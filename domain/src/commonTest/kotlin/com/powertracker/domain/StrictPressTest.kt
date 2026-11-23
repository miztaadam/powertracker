package com.powertracker.domain

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.FatigueRule
import com.powertracker.domain.coach.ProgressionRule
import com.powertracker.domain.coach.StallRule
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.library.Sources
import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.liftLabel
import com.powertracker.domain.model.missPointLabel
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.seed.SeedLibrary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Strict press as the fourth main lift (strict press spec §2 and §3). */
class StrictPressTest {
    private val start = LocalDate(2026, 1, 5) // Monday, day 0 of the history builder
    private fun day(n: Int): LocalDate = start.plus(n, DateTimeUnit.DAY)
    private val settings = CoachSettings()

    // ---- model ----

    @Test
    fun enums_appendStrictPressLast() {
        assertEquals(listOf("SQUAT", "BENCH", "DEADLIFT", "STRICT_PRESS"), MainLift.entries.map { it.name })
        assertEquals(listOf("SQUAT", "BENCH", "DEADLIFT", "STRICT_PRESS", "VARIATION", "ACCESSORY"), Category.entries.map { it.name })
    }

    @Test
    fun strictPressCategory_isTheMainLift() {
        val press = Exercise(1, "Strict press", Category.STRICT_PRESS, null, emptyList(), true)
        assertEquals(MainLift.STRICT_PRESS, press.mainLift)
        assertTrue(press.isMainLift)
        val variation = Exercise(2, "Pin press overhead", Category.VARIATION, MainLift.STRICT_PRESS, emptyList(), false)
        assertEquals(MainLift.STRICT_PRESS, variation.mainLift)
        assertFalse(variation.isMainLift)
    }

    @Test
    fun liftLabels() {
        assertEquals(listOf("Squat", "Bench", "Deadlift", "Strict press"), MainLift.entries.map { liftLabel(it) })
    }

    @Test
    fun missPointLabel_perLift() {
        for (lift in listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)) {
            assertEquals(listOf("Floor", "Mid", "Lockout"), MissPoint.entries.map { missPointLabel(lift, it) }, "$lift")
        }
        assertEquals(
            listOf("Off the shoulders", "Mid", "Lockout"),
            MissPoint.entries.map { missPointLabel(MainLift.STRICT_PRESS, it) },
        )
    }

    // ---- progression targets: the bench rule, +2.5 kg ----

    private fun item(exerciseId: Long = Ids.STRICT_PRESS, weekday: DayOfWeek = DayOfWeek.MONDAY) =
        PlanItem(1, weekday, exerciseId, 3, 5, 8.0, 0)

    private fun target(h: TrainingHistory, today: LocalDate = day(14), id: Long = Ids.STRICT_PRESS): AdvicePayload.Target {
        val advice = assertNotNull(ProgressionRule.target(item(id), h.exercises.getValue(id), HistoryStats(h), settings, today))
        assertEquals(h.exercises.getValue(id).mainLift, advice.lift)
        return advice.payload as AdvicePayload.Target
    }

    private fun pressOnce(kg: Double, rpe: Double?, failed: Boolean = false) = history { session(7) { press(kg, 5, rpe = rpe, failed = failed) } }

    @Test
    fun targets_wellBelowPlan_addTwoPointFive_notFive() {
        assertEquals(62.5, target(pressOnce(60.0, 6.0)).weightKg)
        // The same path on squat adds 5 kg: strict press follows bench.
        assertEquals(155.0, target(history { session(7) { squat(150.0, 5, rpe = 6.0) } }, id = Ids.SQUAT).weightKg)
    }

    @Test
    fun targets_oneBelowPlan_addTwoPointFive() {
        val t = target(pressOnce(60.0, 7.0))
        assertEquals(62.5, t.weightKg)
        assertEquals(5, t.reps)
        assertEquals(8.0, t.rpe)
    }

    @Test
    fun targets_onPlan_addTwoPointFive() {
        assertEquals(62.5, target(pressOnce(60.0, 8.0)).weightKg)
        assertEquals(62.5, target(pressOnce(60.0, null)).weightKg) // no RPE logged: treated as on plan
    }

    @Test
    fun targets_aboveAndFailed_followTheExistingRule() {
        assertEquals(60.0, target(pressOnce(60.0, 9.0)).weightKg) // one above: repeat
        assertEquals(57.5, target(pressOnce(60.0, 10.0)).weightKg) // well above: 97.5%, on the plate grid
        assertEquals(57.5, target(pressOnce(60.0, 10.0, failed = true)).weightKg) // missed: 95%
    }

    @Test
    fun targets_recentJumpWithRisingRpe_repeats() {
        val h = history {
            session(0) { press(57.5, 5, rpe = 7.0) }
            session(7) { press(60.0, 5, rpe = 8.0) }
        }
        assertEquals(60.0, target(h).weightKg)
    }

    @Test
    fun targets_fivePercentCap_addsARepInstead() {
        // 40 + 2.5 is over the 5% cap (42): the load stays and a rep is added.
        val t = target(pressOnce(40.0, 6.0))
        assertEquals(40.0, t.weightKg)
        assertEquals(6, t.reps)
    }

    @Test
    fun targetsFor_needsMinimumHistory_likeAnyMainLift() {
        val plan = listOf(item())
        val two = history { session(0) { press(60.0, 5) }; session(7) { press(60.0, 5) } }
        assertEquals(emptyList(), Coach.targetsFor(DayOfWeek.MONDAY, two, plan, settings, day(14), emptyList()))
        val three = history { session(0) { press(60.0, 5) }; session(7) { press(60.0, 5) }; session(14) { press(60.0, 5) } }
        val advice = Coach.targetsFor(DayOfWeek.MONDAY, three, plan, settings, day(21), emptyList()).single()
        assertEquals(MainLift.STRICT_PRESS, advice.lift)
        assertEquals(62.5, (advice.payload as AdvicePayload.Target).weightKg)
    }

    // ---- stall: 3-week window, one accessory row per miss point ----

    private fun pressFlat(mondays: Int, miss: MissPoint?) =
        history { for (w in 0 until mondays) session(w * 7) { press(60.0, 5, miss = if (w >= 1) miss else null) } }

    private fun stall(h: TrainingHistory, today: LocalDate, owned: Set<String> = emptySet()) =
        StallRule.evaluate(MainLift.STRICT_PRESS, HistoryStats(h), settings, today, owned)

    private fun light(name: String) = AccessoryPrescription(name, 3, 8, 10)
    private fun heavy(name: String) = AccessoryPrescription(name, 3, 3, 5)

    @Test
    fun stall_afterThreeWeeks_notTwo() {
        assertNull(stall(pressFlat(3, MissPoint.MID), day(14)))
        val advice = assertNotNull(stall(pressFlat(4, MissPoint.MID), day(21)))
        assertEquals(AdviceType.STALL, advice.type)
        assertEquals(MainLift.STRICT_PRESS, advice.lift)
        assertEquals(3, advice.severity)
        assertEquals(3, settings.stallWeeks)
    }

    @Test
    fun stall_offTheShoulders_frontDeltsAndUpperChest() {
        val advice = assertNotNull(stall(pressFlat(4, MissPoint.FLOOR), day(21)))
        assertEquals(
            "Strict press e1RM hasn't improved for 3 weeks. You've mostly missed off the shoulders — this works the front delts and upper chest.",
            advice.reason,
        )
        assertEquals(
            AdvicePayload.Accessories(listOf(light("Seated barbell press"), light("Incline bench press"), light("Landmine press"))),
            advice.payload,
        )
    }

    @Test
    fun stall_mid_deltsAndTriceps() {
        val advice = assertNotNull(stall(pressFlat(4, MissPoint.MID), day(21)))
        assertEquals(
            "Strict press e1RM hasn't improved for 3 weeks. You've mostly missed mid-range — this works the delts and triceps.",
            advice.reason,
        )
        assertEquals(
            AdvicePayload.Accessories(listOf(light("DB shoulder press"), light("Arnold press"), light("One-arm DB press"))),
            advice.payload,
        )
    }

    @Test
    fun stall_lockout_tricepsAndUpperBack() {
        val advice = assertNotNull(stall(pressFlat(4, MissPoint.LOCKOUT), day(21)))
        assertEquals(
            "Strict press e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps and upper back.",
            advice.reason,
        )
        assertEquals(
            AdvicePayload.Accessories(listOf(heavy("Close-grip bench"), light("JM press"), light("DB overhead triceps extension"))),
            advice.payload,
        )
    }

    @Test
    fun stall_noMissPoint_fallback() {
        val advice = assertNotNull(stall(pressFlat(4, null), day(21)))
        assertEquals(
            "Strict press e1RM hasn't improved for 3 weeks. Add one set, change the rep range (5s → 3s or 8s), or swap to a variation.",
            advice.reason,
        )
        // The lift's heavy options first (only Close-grip bench is one), filled up to two from the table.
        assertEquals(AdvicePayload.Accessories(listOf(heavy("Close-grip bench"), light("Seated barbell press"))), advice.payload)
    }

    @Test
    fun accessoryMap_strictPressRowsVerbatim() {
        val lift = MainLift.STRICT_PRESS
        assertEquals(listOf("Front delts", "Upper chest"), AccessoryMap.muscles(lift, MissPoint.FLOOR))
        assertEquals(listOf("Delts", "Triceps"), AccessoryMap.muscles(lift, MissPoint.MID))
        assertEquals(listOf("Triceps", "Upper back"), AccessoryMap.muscles(lift, MissPoint.LOCKOUT))
        assertEquals(emptyList(), AccessoryMap.muscles(lift, null))
        // Every name is in the catalogue exactly as written.
        for (option in AccessoryMap.allOptions(lift)) assertEquals(option.exerciseName, Library.find(option.exerciseName)?.name)
    }

    @Test
    fun coachEvaluate_includesAStrictPressStall() {
        val advice = Coach.evaluate(pressFlat(4, MissPoint.LOCKOUT), emptyList(), settings, day(21), emptyList())
        assertEquals(listOf(Triple(AdviceType.STALL, MainLift.STRICT_PRESS, 3)), advice.map { Triple(it.type, it.lift, it.severity) })
    }

    // ---- fatigue: strict press counts as a lift ----

    private val fatigueToday = day(13)
    private fun signals(h: TrainingHistory) = FatigueRule.signals(HistoryStats(h), h, fatigueToday).map { it.text }

    @Test
    fun fatigue_rpeRisingOnSquatAndPress() {
        val h = history {
            session(7, readiness = 2) { squat(100.0, 5, rpe = 7.0); press(50.0, 5, rpe = 7.0) }
            session(9, readiness = 2) { squat(100.0, 5, rpe = 8.5); press(50.0, 5, rpe = 8.5) }
        }
        assertEquals(listOf("RPE rising at the same load on squat and strict press", "low readiness"), signals(h))
        val deload = assertNotNull(FatigueRule.evaluate(HistoryStats(h), h, fatigueToday))
        assertEquals(AdviceType.DELOAD, deload.type)
    }

    @Test
    fun fatigue_pressAloneIsOneLift() {
        val h = history {
            session(7) { press(50.0, 5, rpe = 7.0) }
            session(9) { press(50.0, 5, rpe = 8.5) }
        }
        assertEquals(emptyList(), signals(h))
    }

    @Test
    fun fatigue_failedPressRepsCount() {
        val h = history {
            session(9) { press(50.0, 5); set(Ids.STRICT_PRESS, 52.5, 3, failed = true); set(Ids.STRICT_PRESS, 52.5, 3, failed = true) }
        }
        assertEquals(listOf("2 failed reps"), signals(h))
    }

    @Test
    fun fatigue_e1rmDownOnBenchAndPress() {
        val h = history {
            session(0) { bench(100.0, 5); press(60.0, 5) }
            session(7) { bench(95.0, 5); press(55.0, 5) }
        }
        assertEquals(listOf("e1RM down on bench and strict press"), signals(h))
    }

    // ---- weak points: never in a ratio ----

    private fun weekly(weeks: Int, press: Double, pressMiss: MissPoint? = null) = history {
        for (w in 0 until weeks) {
            session(w * 7) {
                squat(170.0, 5)
                bench(110.0, 5)
                press(press, 5)
                if (pressMiss != null && w >= weeks - 3) set(Ids.STRICT_PRESS, press + 5.0, 3, failed = true, miss = pressMiss)
            }
            session(w * 7 + 2) { deadlift(200.0, 5) }
        }
    }

    @Test
    fun weakPoints_noRatioIncludesPress() {
        // Normal squat ÷ deadlift and bench ÷ squat; the press is far off any other lift either way.
        for (press in listOf(20.0, 250.0)) {
            assertEquals(emptyList(), WeakPointRule.evaluate(HistoryStats(weekly(6, press)), day(35)), "press $press")
        }
        // Ratios that do flag still name only squat, bench and deadlift, whatever the press is.
        val lowSquat = history {
            for (w in 0 until 5) {
                session(w * 7) { squat(120.0, 5); bench(80.0, 5); press(20.0, 5) }
                session(w * 7 + 2) { deadlift(200.0, 5) }
            }
        }
        val advice = WeakPointRule.evaluate(HistoryStats(lowSquat), day(28))
        assertEquals(listOf(MainLift.SQUAT), advice.map { it.lift })
        assertTrue(advice.none { "press" in it.reason }, advice.single().reason)
    }

    @Test
    fun weakPoints_repeatedPressMissStillFlags() {
        val advice = WeakPointRule.evaluate(HistoryStats(weekly(6, 60.0, MissPoint.FLOOR)), day(35)).single()
        assertEquals(MainLift.STRICT_PRESS, advice.lift)
        assertEquals(AdvicePayload.WeakPoint(MainLift.STRICT_PRESS, MissPoint.FLOOR), advice.payload)
        assertEquals("You've missed strict press off the shoulders 3 times in the last 4 weeks.", advice.reason)
    }

    // ---- total ----

    private fun allFour(press: Boolean = true, bench: Boolean = true) = history {
        session(0) {
            squat(150.0, 5)
            if (bench) bench(100.0, 5)
            if (press) press(60.0, 5)
        }
        session(2) { deadlift(180.0, 5) }
    }

    @Test
    fun total_sumsSbd_plusPressWhenIncluded() {
        val stats = HistoryStats(allFour())
        val sbd = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT).sumOf { stats.currentE1rm(it, day(3))!! }
        val press = stats.currentE1rm(MainLift.STRICT_PRESS, day(3))!!
        assertEquals(sbd, ProgressMath.total(stats, day(3), includeStrictPress = false))
        assertEquals(sbd + press, ProgressMath.total(stats, day(3), includeStrictPress = true))
    }

    @Test
    fun total_nullWhenAnIncludedLiftIsMissing() {
        val noPress = HistoryStats(allFour(press = false))
        assertNotNull(ProgressMath.total(noPress, day(3), includeStrictPress = false))
        assertNull(ProgressMath.total(noPress, day(3), includeStrictPress = true))
        val noBench = HistoryStats(allFour(bench = false))
        assertNull(ProgressMath.total(noBench, day(3), includeStrictPress = false))
        assertNull(ProgressMath.total(noBench, day(3), includeStrictPress = true))
    }

    // ---- catalogue ----

    @Test
    fun library_overheadPressIsNowStrictPress() {
        val entry = assertNotNull(Library.find("Overhead press"))
        assertEquals("Strict press", entry.name)
        for (typed in listOf("strict press", "OHP", "Military press", "overhead-press", "Standing press")) {
            assertEquals("Strict press", Library.find(typed)?.name, typed)
        }
        assertTrue(entry.aliases.containsAll(listOf("Overhead press", "OHP", "Military press")))
        assertEquals(Category.STRICT_PRESS, entry.category)
        assertEquals(Kind.COMPOUND, entry.kind)
        assertEquals(BodyArea.MAIN_LIFTS, entry.area)
        assertNull(entry.parentLift)
        assertEquals(listOf("Front delts", "Side delts", "Triceps"), entry.muscles)
        assertEquals(1, entry.guide.setup.count { it == RACK_SAFETY })
        assertTrue("strict press" in entry.guide.purpose.lowercase(), entry.guide.purpose)
        assertEquals(
            listOf(
                Sources.EXRX_BB_MILITARY_PRESS, Sources.ACE_STANDING_SHOULDER_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
                Sources.PMC_UPPER_EXTREMITY_LIFTING_INJURIES, Sources.ACE_BRACE_OR_BREATHE,
            ),
            entry.sources,
        )
        assertEquals(listOf(entry), Library.all().filter { it.category == Category.STRICT_PRESS })
        assertTrue(Library.all().none { it.name == "Overhead press" })
        assertEquals("Strict press", Library.search("overhead press").first().name)
        assertEquals("Strict press", Library.search("OHP").first().name)
        assertEquals("Strict press", Library.search("strict press").first().name)
    }

    @Test
    fun muscleGroups_everyAccessoryMapMuscleMaps() {
        val muscles = MainLift.entries.flatMap { lift -> MissPoint.entries.flatMap { AccessoryMap.muscles(lift, it) } }.toSet()
        assertEquals(emptyList(), muscles.filter { MuscleGroups.of(it) == null })
        // The strict press rows' own words.
        assertEquals(MuscleGroup.FRONT_DELTS, MuscleGroups.of("Delts"))
        assertEquals(MuscleGroup.CHEST, MuscleGroups.of("Upper chest"))
        assertEquals(MuscleGroup.FRONT_DELTS, MuscleGroups.of("Front delts"))
    }

    // ---- seed: unchanged, strict press not seeded ----

    @Test
    fun seed_isUnchanged_andHasNoStrictPress() {
        assertEquals(BASE_SEED, CoachSnapshot.seed())
        assertTrue(SeedLibrary.exercises().none { it.category == Category.STRICT_PRESS || it.name == "Strict press" })
    }

    // ---- golden scenario with a press added ----

    /** The golden history with a flat strict press on each Thursday up to the holiday. */
    private val goldenWithPress: TrainingHistory = GoldenScenario.history.let { h ->
        h.copy(
            sessions = h.sessions.map { s ->
                if (s.date.dayOfWeek == DayOfWeek.THURSDAY && s.date <= GoldenScenario.day(66)) {
                    s.copy(sets = s.sets + LoggedSet(Ids.STRICT_PRESS, 50.0, 5, 8.0, false, null, true))
                } else {
                    s
                }
            },
        )
    }

    @Test
    fun goldenScenarioWithStrictPress_otherLiftsUnchanged() {
        for (cp in GoldenScenario.checkpoints) {
            val history = goldenWithPress.copy(sessions = goldenWithPress.sessions.filter { it.date <= cp.today })
            val advice = Coach.evaluate(history, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            val (press, other) = advice.partition { it.lift == MainLift.STRICT_PRESS }
            assertEquals(cp.expectedAdvice, other.map { ExpectedAdvice(it.type, it.lift, it.severity) }, "advice at ${cp.label}")
            val targets = Coach.targetsFor(cp.weekday, history, GoldenScenario.plan, GoldenScenario.settings, cp.today, cp.storedAdvice)
            val actual = targets.map { (it.payload as AdvicePayload.Target).let { t -> ExpectedTarget(t.exerciseId, t.weightKg, t.reps, t.sets) } }
            assertEquals(cp.expectedTargets, actual, "targets at ${cp.label}")
            // The flat press stalls like any lift, except while stalls are paused after the holiday.
            val expectedPress = if (cp.label == "return") emptyList() else listOf(AdviceType.STALL)
            assertEquals(expectedPress, press.map(Advice::type), "press at ${cp.label}")
        }
    }

    private companion object {
        /** SeedLibrary.exercises() at BASE (ff633da), before strict press: ids and order must never change. */
        val BASE_SEED = """
            1|Squat|SQUAT|null|Quads,Glutes|true|false
            2|Bench|BENCH|null|Pecs,Triceps,Front delts|true|false
            3|Deadlift|DEADLIFT|null|Hamstrings,Glutes,Lats|true|false
            4|Pause squat|VARIATION|SQUAT|Quads,Glutes|true|false
            5|Front squat|VARIATION|SQUAT|Quads,Glutes,Upper back,Trunk|true|false
            6|Bulgarian split squat|ACCESSORY|null|Quads,Glutes|true|false
            7|Pin squat|VARIATION|SQUAT|Upper back,Trunk|true|false
            8|Safety-bar squat|VARIATION|SQUAT|Upper back,Trunk|true|false
            9|Hip thrust|ACCESSORY|null|Glutes,Hips,Upper back|true|false
            10|Box squat|VARIATION|SQUAT|Glutes,Hips|true|false
            11|Spoto press|VARIATION|BENCH|Pecs,Front delts|true|false
            12|Long-pause bench|VARIATION|BENCH|Pecs,Front delts|true|false
            13|DB press|ACCESSORY|null|Pecs,Front delts|true|false
            14|Pin press|VARIATION|BENCH|Pecs,Triceps|true|false
            15|Larsen press|VARIATION|BENCH|Pecs,Triceps|true|false
            16|Close-grip bench|VARIATION|BENCH|Triceps|true|false
            17|JM press|ACCESSORY|null|Triceps|true|false
            18|Board press|VARIATION|BENCH|Triceps|true|false
            19|Deficit deadlift|VARIATION|DEADLIFT|Quads,Lats,Position|true|false
            20|Paused deadlift|VARIATION|DEADLIFT|Quads,Lats,Position|true|false
            21|RDL|ACCESSORY|null|Hamstrings,Lats,Glutes,Upper back|true|false
            22|Paused deadlift at the knee|VARIATION|DEADLIFT|Hamstrings,Lats|true|false
            23|Block pull|VARIATION|DEADLIFT|Glutes,Upper back|true|false
            24|Curl|ACCESSORY|null|Biceps|true|false
            25|Row|ACCESSORY|null|Lats,Upper back|true|false
            26|Pull-up|ACCESSORY|null|Lats,Biceps|true|false
        """.trimIndent()
    }
}

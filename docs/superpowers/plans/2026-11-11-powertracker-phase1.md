# Powertracker Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline Android powerlifting logbook with a rules-based coach. The coach suggests next-session loads, detects stalls and prescribes accessories, calls deloads and flags lagging lifts.

**Architecture:** There are two Gradle modules. `:domain` is plain Kotlin/JVM with no Android code. It holds models, e1RM maths, plate loading, validation and every coach rule, with `Coach.evaluate(...)` as a pure function, and its tests run on any JVM. `:app` is the Android app: Room + DataStore in `data/`, Compose screens + ViewModels in `ui/`, and the rest-timer service and app lock in `platform/`. Dependencies are wired by hand in an `AppContainer`, as in nhlbetstracker.

**Tech Stack:** Kotlin 2.4.21, AGP 9.5.0, Gradle 9.6.0, Compose BOM 2026.02.01 + Material 3, Room 2.8.5 (KSP 2.3.13), DataStore 1.3.0, Navigation Compose 2.10.2, kotlinx-serialization-json 1.12.0, androidx.biometric 1.1.0, JUnit 4.13.2. Version numbers are copied from `nhlbetstracker/gradle/libs.versions.toml` and `GearCalc/gradle/libs.versions.toml`.

**Spec:** `docs/superpowers/specs/2026-11-11-powertracker-phase1-design.md`

## Global Constraints

- Package: `com.powertracker`. App name: "Powertracker". Module `:domain` is `com.powertracker.domain`.
- minSdk 26, targetSdk 37, compileSdk 37. JVM target 17.
- kg only. Bar 20 kg. Default plates per side: 25, 20, 15, 10, 5, 2.5, 1.25. Change plates add 0.5 and 0.25.
- Rounding step = 2 × the smallest available plate, so 2.5 kg by default and 0.5 kg with change plates. *(Deviation from the spec's "1.25 kg": a 1.25 kg total needs 0.625 kg per side, which can't be loaded with real change plates.)*
- Default planned RPE for main lifts: 8.0. Stall window: 3 weeks (deadlift 4). Flat = within +1%.
- The coach never applies anything itself. Every change is an `Advice` with status `OPEN` until the user taps Apply or Dismiss.
- No suggested load may be more than 5% above the last completed top set.
- No internet permission. The merged manifest must not contain `android.permission.INTERNET`.
- Release builds: `optimization { enable = true }` for the whole app (GearCalc fix).
- Every domain function takes `today: LocalDate` as a parameter. No calls to `LocalDate.now()` inside `:domain`.
- Room: `exportSchema = true`, schemas in `app/schemas/`. No `fallbackToDestructiveMigration`.
- Commit messages end with the session's attribution lines.

## Review Focus

1. **A session logged with no RPE on any set.** The coach still works: e1RM uses Epley, and progression treats a missing RPE as "on target" (+2.5 / repeat rule), never as easy. *Test in Task 5.*
2. **A light accessory where the smallest load step is more than 5%** (20 kg × 10 @ 6). The 5% cap would freeze the load forever, so the coach adds a rep instead (max 12). *Test in Task 5.*
3. **The same day logged twice, or two sessions in one week.** Weekly trends use the best set of that week, not the latest, so a light second session can't fake a stall or a drop. *Test in Task 4.*
4. **Restoring a backup from a newer app version, or a corrupt file.** Restore refuses it with a clear message, and existing data stays untouched. *Test in Task 11.*
5. **Exercises edited or deleted after being logged.** A custom exercise that has logged sets can't be deleted (archive only), so history and trends never point at a missing exercise. *Test in Task 10.*

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/*
domain/build.gradle.kts                          kotlin("jvm")
domain/src/main/kotlin/com/powertracker/domain/
  model/Models.kt            enums, Exercise, LoggedSet, LoggedSession, PlanItem, Baseline, TrainingHistory
  model/Advice.kt            Advice, AdviceType, AdviceStatus, AdvicePayload, AccessoryPrescription
  e1rm/E1rm.kt               RPE-table + Epley e1RM
  plates/PlateLoader.kt      per-side breakdown and rounding
  validation/InputValidator.kt
  history/HistoryStats.kt    weekly best e1RM, sessions per lift, gaps, miss points
  coach/CoachSettings.kt
  coach/ProgressionRule.kt
  coach/AccessoryMap.kt
  coach/StallRule.kt
  coach/FatigueRule.kt
  coach/WeakPointRule.kt
  coach/Coach.kt
domain/src/test/kotlin/com/powertracker/domain/
  HistoryBuilder.kt          test DSL for synthetic histories
  ...one test file per source file
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/powertracker/
  MainActivity.kt, PowertrackerApp.kt, AppContainer.kt
  data/db/        Entities.kt, Converters.kt, Daos.kt, AppDatabase.kt, SeedLibrary.kt
  data/           TrainingRepository.kt, AdviceRepository.kt, SettingsRepository.kt, Backup.kt
  ui/theme/       Color.kt, Type.kt, Theme.kt
  ui/             AppShell.kt (bottom bar + nav)
  ui/today/       TodayScreen.kt, TodayViewModel.kt, SetRow.kt, PlateStrip.kt
  ui/coach/       CoachScreen.kt, CoachViewModel.kt, AdviceCard.kt
  ui/progress/    ProgressScreen.kt, ProgressViewModel.kt, LineChart.kt, LiftHistoryScreen.kt
  ui/plan/        PlanScreen.kt, PlanViewModel.kt, SettingsScreen.kt
  platform/       RestTimerService.kt, AppLock.kt
app/src/test/...           JVM tests for Backup
app/src/androidTest/...    DAO, migration and Compose tests
```

---

### Task 1: Project scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` (+ `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` copied from GearCalc), `.gitignore` (Android standard plus `local.properties`)
- Create: `domain/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `PowertrackerApp.kt`, `ui/theme/*`
- Test: `domain/src/test/kotlin/com/powertracker/domain/SmokeTest.kt`

**Interfaces:**
- Produces: modules `:domain` and `:app`, with `:app` depending on `implementation(project(":domain"))`. `PowertrackerApp : Application` (holds `AppContainer` from Task 10).

- [ ] **Step 1:** Write `SmokeTest.smokeRuns()`, which asserts `1 + 1 == 2`, so the domain test pipeline is proven.
- [ ] **Step 2:** Run `./gradlew :domain:test`. Expected: FAIL (no build files yet).
- [ ] **Step 3:** Create the build files with the versions in the Tech Stack.
  - `domain` uses `kotlin("jvm")` with `jvmToolchain(17)`.
  - `app` uses the android-application, kotlin-compose, ksp and kotlin-serialization plugins; namespace and applicationId `com.powertracker`; versionCode 1, versionName "1.0"; release `optimization { enable = true }`; Room schema directory `app/schemas`.
  - The manifest declares `<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` and `USE_BIOMETRIC`.
  - `MainActivity` shows a `PowertrackerTheme { Text("Powertracker") }`. The theme is Material 3 with dynamic colour on Android 12+ and a dark-red fallback seed.
- [ ] **Step 4:** Run `./gradlew :domain:test`. Expected: PASS. If an Android SDK is available, also run `./gradlew :app:assembleDebug` (expected: BUILD SUCCESSFUL).
- [ ] **Step 5:** Commit: `chore: scaffold domain and app modules`.

---

### Task 2: Domain models and e1RM

**Files:**
- Create: `domain/.../model/Models.kt`, `domain/.../model/Advice.kt`, `domain/.../e1rm/E1rm.kt`
- Test: `domain/src/test/.../e1rm/E1rmTest.kt`

**Interfaces:**
- Produces (`Models.kt`):
  ```kotlin
  enum class MainLift { SQUAT, BENCH, DEADLIFT }
  enum class Category { SQUAT, BENCH, DEADLIFT, VARIATION, ACCESSORY }
  enum class MissPoint { FLOOR, MID, LOCKOUT }
  data class Exercise(val id: Long, val name: String, val category: Category,
      val parentLift: MainLift?, val muscles: List<String>, val isBuiltIn: Boolean, val archived: Boolean = false) {
      /** SQUAT/BENCH/DEADLIFT category → that lift; VARIATION → parentLift; ACCESSORY → null */
      val mainLift: MainLift?
      val isMainLift: Boolean   // category is SQUAT, BENCH or DEADLIFT
  }
  data class LoggedSet(val exerciseId: Long, val weightKg: Double, val reps: Int, val rpe: Double?,
      val failed: Boolean, val missPoint: MissPoint?, val isTopSet: Boolean)
  data class LoggedSession(val id: Long, val date: LocalDate, val bodyweightKg: Double?,
      val readiness: Int?, val sets: List<LoggedSet>)
  data class PlanItem(val id: Long, val weekday: DayOfWeek, val exerciseId: Long, val sets: Int,
      val reps: Int, val targetRpe: Double, val order: Int)
  data class Baseline(val exerciseId: Long, val e1rmKg: Double, val date: LocalDate)
  data class TrainingHistory(val sessions: List<LoggedSession>, val exercises: Map<Long, Exercise>,
      val baselines: List<Baseline>)
  ```
- Produces (`Advice.kt`):
  ```kotlin
  enum class AdviceType { TARGET, STALL, DELOAD, WEAK_POINT, RE_ENTRY }
  enum class AdviceStatus { OPEN, APPLIED, DISMISSED }
  data class AccessoryPrescription(val exerciseName: String, val sets: Int, val repsLow: Int, val repsHigh: Int)
  @Serializable sealed interface AdvicePayload {
      data class Target(val exerciseId: Long, val weightKg: Double, val reps: Int, val rpe: Double)
      data class Accessories(val items: List<AccessoryPrescription>)
      data class Deload(val volumeFactor: Double = 0.6, val loadFactor: Double = 0.9)
      data class WeakPoint(val lift: MainLift, val missPoint: MissPoint?)
      data class ReEntry(val loadFactor: Double = 0.85)
  }
  data class Advice(val type: AdviceType, val lift: MainLift?, val createdAt: LocalDate,
      val reason: String, val severity: Int, val payload: AdvicePayload,
      val status: AdviceStatus = AdviceStatus.OPEN, val id: Long = 0)
  ```
  `:domain` uses the kotlinx-serialization plugin and json core so that `:app` can store payloads as JSON.
- Produces: `object E1rm { fun estimate(weightKg: Double, reps: Int, rpe: Double?): Double }`

- [ ] **Step 1: Write the failing tests** in `E1rmTest`:
  - `estimate(100.0, 1, 10.0) == 100.0`
  - `estimate(100.0, 5, 10.0)` ≈ `115.87` (±0.01)
  - `estimate(100.0, 3, 8.0)` == `estimate(100.0, 5, 10.0)` (RPE 8 = 2 reps in reserve)
  - `estimate(100.0, 4, 8.5)` ≈ `100 / 0.8495` (half-step interpolation between 5 and 6 effective reps)
  - `estimate(100.0, 5, null)` ≈ `116.67` (Epley)
  - `estimate(100.0, 12, 7.0)` falls back to Epley on 15 effective reps ≈ `150.0`
  - `estimate(0.0, 5, 8.0) == 0.0`
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*E1rmTest'`. Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement the models and `E1rm.estimate`. Effective reps = `reps + (10 − rpe)`. Percentage of 1RM at RPE 10 for 1–12 reps: `1.000, .955, .922, .892, .863, .837, .811, .786, .762, .739, .707, .680`. Interpolate linearly between half steps. When effective reps > 12 or RPE is null, use Epley: `w × (1 + effectiveReps/30)`, where effectiveReps = reps when RPE is null.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(domain): add models and e1RM estimator`.

---

### Task 3: Plate loader and input validation

**Files:**
- Create: `domain/.../plates/PlateLoader.kt`, `domain/.../validation/InputValidator.kt`
- Test: `PlateLoaderTest.kt`, `InputValidatorTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object PlateLoader {
      val DEFAULT_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
      val CHANGE_PLATES = listOf(0.5, 0.25)
      const val BAR_KG = 20.0
      fun plates(changePlates: Boolean): List<Double>
      fun step(changePlates: Boolean): Double          // 2 × smallest plate
      fun round(kg: Double, changePlates: Boolean): Double   // nearest step, ties go lighter, never below BAR_KG
      fun perSide(totalKg: Double, changePlates: Boolean): List<Double>?  // greedy; null if not loadable
  }
  sealed interface InputWarning { data object HighReps : InputWarning; data class BigJump(val fromKg: Double) : InputWarning }
  object InputValidator { fun check(weightKg: Double, reps: Int, lastWeightKg: Double?): List<InputWarning> }
  ```

- [ ] **Step 1: Write failing tests:**
  - `step(false) == 2.5`, `step(true) == 0.5`
  - `round(146.25, false) == 145.0` (tie goes lighter), `round(147.6, false) == 147.5`, `round(10.0, false) == 20.0`, `round(101.3, true) == 101.5`
  - `perSide(182.5, false) == listOf(25.0, 25.0, 25.0, 5.0, 1.25)`, `perSide(20.0, false) == emptyList()`, `perSide(21.0, false) == null`, `perSide(101.0, true) == listOf(25.0, 15.0, 0.5)`
  - `check(100.0, 13, 100.0) == listOf(HighReps)`; `check(131.0, 5, 100.0) == listOf(BigJump(100.0))`; `check(130.0, 5, 100.0) == emptyList()` (exactly 30% is allowed); `check(100.0, 5, null) == emptyList()`
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*PlateLoaderTest' --tests '*InputValidatorTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement as specified. HighReps is reps > 12. BigJump is weight > lastWeightKg × 1.30.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(domain): add plate loader and input validation`.

---

### Task 4: History stats and the test history builder

**Files:**
- Create: `domain/.../history/HistoryStats.kt`
- Create (test): `domain/src/test/.../HistoryBuilder.kt`
- Test: `HistoryStatsTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class WeeklyPoint(val weekStart: LocalDate, val e1rmKg: Double)
  class HistoryStats(private val history: TrainingHistory) {
      /** Best e1RM per ISO week (Monday start) for the lift. Main-lift sets count in full;
       *  VARIATION sets of that parentLift count at ×0.9 (trend only). Failed sets are excluded.
       *  Weeks with no qualifying sets are omitted. Sorted ascending. */
      fun weeklyTrend(lift: MainLift, until: LocalDate): List<WeeklyPoint>
      /** Best e1RM from main-lift sets only within (until − days, until]. null if none. Falls back to the latest Baseline when there are no sets. */
      fun currentE1rm(lift: MainLift, until: LocalDate, days: Long = 28): Double?
      fun sessionCount(lift: MainLift): Int                 // sessions containing a main-lift set of it
      fun lastTopSet(exerciseId: Long, before: LocalDate): Pair<LocalDate, LoggedSet>?  // isTopSet, else heaviest set
      fun previousTopSet(exerciseId: Long, before: LocalDate): Pair<LocalDate, LoggedSet>?  // the one before lastTopSet
      fun missPoints(lift: MainLift, from: LocalDate, until: LocalDate): List<MissPoint>  // main + variations
      fun lastSessionDate(before: LocalDate): LocalDate?
      /** First session date after a gap of ≥ 14 days, if that gap is the most recent one; else null. */
      fun returnDateAfterGap(until: LocalDate): LocalDate?
  }
  ```
- Produces (test DSL, used by Tasks 5–9):
  ```kotlin
  fun history(start: LocalDate = LocalDate.of(2026, 1, 5), block: HistoryBuilder.() -> Unit): TrainingHistory
  class HistoryBuilder { fun session(dayOffset: Int, readiness: Int? = null, block: SessionBuilder.() -> Unit); fun baseline(exerciseId: Long, e1rm: Double) }
  class SessionBuilder { fun squat(kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, miss: MissPoint? = null)
      fun bench(...); fun deadlift(...); fun set(exerciseId: Long, ...) }
  object Ids { const val SQUAT = 1L; const val BENCH = 2L; const val DEADLIFT = 3L; const val PAUSE_BENCH = 10L; const val CURL = 50L }
  ```
  Each named lift call logs one top set. Builder exercises: the three main lifts, Pause Bench (VARIATION, BENCH) and Curl (ACCESSORY).

- [ ] **Step 1: Write failing tests:**
  - `weeklyTrend_takesBestSetOfWeek`: Monday squat 150×5@8, Thursday squat 120×5@8 in the same week → one point with `E1rm.estimate(150.0,5,8.0)` *(Review Focus 3)*
  - `weeklyTrend_variationCountsAtNinetyPercent`: a week with only pause bench 100×5@8 → point = `0.9 × estimate(100,5,8)`; `currentE1rm(BENCH, ...)` == null (variations never set the main e1RM)
  - `weeklyTrend_excludesFailedSets`
  - `currentE1rm_usesBaselineWhenNoSets`: baseline bench 120 → 120.0
  - `returnDateAfterGap`: sessions on days 0, 3, 20, 23 → returns day 20; sessions 0, 3, 7 → null
  - `missPoints_includesVariations`
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*HistoryStatsTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement `HistoryStats` and `HistoryBuilder`.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(domain): add history stats and test history builder`.

---

### Task 5: Next-session targets

**Files:**
- Create: `domain/.../coach/CoachSettings.kt`, `domain/.../coach/ProgressionRule.kt`
- Test: `ProgressionRuleTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class CoachSettings(val defaultRpe: Double = 8.0, val changePlates: Boolean = false,
      val stallWeeks: Int = 3, val deadliftStallWeeks: Int = 4)
  object ProgressionRule {
      /** null when the exercise has no prior top set. */
      fun target(item: PlanItem, exercise: Exercise, stats: HistoryStats, settings: CoachSettings, today: LocalDate): Advice?
  }
  ```
  It returns `Advice(type = TARGET, lift = exercise.mainLift, payload = Target(...), severity = 0)` with a reason in the form `"Last time: 150 × 5 @ 7. Coach suggests 155 × 5 @ 8."`

- [ ] **Step 1: Write failing tests** (plan RPE 8, reps 5 unless stated):
  - squat last 150×5@7 → 155.0 (+5 when RPE ≥ 1 below plan)
  - bench last 100×5@7 → 102.5 (bench/accessory +2.5)
  - squat last 150×5@8 → 152.5
  - squat last 150×5@8, previous top set 147.5×5@7.5 ten days earlier (jumped within 2 weeks and RPE rose) → 150.0
  - squat last 150×5@9 → 150.0 (repeat)
  - squat last 150×5@9.5 → 145.0 (−2.5%, rounded with ties lighter)
  - squat last 150×5 failed → 142.5 (−5%)
  - squat last 150×5 @ null RPE → 152.5 (treated as on target) *(Review Focus 1)*
  - curl last 20×10@6 → weight 20.0, reps 11 (the 5% cap blocks a load jump, so add a rep) *(Review Focus 2)*
  - curl last 20×12@6 → weight 20.0, reps 12 (rep ceiling 12)
  - every case: `payload.weightKg ≤ last × 1.05`
  - no prior set → null
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*ProgressionRuleTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement the rules in spec 5.1. Order: failed / far over plan → drop; above plan by more than 0.5 → repeat; within 0.5 → +2.5 or repeat; ≥ 1 below → +5 (squat/deadlift) or +2.5. Then apply the 5% cap and `PlateLoader.round`. If the capped load equals the last load and the rule wanted an increase, add 1 rep (max 12) instead.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(coach): add next-session progression rule`.

---

### Task 6: Accessory map and stall detection

**Files:**
- Create: `domain/.../coach/AccessoryMap.kt`, `domain/.../coach/StallRule.kt`
- Test: `AccessoryMapTest.kt`, `StallRuleTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class AccessoryOption(val exerciseName: String, val heavy: Boolean)  // heavy → 3×3–5, else 3×8–10
  object AccessoryMap {
      fun muscles(lift: MainLift, miss: MissPoint?): List<String>
      fun options(lift: MainLift, miss: MissPoint?): List<AccessoryOption>   // miss == null → fallback list
      val allExerciseNames: Set<String>   // seeded into the library in Task 10
  }
  object StallRule {
      fun evaluate(lift: MainLift, stats: HistoryStats, settings: CoachSettings, today: LocalDate,
                   currentAccessoryNames: Set<String>): Advice?
  }
  ```
  The map table is copied verbatim from spec §5.2. Heavy options are the variations (pause squat, front squat, pin squat, safety-bar squat, box squat, Spoto press, long-pause bench, pin press, Larsen press, close-grip bench, board press, deficit deadlift, paused deadlift, block pull). Everything else (Bulgarian split squat, hip thrust, DB press, JM press, RDL) is hypertrophy. The fallback (`miss == null`) returns options with the reason "Add one set, change the rep range (5s → 3s or 8s), or swap to a variation" and the lift's first two heavy variations.

- [ ] **Step 1: Write failing tests:**
  - `AccessoryMapTest`: `options(BENCH, LOCKOUT)` names == `[Close-grip bench, JM press, Board press]`; `muscles(SQUAT, FLOOR) == [Quads, Glutes]`; every lift × miss point (and null) returns ≥ 2 options.
  - `stall_benchFlatFourWeeksLockoutMisses_prescribesTriceps`: bench e1RM flat for 4 weeks, 3 lockout misses → STALL advice, payload items ⊂ the bench-lockout options, 2–3 items, JM press marked 3×8–10 and close-grip bench 3×3–5
  - `stall_skipsAccessoriesAlreadyDone`: close-grip bench in `currentAccessoryNames` → it isn't prescribed
  - `noStall_whenRisingOverOnePercent`: +2% over the window → null
  - `deadlift_needsFourWeeks`: flat 3 weeks → null; flat 4 weeks → STALL
  - `noMissData_usesFallback`
  - `severity == weeks stalled` (flat 5 weeks → 5)
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*AccessoryMapTest' --tests '*StallRuleTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. A stall exists when the reference point (the latest weekly point at or before `today − N weeks`) exists, max(weekly points after it) ≤ ref × 1.01, and at least N − 1 of the N window weeks have points. The dominant miss point is the most frequent in the window, with ties going to the most recent. Severity = consecutive weeks since the last point that beat ref × 1.01.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(coach): add stall detection with accessory prescriptions`.

---

### Task 7: Fatigue and deload

**Files:**
- Create: `domain/.../coach/FatigueRule.kt`
- Test: `FatigueRuleTest.kt`

**Interfaces:**
- Produces: `object FatigueRule { fun evaluate(stats: HistoryStats, history: TrainingHistory, today: LocalDate): Advice? }`. The result is a DELOAD with `payload = Deload(0.6, 0.9)`, `severity` = the number of signals, and a reason listing each signal that fired.

- [ ] **Step 1: Write failing tests** (each over the last 14 days):
  - `twoSignals_deload`: RPE on squat and bench up ≥ 1 at the same load, plus readiness averaging 2 → DELOAD, severity 2
  - `oneSignal_noDeload`: only 2 failed reps → null
  - `allFour_severityFour`
  - `singleLiftDrop_isNotFatigueSignal`: e1RM down on bench only → that signal doesn't fire
  - `readinessDrop`: readiness falling from 4.5 to 3.0 (≥ 1.5 drop between the first and second weeks) counts as a signal
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*FatigueRuleTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement the four signals from spec §5.3. "RPE rising at the same load": for each main lift, the RPE of the latest top set minus the RPE of an earlier top set in the window at ≥ the same weight is ≥ 1. "e1RM down": the latest weekly point is < the previous point for ≥ 2 lifts.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(coach): add fatigue and deload rule`.

---

### Task 8: Weak-point check

**Files:**
- Create: `domain/.../coach/WeakPointRule.kt`
- Test: `WeakPointRuleTest.kt`

**Interfaces:**
- Produces: `object WeakPointRule { fun evaluate(stats: HistoryStats, today: LocalDate): List<Advice> }`. Each result is a WEAK_POINT with `payload = WeakPoint(lift, missPoint)`.
- Ratio ranges: `SQUAT_TO_DEADLIFT = 0.80..0.90`, `BENCH_TO_SQUAT = 0.60..0.70`.

- [ ] **Step 1: Write failing tests:**
  - `lowBenchRatioFourWeeks_flagsBench`: squat 200, bench 110 (0.55) every week for 4 weeks → WEAK_POINT(BENCH)
  - `lowRatioOnlyThreeWeeks_noFlag`
  - `squatToDeadliftLow_flagsSquat`; `squatToDeadliftHigh_flagsDeadlift` (ratio > 0.90 → deadlift is lagging)
  - `consistentMissPoint_flags`: 3 deadlift FLOOR misses in 4 weeks, ≥ 60% of deadlift misses → WEAK_POINT(DEADLIFT, FLOOR), even with normal ratios
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*WeakPointRuleTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Compute the weekly ratio from `weeklyTrend` points that share a week. Flag the lagging lift when all of the last 4 weekly ratios are outside the range on the same side.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(coach): add weak-point rule`.

---

### Task 9: Coach (composition, conflicts and safety rails)

**Files:**
- Create: `domain/.../coach/Coach.kt`
- Test: `CoachTest.kt`

**Interfaces:**
- Consumes: the rules from Tasks 5–8.
- Produces:
  ```kotlin
  object Coach {
      /** New advice to store. Never returns duplicates of OPEN prior advice. */
      fun evaluate(history: TrainingHistory, plan: List<PlanItem>, settings: CoachSettings,
                   today: LocalDate, priorAdvice: List<Advice>): List<Advice>
      /** TARGET advice for today's plan items only (used by the Today screen). */
      fun targetsFor(day: DayOfWeek, history: TrainingHistory, plan: List<PlanItem>,
                     settings: CoachSettings, today: LocalDate, priorAdvice: List<Advice>): List<Advice>
  }
  ```

- [ ] **Step 1: Write failing tests:**
  - `deloadSuppressesIncreases`: fatigue history with an OPEN DELOAD → `targetsFor` never returns a weight above the last top set
  - `minimumHistory`: bench with 2 sessions and no baseline → no STALL, WEAK_POINT or TARGET for bench; with a baseline → allowed
  - `onePerLiftPerWeek`: a STALL for bench created 3 days ago (any status) → no new bench card; 8 days ago → allowed. TARGET advice is exempt (per-session chips).
  - `dismissedNotRepeatedUnlessWorse`: DISMISSED STALL bench with severity 4 and current severity 4 → none; severity 5 → returned
  - `openNotDuplicated`: OPEN STALL bench → none, whatever the severity
  - `gapReturnsReEntry`: sessions on day 0 and day 20, today = day 21 → RE_ENTRY advice, and no STALL for any lift until day 34
  - `integration_benchStallWithLockout_endToEnd`: a 6-week synthetic history → exactly one STALL(BENCH) with triceps accessories
- [ ] **Step 2:** Run `./gradlew :domain:test --tests '*CoachTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement `evaluate`:
  1. RE_ENTRY check, which pauses stalls for 14 days after the return date.
  2. Fatigue.
  3. Stall per lift.
  4. Weak point.
  5. Filter for minimum history, the weekly cap per lift (non-TARGET), dedupe against OPEN, and DISMISSED-unless-worse (same type + lift, higher severity).
  
  `targetsFor`: apply `ProgressionRule.target` to each plan item for `day`. With an OPEN DELOAD, clamp each weight to ≤ last top set × 0.9 and multiply sets by 0.6 (rounded, min 1).
- [ ] **Step 4:** Run `./gradlew :domain:test`. Expected: all domain tests PASS.
- [ ] **Step 5:** Commit: `feat(coach): compose rules with conflict resolution and safety rails`.

---

### Task 10: Room database, seed library and repositories

**Files:**
- Create: `app/.../data/db/Entities.kt`, `Converters.kt`, `Daos.kt`, `AppDatabase.kt`, `SeedLibrary.kt`, `app/.../data/TrainingRepository.kt`, `AdviceRepository.kt`, `app/.../AppContainer.kt`
- Test: `app/src/androidTest/.../data/TrainingRepositoryTest.kt`, `MigrationTest.kt`

**Interfaces:**
- Consumes: domain models; `AccessoryMap.allExerciseNames`.
- Produces:
  ```kotlin
  @Database(entities = [ExerciseEntity::class, SessionEntity::class, SetEntity::class,
      PlanItemEntity::class, AdviceEntity::class, BaselineEntity::class], version = 1, exportSchema = true)
  abstract class AppDatabase : RoomDatabase()
  class TrainingRepository(db: AppDatabase) {
      fun history(): Flow<TrainingHistory>
      fun exercises(): Flow<List<Exercise>>
      fun plan(): Flow<List<PlanItem>>
      suspend fun startSession(date: LocalDate, bodyweightKg: Double?, readiness: Int?): Long
      suspend fun logSet(sessionId: Long, set: LoggedSet): Long
      suspend fun updateSet(setId: Long, set: LoggedSet); suspend fun deleteSet(setId: Long)
      suspend fun savePlanItem(item: PlanItem): Long; suspend fun deletePlanItem(id: Long)
      suspend fun addExercise(e: Exercise): Long
      /** Throws ExerciseInUseException if sets reference it; use archiveExercise instead. */
      suspend fun deleteExercise(id: Long); suspend fun archiveExercise(id: Long)
      suspend fun setBaseline(b: Baseline)
  }
  class AdviceRepository(db: AppDatabase) {
      fun open(): Flow<List<Advice>>; suspend fun all(): List<Advice>
      suspend fun insert(advice: List<Advice>); suspend fun setStatus(id: Long, status: AdviceStatus)
  }
  class AppContainer(context: Context) { val db; val training; val advice; val settings /* Task 11 */ }
  ```
  `SetEntity` has a foreign key to `ExerciseEntity` with `onDelete = RESTRICT`, and to `SessionEntity` with `CASCADE`. `AdviceEntity.payload` stores `Json.encodeToString(AdvicePayload)`. `SeedLibrary` inserts the three main lifts, the variations and accessories from `AccessoryMap.allExerciseNames` (with categories, parent lifts and muscles), and curl, row and pull-up in `RoomDatabase.Callback.onCreate`.

- [ ] **Step 1: Write failing instrumented tests** (in-memory DB):
  - `seedContainsEveryAccessoryMapName`
  - `logAndReadHistory_roundTrips` (miss point, null RPE, failed flag)
  - `deleteExerciseWithSets_throws_archiveWorks` *(Review Focus 5)*
  - `advicePayload_roundTrips` for all 5 payload types
  - `MigrationTest.schemaV1_opens` using `MigrationTestHelper` (sets up the harness for future versions)
- [ ] **Step 2:** Run `./gradlew :app:connectedDebugAndroidTest --tests '*TrainingRepositoryTest'` (needs an emulator or device). Expected: FAIL.
- [ ] **Step 3:** Implement the entities, DAOs, converters (LocalDate as epochDay, DayOfWeek as Int, lists as JSON), seeding and repositories.
- [ ] **Step 4:** Run the same command. Expected: PASS. `app/schemas/com.powertracker.data.db.AppDatabase/1.json` exists.
- [ ] **Step 5:** Commit: `feat(data): add Room database, seed library and repositories`.

---

### Task 11: Settings and backup/restore

**Files:**
- Create: `app/.../data/SettingsRepository.kt`, `app/.../data/Backup.kt`
- Test: `app/src/test/.../data/BackupTest.kt` (JVM)

**Interfaces:**
- Produces:
  ```kotlin
  data class AppSettings(val changePlates: Boolean = false, val defaultRpe: Double = 8.0,
      val trainingDays: Set<DayOfWeek> = setOf(MONDAY, WEDNESDAY, FRIDAY), val appLock: Boolean = false,
      val restMainSec: Int = 180, val restAccessorySec: Int = 90) { fun coach(): CoachSettings }
  class SettingsRepository(context: Context) { val settings: Flow<AppSettings>; suspend fun update(f: (AppSettings) -> AppSettings) }
  @Serializable data class BackupFile(val version: Int, val exportedAt: Long, /* one list per table */ ...)
  object Backup {
      const val VERSION = 1
      fun encode(file: BackupFile): String
      fun decode(text: String): BackupResult   // sealed: Ok(file) | TooNew(version) | Corrupt(message)
  }
  suspend fun TrainingRepository.exportBackup(settings: AppSettings): BackupFile
  suspend fun TrainingRepository.restore(file: BackupFile)   // single transaction: clear + insert
  ```

- [ ] **Step 1: Write failing tests** in `BackupTest`:
  - `encodeDecode_roundTrips`
  - `newerVersion_isTooNew`: version 2 → `TooNew(2)` *(Review Focus 4)*
  - `garbage_isCorrupt`: `"not json"` and `"{}"` → `Corrupt`
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*BackupTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Restore runs in `db.withTransaction`, so a failure leaves the existing data untouched. The UI flow (export through `ACTION_CREATE_DOCUMENT` and share, import through `ACTION_OPEN_DOCUMENT` with a "Replace all data?" confirmation) lives in the Settings screen from Task 12.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(data): add settings and versioned backup/restore`.

---

### Task 12: App shell, Plan and Settings screens

**Files:**
- Create: `ui/AppShell.kt`, `ui/plan/PlanScreen.kt`, `PlanViewModel.kt`, `SettingsScreen.kt`
- Modify: `MainActivity.kt` (hosts `AppShell`)
- Test: `app/src/androidTest/.../ui/PlanScreenTest.kt`

**Interfaces:**
- Consumes: `TrainingRepository`, `SettingsRepository`, `Backup`.
- Produces: `enum class Tab { TODAY, COACH, PROGRESS, PLAN }` and `@Composable fun AppShell(container: AppContainer)`, a bottom navigation bar with these 4 tabs (Today is the start destination), plus routes `liftHistory/{lift}` and `settings`.

- [ ] **Step 1:** Write a failing Compose test: `addPlanItem_showsUnderWeekday`. On the Plan tab, add "Squat 3 × 5 @ 8" to Monday, and the Monday section shows "Squat  3 × 5 @ 8".
- [ ] **Step 2:** Run `./gradlew :app:connectedDebugAndroidTest --tests '*PlanScreenTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement.
  - **Plan screen:** a section per training day, a reorderable list of items, an add/edit dialog (exercise picker with search and an "Add custom exercise" option: name, category, parent lift, muscles, sets, reps, RPE stepper 6–10 in steps of 0.5), and a "Set a known best" action per main lift (writes a `Baseline`).
  - **Settings screen:** change plates, default RPE, training days, rest durations, app lock, Export backup, Restore backup. The settings gear sits in the Plan top bar.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(ui): add app shell, plan and settings screens`.

---

### Task 13: Today screen (logging)

**Files:**
- Create: `ui/today/TodayScreen.kt`, `TodayViewModel.kt`, `SetRow.kt`, `PlateStrip.kt`
- Test: `app/src/androidTest/.../ui/TodayScreenTest.kt`

**Interfaces:**
- Consumes: `Coach.targetsFor`, `Coach.evaluate`, `PlateLoader`, `InputValidator`, the repositories.
- Produces:
  ```kotlin
  class TodayViewModel(...) : ViewModel() {
      val state: StateFlow<TodayState>
      fun startSession(bodyweightKg: Double?, readiness: Int?)
      fun logSet(planItemId: Long?, exerciseId: Long, set: LoggedSet, confirmed: Boolean = false)  // emits a warning first unless confirmed
      fun applyTarget(adviceId: Long); fun dismissTarget(adviceId: Long)
      fun addExercise(exerciseId: Long)
      fun finishSession()   // runs Coach.evaluate, inserts the new advice
  }
  data class TodayState(val sessionId: Long?, val items: List<TodayItem>, val pendingWarning: InputWarning?)
  data class TodayItem(val exercise: Exercise, val planned: PlanItem?, val suggestion: Advice?,
      val logged: List<LoggedSet>, val perSide: List<Double>?)
  ```
  Each item is pre-filled with the **planned** numbers (weight = last top set, or blank). A suggestion chip "Coach suggests 155 × 5 @ 8" with Apply/Dismiss replaces the pre-filled values only when Apply is tapped.

- [ ] **Step 1: Write failing Compose tests:**
  - `tapSet_logsAsPrescribed`
  - `failedSet_showsMissRow`: marking a rep as failed shows Floor / Mid / Lockout. The row isn't shown at RPE 9 but is shown at RPE 9.5.
  - `coachChip_changesNothingUntilApplied`: the chip is visible and the weight field still shows the planned weight; tapping Apply changes it to the suggested weight.
  - `typo_promptsBeforeSaving`: entering 13 reps shows "Did you mean…?" and nothing is saved until Confirm.
  - `plateStrip_shows`: 182.5 → "25 + 25 + 25 + 5 + 1.25"
- [ ] **Step 2:** Run `./gradlew :app:connectedDebugAndroidTest --tests '*TodayScreenTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Make the controls one-handed: large tap targets, ± steppers (weight by `PlateLoader.step`, reps by 1, RPE by 0.5), and a Finish button pinned at the bottom.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(ui): add Today logging screen with coach suggestion chips`.

---

### Task 14: Rest timer

**Files:**
- Create: `platform/RestTimerService.kt`
- Modify: `TodayViewModel.logSet` (starts the timer), `TodayScreen.kt` (inline countdown with −30s / +30s / Skip)
- Test: `domain`-style JVM test of `RestTimerState` in `app/src/test/.../platform/RestTimerStateTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class RestTimerState(val endsAtMillis: Long?) { fun remainingSec(now: Long): Int; fun plus(sec: Int, now: Long): RestTimerState }
  object RestTimer { val state: StateFlow<RestTimerState>; fun start(ctx: Context, seconds: Int); fun adjust(ctx: Context, deltaSec: Int); fun stop(ctx: Context) }
  ```
  The service is a `specialUse` foreground service that shows an ongoing notification with a chronometer counting down. It vibrates at zero, then stops itself. It asks for notification permission (Android 13+) the first time a timer starts.

- [ ] **Step 1:** Write failing tests: `remainingSec` never goes below 0; `plus(-30)` never sets the end time before now; `plus(30)` extends it.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*RestTimerStateTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. The duration is `restMainSec` for main lifts and variations and `restAccessorySec` for accessories.
- [ ] **Step 4:** Run the same command. Expected: PASS. Manual check: log a set, lock the phone, and the notification counts down and vibrates at zero.
- [ ] **Step 5:** Commit: `feat(platform): add foreground rest timer`.

---

### Task 15: Coach screen

**Files:**
- Create: `ui/coach/CoachScreen.kt`, `CoachViewModel.kt`, `AdviceCard.kt`
- Test: `app/src/androidTest/.../ui/CoachScreenTest.kt`

**Interfaces:**
- Consumes: `AdviceRepository.open()`, `HistoryStats.weeklyTrend`, `LineChart` (Task 16; until then a placeholder `Box`).
- Produces: `CoachViewModel.apply(adviceId)`, which carries out each advice type:
  - **STALL / WEAK_POINT with accessories:** inserts `PlanItem`s for the prescribed accessories on the training day that has that lift.
  - **DELOAD:** sets the advice to APPLIED, and `targetsFor` then clamps (Task 9) for the next 7 days.
  - **RE_ENTRY:** clamps targets ×0.85 for 7 days.
  
  `dismiss(adviceId)` sets the advice to DISMISSED.

- [ ] **Step 1:** Write failing tests:
  - `stallCard_applyAddsAccessoriesToPlan`
  - `dismiss_removesCard`
  - `emptyState_showsMessage`: "No advice right now. Keep logging."
- [ ] **Step 2:** Run `./gradlew :app:connectedDebugAndroidTest --tests '*CoachScreenTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Each card shows a title (e.g. "Bench has stalled"), the reason text, a 6-week mini trend chart, the prescription list, and Apply/Dismiss. Order cards by DELOAD, RE_ENTRY, STALL, WEAK_POINT.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(ui): add Coach advice feed`.

---

### Task 16: Progress screen

**Files:**
- Create: `ui/progress/ProgressScreen.kt`, `ProgressViewModel.kt`, `LineChart.kt`, `LiftHistoryScreen.kt`
- Test: `app/src/test/.../ui/progress/ProgressMathTest.kt` (JVM), `app/src/androidTest/.../ui/ProgressScreenTest.kt`

**Interfaces:**
- Consumes: `HistoryStats`, `E1rm`, `WeakPointRule` ranges.
- Produces:
  - `@Composable fun LineChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier)`: Canvas-drawn, no chart library.
  - `object ProgressMath { fun prs(history: TrainingHistory, lift: MainLift): List<Pr>; fun weeklyVolume(history, lift): List<Pair<LocalDate, Double>> /* Σ weight×reps per week */; fun total(stats, until): Double? }`

- [ ] **Step 1:** Write failing tests:
  - `prs_onlyRecordsImprovements`: e1RMs of 100, 105, 103, 110 → PRs at 100, 105, 110
  - `weeklyVolume_sumsWeightTimesReps`
  - `total_nullUnlessAllThreeLifts`
  - Compose: `tapLift_opensHistory`
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*ProgressMathTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. The screen shows e1RM charts for squat, bench, deadlift and the total, a bodyweight chart, a PR list, a weekly volume bar per lift, and lift ratios shown against their ranges. The lift history screen lists every session's sets for that lift.
- [ ] **Step 4:** Run the JVM and Compose tests. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(ui): add progress charts, PRs and lift history`.

---

### Task 17: App lock, release config and final checks

**Files:**
- Create: `platform/AppLock.kt` (ported from `GearCalc/app/src/main/java/com/example/dosagecalculator/AppLock.kt`, with the package changed)
- Modify: `MainActivity.kt` (when `appLock` is on, require biometric/device credentials on start and after 60 s in the background), `README.md` (build and run instructions)
- Test: `app/src/androidTest/.../ManifestTest.kt`

- [ ] **Step 1:** Write a failing test `noInternetPermission`: `packageManager.getPackageInfo(pkg, GET_PERMISSIONS).requestedPermissions` doesn't contain `android.permission.INTERNET`.
- [ ] **Step 2:** Run `./gradlew :app:connectedDebugAndroidTest --tests '*ManifestTest'`. Expected: FAIL if any dependency adds INTERNET without the `tools:node="remove"` override, otherwise PASS. Keep the test as a guard either way.
- [ ] **Step 3:** Port AppLock and wire it into `MainActivity`.
- [ ] **Step 4:** Final verification:
  - `./gradlew :domain:test :app:testDebugUnitTest`: all PASS
  - `./gradlew :app:connectedDebugAndroidTest`: all PASS
  - `./gradlew :app:assembleRelease`: BUILD SUCCESSFUL, and the release APK launches on a device without crashing (R8 check)
- [ ] **Step 5:** Commit: `feat: add app lock and release configuration`.

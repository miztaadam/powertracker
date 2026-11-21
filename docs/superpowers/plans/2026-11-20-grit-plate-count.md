# Counting Plates on Stack Machines (1.17) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A lifter can set a machine or cable exercise to "count plates", log "plate 7 × 10" instead of a guessed kg number, and be coached in plate steps on both sides, on both apps, with no plate number ever read as kg.

**Architecture:** Plates are a second unit, not a conversion. A set, a stored TARGET and the Today editor each keep their one number and gain a unit beside it (kg by default); an exercise gains a three-way setting (off, whole plates, half steps). One domain object, `PlateStack`, owns eligibility, the step grid, the lighter-week rule and the wording; one history filter, `inCurrentUnits()`, is what makes every coach rule read only sets in the exercise's current unit; every kg-only figure (e1RM, totals, volume, records, Known bests) skips non-kg sets as a guard. Storage adds two defaulted columns/fields per app and backup version 6.

**Tech Stack:** Kotlin Multiplatform domain (`@JsExport` through `GritDomain`, kotlinx.serialization), Android Compose + Room (schema 5 → 6), React + TypeScript + Dexie (5 → 6), Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-20-plate-count-design.md`. Read all of it with this plan, including "Questions answered by the owner": those four answers are decisions. Where this plan says **Planner's call**, the design left it open and the code did not answer it.

**Where the code differs from the design** (the plan follows the code):
- **versionCode is 28,** not 27: 1.16.1 already took 27 (`app/build.gradle.kts`).
- **Android has stand-ins too.** `TodayViewModel` shows a catalogue exercise added on Today under a negative id with no row until its first set (`standInId`, `catalogueEntry`). Choosing plates there must create the row, as on the web.
- **`ProgressionRule.MAX_REPS` and `InputValidator.MAX_JUMP_FACTOR` are `private`.** Task 1 makes both `internal`, as 1.16 did for the other constants. No value changes.
- **The two apps test each other's backups** through shared fixtures in `web/src/backup/__fixtures__/`, which the design does not mention (see Tasks 4 to 6).
- **"Every existing backup test passes unchanged" cannot hold.** Existing tests pin the newest version at 5 and feed a version 6 file expecting "too new". The edits allowed are listed under Global Constraints.
- **A custom row can carry a catalogue name** (`CatalogueResolver.resolve` reuses a row "whoever made it"), so the two "offered" rules can both apply to one row. Task 1 settles which wins.

## Global Constraints

- **Version:** 1.17 on both apps, versionCode 28. Room schema 6, Dexie schema 6 (declared, with no upgrade step), backup version 6.
- **Kg coaching is unchanged.** Every existing domain, Android and web test passes **with no test edited**, except the version pins listed next; a lifter who never turns this on sees no difference. If any other existing test has to change, stop and report it.
- **The only existing tests that may be edited,** each only as said (Tasks 4, 5 and 6). They pin the schema or backup version at 5, or read "the fixture this app writes now":
  - Android `data/BackupTest.kt`:
    - `v5File_onVersion4Decoder_isTooNew` (363–367): the file it decodes becomes `service.export(1L).copy(version = 5)`, as the version 3 test in that file does; it still expects `TooNew(5)`.
    - `everyBackup_isVersion5_withOrWithoutSkips` (371–378): both literal 5s become `Backup.VERSION`.
    - `v6File_isTooNew` (466–470): the made-up file is version 7 and the result `TooNew(7)`.
  - Android `data/BackupFixtureTest.kt`: `androidFixture` (68) points at `android-v6.json`; `androidFixture_holdsBothSides` (241) expects version 6; `webV5Fixture_restoresOnAndroid_withItsSkips` (319) compares the export with the fixture with its `version` raised to `Backup.VERSION` (a new `asUpgradedV5`, as `asUpgradedV4` does).
  - Android `data/MigrationTest.kt`: `appDatabase_opensAVersion4File` (318) expects `db.version` 6.
  - Web `backup/backup.test.ts`: `WEB_FIXTURE` (36) points at `web-v6.json`, with a new `WEB_V5_FIXTURE` for the frozen file; `webFixture_isUpToDate` (498) expects version 6; `everyBackup_isVersion5_withOrWithoutSkips` (870–880) expects 6 in its three places; `v5File_onVersion4Decoder_isTooNew_andVersion6IsTooNewHere` (932–938) reads `WEB_V5_FIXTURE` in its first half, and in its second half the made-up file and the result are version 7.
  - Web `db/skips.test.ts` (93): `installedVersion` is 6. Web `db/db.test.ts` (433, 435): the made-up version appended after `SCHEMA_VERSIONS` is 7, and so is the version expected.
  - Web `e2e/backup.spec.ts` (39): the exported file's `version` is 6 (edited in Task 6, where Playwright can run).
- **No other test changes, because a default is never written** (on both apps): a backup row carries `unit` only for a plate set and `plateCount` only for a plate-counted exercise; the web's stored rows, editors, targets and set inputs carry those keys only when not kg / off. So the frozen-fixture comparisons (`asUpgradedV1`…`V4`, `withSidesAdded`, `v1Restore_convertsOverheadPress`; `withV3Defaults`, `withV4Defaults`, `withV5Defaults`, the web's `asUpgradedV1`) and the row deep-equals (`skips.test.ts` `upgrade4to5_…`, `db.test.ts` `upgrade_noMatch_noChange`, `catalogue.test.ts:48,79`, `TodayScreen.test.tsx:474`, `useToday.test.tsx:151,163,429,520`) pass as they are. If one of them fails, a default is being written somewhere: fix that, not the test.
- **The 1.16 guide is unchanged:** its text, tests and committed screenshots. The guide draws the real Today screen with a sample that holds Leg press and Lat pulldown, so the plate menu item must not appear there (the seam is in Tasks 7 and 8).
- **No new sourced number.** The plate rules use only 0.85 (`AdvicePayload.ReEntry.loadFactor`), 0.9 (`AdvicePayload.Deload.loadFactor`), 0.95 and 0.975 (`ProgressionRule.FAILED_SCALE`, `HARD_SCALE`), 12 (`ProgressionRule.MAX_REPS`), 1.30 (`InputValidator.MAX_JUMP_FACTOR`), by name. Training numbers come only from acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport, and the design's own decided numbers.
- **Never plates:** squat, bench, deadlift, strict press, their variations (built-in or custom), SMITH exercises, and the three assisted machines (Assisted pull-up, Assisted chin-up, Assisted dip), which stay in kg. Each is the design's own: "Never for a main lift (category SQUAT, BENCH, DEADLIFT or STRICT_PRESS)"; "Not for custom variations …, nor `SMITH` exercises (a bar loaded with plates in kg)"; assisted machines "Decided: not in 1.17; they stay in kg". The plan adds one exclusion of its own: a custom accessory with the word "assisted" in its name (Planner's call 3).
- **Never mixed:** no code reads a set's number without its unit. A plate set never reaches e1RM, a total, a volume, a record, Known bests, a ratio or a kg rule, and never reads as 0 kg.
- **History is never rewritten:** changing the setting changes no stored set, no stored target and no backup.
- **Field naming:** `weightKg` keeps its name everywhere (domain, Room, Dexie, backup JSON, payload JSON). It means "plate number" when the unit beside it is plates. New fields go **last, with a kg / off default**, and outside Room a default is left out, never written. Find every use with `grep -rn weightKg`.
- **Generated web domain:** `web/src/domain/generated` is made by `./gradlew :domain:syncWebDomain`, checked by `:domain:checkWebDomain`, committed, and never edited or merged by hand. After any merge that touches `domain/`, regenerate.
- **Fixed rules:** new commits only (no force-push, no amend); stage files by path; never commit `.claude/`, `CLAUDE.md`, `.superpowers/`; no model names in code or commit bodies; never `playwright install`; e2e with `--trace off`; every web commit passes `npx tsc -b`; the commit trailer comes from the session's latest attribution reminder.
- **One builder per working tree.** Playwright runs only in the main checkout.

## Review Focus

The five likeliest ways this bites a lifter, most likely first. Each has a test in the task that owns the code.

1. **A kg number left in the PLATE field.** The lifter has 40 kg in the field (typed, from an applied target, or kept across a reload on the web), then turns on plates: the field must be empty, never "40". (Tasks 7, 8)
2. **The wrong target after switching back the same day.** Kg → plates → kg in one day: the kg target shows again; a kg target never stands for a plate exercise; and an exercise with older plate history gets its plate target the day plates are turned back on. (Tasks 7, 8)
3. **A false "big jump" across units.** The first plate set after a kg history (60 kg, then plate 7) and the first kg set after plates raise no warning, and "That is a big jump from 60 kg" never names the other unit. (Tasks 1, 7, 8)
4. **A hand-edited or damaged backup.** A plate set on a squat, a plate set numbered 0, or an unknown unit name: the first gives no e1RM, total, record or Known best; the other two are refused as a corrupt file with nothing changed. (Tasks 2, 4, 5)
5. **A narrow phone with large text.** At 320 px and 200 % text, the chip "plate 12½ × 8–12 @ 8", the PLATE stepper showing 12.5 and the sheet's three choices with their hints: nothing is cut, and Done stays reachable. (Tasks 7, 8)

## File Structure

- **Domain** (`domain/src/commonMain/kotlin/com/powertracker/domain/`)
  - Create `model/Load.kt`: `LoadUnit`, `PlateCount`.
  - Create `stack/PlateStack.kt`: eligibility, the step grid, the lighter-week rule, the wording, `inCurrentUnits()`. (Not in `plates/`: that package is the kg barbell loader.)
  - Modify `model/Models.kt`, `model/Advice.kt`, `validation/InputValidator.kt`, `history/HistoryStats.kt`, `progress/ProgressMath.kt`, `progress/BodybuildingProgress.kt`, `coach/ProgressionRule.kt`, `coach/Coach.kt`, `coach/BodybuildingCoach.kt`, `coach/ReturnRule.kt`, `coach/PhysiqueCoach.kt`, `coach/CoachSources.kt` (one sentence).
  - Modify `domain/src/jsMain/kotlin/com/powertracker/domain/web/JsTypes.kt`, `GritDomain.kt`; regenerate `web/src/domain/generated`.
- **Android** (`app/src/main/kotlin/com/powertracker/`)
  - Modify `data/db/Entities.kt`, `Converters.kt`, `Daos.kt`, `AppDatabase.kt`; `data/TrainingRepository.kt`, `data/Backup.kt`. Create `app/schemas/com.powertracker.data.db.AppDatabase/6.json` (exported by the build).
  - Create `ui/today/PlateCount.kt` (the sheet and its wording). Modify `ui/today/SkipToday.kt` (the menu), `TodayViewModel.kt`, `TodayScreen.kt`, `ExerciseCard.kt`, `Entry.kt`, `TodayDialogs.kt`, `ui/components/SetRow.kt`, `Format.kt`, `ui/progress/ProgressScreen.kt`, `LiftHistoryScreen.kt`.
- **Web** (`web/src/`)
  - Modify `db/schema.ts`, `db/mapping.ts`, `db/catalogue.ts`, `backup/format.ts`, `backup/backup.ts`.
  - Create `screens/today/PlateCount.tsx`. Modify `screens/today/SkipToday.tsx`, `ExerciseCard.tsx`, `TodayScreen.tsx`, `TodayDialogs.tsx`, `entry.ts`, `today.css`, `state/useToday.ts`, `ui/components/SetRow.tsx`, `format.ts`, `screens/progress/BodybuildingProgress.tsx`, `ExerciseHistoryScreen.tsx`.
- **Shared fixtures:** `web/src/backup/__fixtures__/android-v6.json` (written by Android's test), `web-v6.json` (written by the web's). The v1 to v5 fixtures are frozen.

## Task order and what can run in parallel

| Order | Task | Where |
|---|---|---|
| 1 → 2 → 3 | Domain, one at a time (each regenerates `web/src/domain/generated`, which two branches cannot both change) | one worktree, or the main checkout |
| 4 ∥ 5 | Android storage and backup ∥ web storage and backup | two worktrees |
| 6 | Each app restores the other's version 6 backup | main checkout, after 4 and 5 are merged |
| 7 ∥ 8 | Android Today ∥ web Today | two worktrees; Task 8's Step 6 (Playwright) runs in the main checkout after its merge |
| 9 | Release | main checkout |

Tasks 4, 5, 7 and 8 change nothing under `domain/`. If one finds it must, it stops and reports: the domain change goes in alone, serially, with its own regeneration.

---

### Task 1: The unit, the setting and the plate stack (domain)

**Files:** create `model/Load.kt`, `stack/PlateStack.kt`; modify `model/Models.kt`, `validation/InputValidator.kt`, `coach/ProgressionRule.kt` (visibility only), `web/JsTypes.kt`, `web/GritDomain.kt`; regenerate `web/src/domain/generated`. Tests: `domain/src/commonTest/kotlin/com/powertracker/domain/stack/PlateStackTest.kt`, `validation/InputValidatorPlatesTest.kt`, `domain/src/jsTest/kotlin/com/powertracker/domain/web/GritDomainPlatesTest.kt`.

**Interfaces — Produces:**

```kotlin
// model/Load.kt. The names are stored in databases, backups and advice payloads: new values go last.
enum class LoadUnit { KG, PLATES }
enum class PlateCount { OFF, WHOLE, HALF }

// model/Models.kt: each gains one LAST constructor parameter
data class Exercise(..., val archived: Boolean = false, val plateCount: PlateCount = PlateCount.OFF)
data class LoggedSet(..., val isTopSet: Boolean, val unit: LoadUnit = LoadUnit.KG)

// stack/PlateStack.kt
enum class PlateCheck { OK, STEP, RANGE }
object PlateStack {
    const val MIN_PLATE = 1.0
    const val MAX_PLATE = 50.0                              // Planner's call: the upper bound for a typed plate
    fun offered(exercise: Exercise): Boolean                // may this exercise count plates at all
    fun unitOf(exercise: Exercise): LoadUnit                // PLATES only when offered(exercise) && plateCount != OFF
    fun step(count: PlateCount): Double                     // HALF 0.5; WHOLE and OFF 1.0
    fun up(plate: Double?, step: Double): Double            // null -> 1; else the lowest grid plate above it, at most MAX_PLATE
    fun down(plate: Double?, step: Double): Double          // null -> 1; else the highest grid plate below it, never under 1
    fun floor(plate: Double, step: Double): Double          // the highest grid plate not above it, never under 1
    fun eased(reference: Double, factor: Double, step: Double): Double   // the lighter-week and step-down rule
    fun check(plate: Double, count: PlateCount): PlateCheck // a typed number
    fun text(plate: Double): String                         // "plate 7", "plate 7½"
    fun spoken(plate: Double): String                       // "plate 7", "plate 7 and a half"
}

// validation/InputValidator.kt: the 3-argument check stays as it is; MAX_JUMP_FACTOR becomes internal
data class BigJump(val fromKg: Double, val unit: LoadUnit = LoadUnit.KG) : InputWarning
fun check(weight: Double, reps: Int, lastWeight: Double?, unit: LoadUnit, plateStep: Double): List<InputWarning>
```

**The rules these functions fix:**
- **The grid** is 1, 2, 3 … (step 1) or 1, 1½, 2 … (step ½). `up(7.5, 1.0)` is 8 and `down(7.5, 1.0)` is 7.
- **`eased`** = the lower of `floor(reference × factor, step)` and `down(reference, step)`, never under 1. Compare in whole half-plates (integers), so 10 × 0.9 is 9 and never 8.
- **`offered`** (Planner's call where the two design rules meet): the row's category is `ACCESSORY`, **and** its name, after `Library.normalise`, does not hold the word "assisted" (it starts with `assisted` followed by a space or the end, or holds ` assisted`), **and** either `Library.find(name)` lists `MACHINE` or `CABLE` anywhere in its equipment, or `isBuiltIn` is false. So a custom accessory is offered whatever it is called, except one with that word: "Assisted dip", "Machine-assisted dip" and "My assisted row" are not offered; "Unassisted row" is.
- **`check`:** not on the count's grid → `STEP`; else outside 1..50 → `RANGE`; else `OK`. `OFF` checks as `WHOLE`.
- **`InputValidator.check` in plates:** high reps as today; a big jump only when `lastWeight != null`, `weight > lastWeight + plateStep` and `weight > lastWeight × MAX_JUMP_FACTOR`. In kg the 5-argument form answers exactly as the 3-argument one. The caller passes a `lastWeight` from the same unit, or null.

**JS bridge:** `JsSet` gains a last `unit: String = "KG"`, `JsExercise` a last `plateCount: String = "OFF"` (left out from JS they take the default, as `mode` does; `toDomain` reads them with `parseEnum`; `LoggedSet.toJs()` and `Exercise.toJs()` write them, so `JsBestSet.set` and every history that comes back carry them). New exports on `GritDomain`; no existing export changes:

```kotlin
fun plateCountOffered(exercise: JsExercise): Boolean
fun loadUnitOf(exercise: JsExercise): String                       // "KG" | "PLATES"
fun plateStep(plateCount: String): Double
fun plateUp(plate: Double?, plateCount: String): Double
fun plateDown(plate: Double?, plateCount: String): Double
fun plateCheck(plate: Double, plateCount: String): String          // "OK" | "STEP" | "RANGE"
fun plateText(plate: Double): String
fun plateSpoken(plate: Double): String
fun maxPlate(): Double
fun checkLoadInput(weight: Double, reps: Int, lastWeight: Double?, unit: String, plateCount: String): Array<String>   // as checkInput, in a unit
```

- [ ] **Step 1: Write the failing tests** in `PlateStackTest.kt`:
  - `offered_machineAndCableAccessories_andCustomAccessories`: Lat pulldown, Cable fly (any `CABLE` entry), T-bar row (`MACHINE, BARBELL`), Leg press: true. Custom `ACCESSORY` rows named "My row machine" and "Unassisted row": true.
  - `neverOffered_mainLifts_variations_smith_assisted`: each of the four main-lift categories; a built-in and a custom `VARIATION`; a `SMITH` catalogue entry; Assisted pull-up, Assisted chin-up, Assisted dip; custom accessories named "Assisted dip", "assisted row", "Machine-assisted dip" and "My assisted row": all false. A dumbbell catalogue accessory: false.
  - `everyMachineOrCableCatalogueEntry_isAnAccessory` (walks `Library.all()`).
  - `unitOf_isKgWhenNotOffered_whateverTheSettingSays`: a `SQUAT` row with `plateCount = WHOLE` is `KG`; Lat pulldown `OFF` → `KG`, `WHOLE` → `PLATES`, `HALF` → `PLATES`.
  - `upAndDown_onEachGrid`: `up(null, 1.0) == 1.0`; `up(7.0, 1.0) == 8.0`; `up(7.0, 0.5) == 7.5`; `up(7.5, 1.0) == 8.0`; `down(7.5, 1.0) == 7.0`; `down(1.0, 1.0) == 1.0`; `down(1.0, 0.5) == 1.0`; `up(50.0, 1.0) == 50.0`.
  - `eased_theDesignsExamples`: `eased(7.0, 0.95, 1.0) == 6.0`; `eased(7.0, 0.9, 1.0) == 6.0`; `eased(7.0, 0.85, 1.0) == 5.0`; `eased(7.0, 0.85, 0.5) == 5.5`; `eased(2.0, 0.9, 1.0) == 1.0`; `eased(2.0, 0.9, 0.5) == 1.5`; `eased(1.0, 0.9, 1.0) == 1.0`; `eased(7.0, 0.975, 1.0) == 6.0`; `eased(7.0, 0.975, 0.5) == 6.5`; `eased(7.0, 0.95, 0.5) == 6.5`.
  - `eased_isAtLeastOneStepDown_andLandsOnTheGrid`: `eased(10.0, 0.9, 1.0) == 9.0`; `eased(20.0, 0.975, 1.0) == 19.0`; `eased(7.5, 0.95, 1.0) == 7.0` (a logged half with Whole plates on); `eased(40.0, 0.975, 1.0) == 39.0`.
  - `check_wholeAndHalf`: `check(7.0, WHOLE) == OK`; `check(7.5, WHOLE) == STEP`; `check(7.5, HALF) == OK`; `check(7.25, HALF) == STEP`; `check(0.0, WHOLE) == RANGE`; `check(0.5, HALF) == RANGE`; `check(51.0, WHOLE) == RANGE`; `check(50.0, HALF) == OK`.
  - `text_andSpoken`: `text(7.0) == "plate 7"`; `text(7.5) == "plate 7½"`; `text(12.5) == "plate 12½"`; `spoken(7.5) == "plate 7 and a half"`; `spoken(7.0) == "plate 7"`.
- [ ] **Step 2: Write the failing tests** in `InputValidatorPlatesTest.kt`:
  - `oneStepUp_neverWarns`: (4, last 3, step 1) and (1.5, last 1, step 0.5): no `BigJump`.
  - `aBiggerJump_usesTheSameFactor`: (5, last 3, step 1) → `BigJump(3.0, LoadUnit.PLATES)`; (12, last 10) none; (13, last 10) none; (14, last 10) warns; (2, last 1, step 0.5) warns.
  - `noLastSetInThisUnit_neverWarns` (Review Focus 3): `check(7.0, 10, null, PLATES, 1.0)` and `check(60.0, 10, null, KG, 1.0)` hold no `BigJump`.
  - `highReps_isUnchanged`: 13 reps in plates → `HighReps`.
  - `kg_throughTheNewForm_equalsTheOldForm` for (130, last 100), (131, last 100), (20, last 0).
- [ ] **Step 3: Write the failing tests** in `GritDomainPlatesTest.kt`: `aSetBuiltWithoutAUnit_isKg` and `anExerciseBuiltWithoutASetting_isOff` (the seven-argument constructors still work); `toJs_carriesUnitAndPlateCount` (a `PLATES` set and a `HALF` exercise through `toDomain().toJs()` come back `"PLATES"` and `"HALF"`; `bodybuildingProgress` on a plate history gives `bestSets[0].set.unit == "PLATES"`); `plateExports_answerAsPlateStack` (one assertion per export above, from the values in Step 1); `checkLoadInput_inPlates_namesBigJump`.
- [ ] **Step 4: Run** `./gradlew :domain:allTests` — the new tests fail (unresolved references).
- [ ] **Step 5: Implement** the types, `PlateStack`, the validator form and the bridge. Make `ProgressionRule.MAX_REPS` and `InputValidator.MAX_JUMP_FACTOR` `internal`.
- [ ] **Step 6: Run** `./gradlew :domain:syncWebDomain :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`, then in `web/`: `npx tsc -b && npm test` — all pass with no existing test edited.
- [ ] **Step 7: Commit** by path (the domain files and `web/src/domain/generated`): `feat(domain): plates as a unit, the per-exercise setting and the plate stack`.

### Task 2: Nothing mixes plates with kg (domain)

**Files:** modify `stack/PlateStack.kt`, `model/Advice.kt`, `history/HistoryStats.kt`, `coach/FatigueRule.kt`, `progress/ProgressMath.kt`, `progress/BodybuildingProgress.kt`, `coach/Coach.kt`, `coach/BodybuildingCoach.kt`, `coach/PhysiqueCoach.kt`, `web/JsTypes.kt`, `web/GritDomain.kt`; regenerate. Tests: `stack/UnitsNeverMixTest.kt`, `model/TargetUnitTest.kt`.

**Interfaces — Consumes:** Task 1's `LoadUnit`, `PlateStack.unitOf`. **Produces:**

```kotlin
// stack/PlateStack.kt
/** The same sessions, each holding only the sets logged in their exercise's current unit. No session is dropped. */
fun TrainingHistory.inCurrentUnits(): TrainingHistory

// model/Advice.kt: LAST field; left out of the stored JSON when kg, as repsHigh is when null
data class Target(..., val repsHigh: Int? = null, val unit: LoadUnit = LoadUnit.KG) : AdvicePayload

// web/JsTypes.kt: JsAdvicePayload gains  val unit: String? = null   (Target: "KG" or "PLATES")
```

**Where the filter is applied** (this list is the whole of "the coach reads only sets in the current unit"):

| Reads `history.inCurrentUnits()` | Reads the whole `history` |
|---|---|
| `Coach.targetsFor`: the stats given to `ProgressionRule.target`, `ProgressionRule.held`, `ReturnRule.ease` and `clamp` | `SkipCoach.held` (any set of the exercise releases a pain hold), `hasMinimumHistory`, the session dates (`lastSessionDate`, `returnDateAfterGap`, `trainedBefore`, `easesBackIn`) |
| `BodybuildingCoach.targetsFor` and `repsToBeat`: `lastSets`, `firstTimeWeight`, the side's stats, `ReturnRule.ease`, `withoutAddWeight` | `SkipCoach.held`, the session dates |
| `PhysiqueCoach.stuck` (its sessions list) | weekly sets, check-ins, `Coach.evaluate` |
| `BodybuildingProgress.rows` | |
| `GritDomain.lastTopSet` (the web's Today uses it for the field and the typo check) | |

Sessions are kept even when emptied, because the 14-day gap, easing back in and `trainedBefore` count sessions. With no plate set and no setting on, `inCurrentUnits()` returns an equal history, so kg output is unchanged.

**The kg-only guards:** `HistoryStats.weeklyTrend`, `currentE1rm` and `sessionCount`, and `ProgressMath.isCompletedMainSet` (which `prs`, `isNewPr`, `weeklyVolume` and, through `prs`, `knownBest` all use) skip any set whose `unit` is not `KG`. `total` follows through `currentE1rm`. `FatigueRule.mainSets`, which feeds its top-set and RPE-rising signals from raw `weightKg`, keeps kg sets only (the design calls `FatigueRule` unchanged, and it is for every real log, since a main lift is never in plates). `E1rm` keeps its kg-only signature.

- [ ] **Step 1: Write the failing tests** in `UnitsNeverMixTest.kt`. Fixture: Lat pulldown with `plateCount = WHOLE`, powerlifting plan item 3 × 8 @ 8.
  - `inCurrentUnits_keepsEverySession_andOnlyCurrentUnitSets`: a session with a kg and a plate set of the exercise keeps one set; a session left with none is still there, with its date.
  - `inCurrentUnits_withNoPlates_changesNothing` (equal to the input).
  - `aKgHistoryThenPlates_givesNoPowerliftingTarget`: three kg sessions, then the setting turned on: `Coaching.targetsFor(POWERLIFTING, …)` has no target for it.
  - `switchingBackToKg_picksTheOldKgSetsUp`: kg sessions, then plate sessions, setting `OFF`: the target's `weightKg` comes from the last kg top set and its `unit` is `KG`.
  - `aPlateSetOnAMainLift_feedsNoKgFigure` (Review Focus 4): a `SQUAT` set `LoggedSet(…, weightKg = 9.0, reps = 5, …, unit = PLATES)` beside real kg sets: `weeklyTrend`, `currentE1rm`, `ProgressMath.prs`, `isNewPr` (for that set: false), `weeklyVolume`, `knownBest`, `total` and `FatigueRule.evaluate` (on a history that raises the RPE-rising deload, with a heavier plate "top set" added to its sessions) equal their values for the history without it; with only that set, `currentE1rm` is null (or the baseline) and `prs` is empty.
  - `aPainHold_isReleasedByASetInEitherUnit`: a pain skip, then a kg set of an exercise now in plates: `SkipCoach.held` is false.
  - `stuck_comparesOnlyTheCurrentUnit`: 21 days of kg sessions that would be stuck, then plates turned on: no SWAP.
  - **Stuck in plates: the rule is today's, unchanged.** `PhysiqueCoach.stuck` uses no kg and no e1RM: its baseline is the exercise's last session on or before `today − STUCK_DAYS` (21), and it is stuck when at least two later sessions exist and none `beats` the baseline, where `beats` is a higher heaviest weight, or the same with more reps in total. On a plate exercise that reads "a higher plate, or the same plate with more reps", plate against plate, so nothing replaces anything. Three tests (3 sets each, plan 3 × 8–12):
    - `stuck_aPlateExerciseAddingRepsThenAPlate_isNotStuck`: plate 7 × 10 (day −28), 7 × 11 (−21), 7 × 12 (−14), 8 × 8 (−7): no SWAP. Also with the baseline at 7 × 12 (day −21) and then 8 × 8 twice (−14, −7): no SWAP, because the higher plate beats it although the reps fell from 36 to 24.
    - `stuck_theSamePlateAndRepsFor21Days_givesASwap`: plate 7 × 10, 10, 10 on days −28, −14 and −7: one SWAP for the exercise, its reason naming the exercise and no weight.
    - `stuck_anExerciseSwitchedToPlatesLessThan21DaysAgo_isNotStuck`: kg sessions for two months, plates turned on at day −14 with plate 7 × 10 on days −14, −7 and −1: no SWAP (no plate session is old enough to be the baseline).
  - `bodybuildingProgress_rowsHoldOnlyCurrentUnitSets`: the row's `bestSets` are all `PLATES`; `goingUp` compares plate 8 with plate 9 (true), and is null with one plate session after many kg ones.
- [ ] **Step 2: Write the failing tests** in `TargetUnitTest.kt`: `aKgTarget_encodesWithNoUnitKey` (`Json.encodeToString<AdvicePayload>(Target(1, 100.0, 5, 8.0))` holds no `"unit"`); `aPayloadStoredBefore1_17_decodesAsKg`; `aPlateTarget_roundTrips` (holds `"unit":"PLATES"`). In `GritDomainPlatesTest.kt`: `advicePayload_carriesTheUnit` ("KG" for an old payload, "PLATES" for a plate one); `lastTopSet_isInTheCurrentUnit`.
- [ ] **Step 3: Run** `./gradlew :domain:allTests` — they fail.
- [ ] **Step 4: Implement** the filter, its call sites, the guards and the payload field.
- [ ] **Step 5: Run** `./gradlew :domain:syncWebDomain :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`, then `npx tsc -b && npm test` in `web/` — all pass, no existing test edited (the golden scenarios, the guide's sample and the stored-payload fixtures are the proof that kg is unchanged).
- [ ] **Step 6: Commit** `feat(domain): each rule reads one unit; kg figures skip plate sets; targets carry a unit`.

### Task 3: The coach in plates (domain)

**Files:** modify `coach/ProgressionRule.kt`, `Coach.kt`, `BodybuildingCoach.kt`, `ReturnRule.kt`, `CoachSources.kt`; regenerate. Tests: `coach/ProgressionRulePlatesTest.kt`, `CoachPlatesTest.kt`, `BodybuildingCoachPlatesTest.kt`, `ReturnRulePlatesTest.kt`.

**Interfaces — Consumes:** `PlateStack.unitOf / step / up / floor / eased / text`, `inCurrentUnits()`, `Target.unit`. **Produces** (existing public signatures unchanged; each rule branches on `PlateStack.unitOf(exercise)` and stamps `unit` on its `Target`):

```kotlin
// ProgressionRule: new trailing defaults only
fun held(target: Advice, item: PlanItem, stats: HistoryStats, today: LocalDate, plateStep: Double = 1.0): Advice
internal fun describe(last: LoggedSet, weightKg: Double, reps: Int, rpe: Double, unit: LoadUnit = LoadUnit.KG): String
internal fun fmt(v: Double, unit: LoadUnit): String      // KG: fmt(v) as today; PLATES: PlateStack.text(v)
// ReturnRule
internal fun withSuggestion(reason: String, weightKg: Double, reps: Int, repsHigh: Int?, rpe: Double, unit: LoadUnit = LoadUnit.KG): String
```

**The plate rules** (`step` = `PlateStack.step(exercise.plateCount)`; `last` = the last top set, which is a plate set because of Task 2; every kg-only helper — `isFreeLoad`, `round`, `roundDown`, `MAX_INCREASE`, `STEP_KG`, `BIG_STEP_KG`, `INCREASE`, `PlateLoader`, `E1rm` — is not called):

- **Powerlifting side, `ProgressionRule.target`,** with `delta = (last.rpe ?: targetRpe) − targetRpe`, checked in this order:
  1. **Missed:** `eased(last, FAILED_SCALE, step)` × the planned reps.
  2. **`delta ≥ 1.5`:** `eased(last, HARD_SCALE, step)` × the planned reps.
  3. **`delta > 0.5`, or `delta ≥ −0.5` with `recentJumpWithRisingRpe` (the same window as kg):** repeat: `floor(last, step)` × `max(planned reps, last reps)`. **Decided by the owner (2026-11-20):** the kg rule's repeat falls back to the planned reps; in plates the reps are the progress, so a repeat keeps them, as the pain hold already does.
  4. **Otherwise (on target or easier):** when `last.reps ≥ MAX_REPS` or the planned reps ≥ `MAX_REPS`: `up(last, step)` × the planned reps. Else `floor(last, step)` × `min(max(last.reps + 1, planned reps), MAX_REPS)`. **Planner's call:** the extra rep never aims under the plan's reps.
  - No 5 % cap and no big step.
- **`ProgressionRule.held`** (pain hold) has no exercise parameter: it branches on the target payload's `unit`. In plates the held aim is `floor(last, plateStep)` × `max(planned reps, last reps)`; a target above it (a higher plate, or the same plate with more reps) is replaced by it. `Coach` passes the exercise's step.
- **`Coach.clamp` and `BodybuildingCoach.clamp`:** `capFor` = `eased(reference, factor, step)` from the same fixed reference top set as kg; the target takes the lower of its own plate and each cap. Sets, notes and reps as kg.
- **Bodybuilding side:** earned → `up(heaviest, step)`; otherwise `floor(heaviest, step)`; the range, the texts and `repsToBeat` as kg.
- **`firstTimeWeight` in plates:** the heaviest non-missed plate of the exercise on the powerlifting side in the same 28-day window; `eased(…, ReEntry.loadFactor, step)` when that is more than 28 days old and `easeOld`, else `floor(…, step)`. With no plate sets: null. Baselines are never read.
- **`ReturnRule.ease` in plates:** trained on the other side → cap = `floor(heaviest non-missed plate there in the 28 days, step)`, with the easing note when it lowers the target; not trained on either side → the lower of the target and `eased(last, ReEntry.loadFactor, step)`.
- **Wording:** every reason prints the weight through `fmt(v, unit)` and the RPE through the one-argument `fmt`, as today, so "plate" never follows "@": "Last time: plate 7 × 8 @ 8. Coach suggests plate 7 × 9 @ 8."
- **`CoachSources.BODYBUILDING_INCREASE`** gets one sentence in its comment, no new source and no number beyond what it already cites: on a plate-counted machine the increase is one step, the smallest change the machine allows, which is often more than the 2–10 % cited; the owner chose one step.

- [ ] **Step 1: Write the failing tests** in `ProgressionRulePlatesTest.kt` (Lat pulldown, `WHOLE` unless said, plan 3 × 8 @ 8; "→" is the target's plate × reps):
  - `onTarget_addsARep`: last 7 × 8 @ 8 → 7 × 9, `unit == PLATES`, reason `"Last time: plate 7 × 8 @ 8. Coach suggests plate 7 × 9 @ 8."`; 7 × 11 @ 8 → 7 × 12; @ 8.5 (delta 0.5) → 7 × 9; 7 × 8 @ 6.5 → 7 × 9 (no big step).
  - `atTwelveReps_goesUpOneStep_backToThePlannedReps`: 7 × 12 @ 8 → 8 × 8; 7 × 12 @ 7 → 8 × 8; `HALF`: → 7.5 × 8, reason ends `"Coach suggests plate 7½ × 8 @ 8."`
  - `aPlanOfTwelveOrMoreReps_earnsTheStepEachTime`: plan 3 × 12, last 7 × 12 @ 8 → 8 × 12; plan 3 × 15, last 7 × 15 @ 8 → 8 × 15.
  - `oneStepIsNotCappedAtFivePercent`: last 2 × 12 @ 8 → 3 × 8.
  - `aLittleTooHard_repeats`: 7 × 8 @ 9 → 7 × 8; 7 × 10 @ 9 → 7 × 10.
  - `aRecentStepUpWithRpeRising_repeats`: previous 6 × 12 @ 8 ten days before, last 7 × 8 @ 8.5 → 7 × 8; but last 7 × 8 @ 7 after previous 6 × 12 @ 6.5 (delta −1, as kg) → 7 × 9.
  - `farTooHard_stepsDown`: 7 × 8 @ 9.5 → 6 × 8 (7 × 0.975 = 6.825); `HALF` → 6.5 × 8.
  - `aMissedTopSet_stepsDown`: plate 7 missed → 6 × 8 (7 × 0.95 = 6.65), reason starts `"Last time: plate 7 × 8 (missed)."`; `HALF` → 6.5 × 8; plate 1 missed → 1 × 8.
  - `halfStepsTurnedOffAfterAHalf`: last 7.5 × 9 @ 8 with `WHOLE` → 7 × 10; 7.5 × 12 @ 8 → 8 × 8; 7.5 × 8 @ 9 → 7 × 8; 7.5 missed → 7 × 8 (Planner's call: 7.125 rounds down to 7, which is already one whole step under 7½).
  - `belowThePlannedReps_aimsAtThePlan`: last 7 × 6 @ 8 → 7 × 8.
  - `noReason_putsPlateAfterTheAt`: across every target made in this file and the two below, no reason holds `"@ plate"`.
  - `underAPainHold_noStepAndNoExtraRep`: last 7 × 12 @ 8, held → 7 × 12; last 7 × 9 @ 8, held → 7 × 9; a missed 7 while held → 6 × 8 (a decrease still applies).
- [ ] **Step 2: Write the failing tests** in `CoachPlatesTest.kt` (through `Coaching.targetsFor(POWERLIFTING, …)`; top set plate 7 before the advice):
  - `deload_isNinetyPercentRoundedDown`: → plate 6, sets 3 → 2, reason ends with `Coach.DELOAD_NOTE`.
  - `easingBackIn_isEightyFivePercentRoundedDown`: → plate 5; `HALF` → 5.5.
  - `plateTwo_inADeload`: → plate 1; `HALF` → 1.5.
  - `plateOne_staysPlateOne_andTheDeloadStillCutsTheSets`.
  - `deloadAndEasingBackTogether_takeTheLowerCap_notTwoSteps`: → plate 5.
  - `aLighterWeekOfSeveralSessions_losesOnePlateOnly`: two deload sessions logged at plate 6 → still plate 6.
  - `aLighterWeek_doesThePlannedReps`: last 7 × 10 @ 8 in a deload → 6 × 8.
- [ ] **Step 3: Write the failing tests** in `BodybuildingCoachPlatesTest.kt` (plan 3 × 8–12 @ 8):
  - `everySetAtTheTop_goesUpOneStep`: 7 × 12, 12, 12 @ 8 → plate 8, reps 8, repsHigh 12, reason `"Last time: plate 7 × 12, 12, 12. Time to add weight. Coach suggests plate 8 × 8–12 @ 8."`; `HALF` → 7.5, `"… Coach suggests plate 7½ × 8–12 @ 8."`
  - `otherwise_samePlate_beatIt`: 7 × 12, 12, 11 → plate 7, reason `"Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 7 × 8–12 @ 8."`
  - `aMissedSet_orASetOverTheTargetRpe_earnsNothing`; `aSetWithNoRpe_countsAsOnTarget`; `underAPainHold_noStep_andRepsToBeatCarriesOn`.
  - `differentPlatesInOneSession_areEachNamed`: `"Last time: plate 7 × 12, plate 7½ × 10, plate 7½ × 9. Beat it. …"` (`HALF`).
  - `halfStepsTurnedOff_repeatRoundsDown_increaseGoesToTheNextWholePlate`: 7.5 × 12, 12, 11 with `WHOLE` → 7; 7.5 × 12, 12, 12 → 8.
  - `deloadAndEasingBackIn_useTheStepRule`: plate 7 → 6 in a deload (sets cut, range kept), → 5 easing back in, both → 5; "Time to add weight." is dropped when the final plate is not above last time's.
  - `firstTimeOnThisSide_takesTheHeaviestGoodPlateFromTheOtherSide`: powerlifting plate sets 8 and 9 in the last 28 days, and a missed 10 → plate 9, reason starts `"First time on this side."`; no e1RM conversion (the reps there do not change it).
  - `firstTime_fromOldPlateSets_isEasedOnce`: the heaviest is 9, 40 days old → 7 (9 × 0.85 = 7.65); `HALF` → 7.5; with a stored easing back in as well, still 7, not lower.
  - `firstTime_withOnlyKgSets_orOnlyABaseline_givesNoTarget`; `aKgHistoryThenPlates_givesNoBodybuildingTarget`.
  - `aPlateSet_neverTakesABodyweightBranch`: plate 1 × 12, 12, 12 @ 8 earns the step (→ plate 2: `earnedIncrease` refuses 0 kg, never plate 1); first time on this side from a powerlifting plate 1 gives plate 1 (`firstTimeWeight` skips 0 kg sets, never plate 1); `InputValidator.check(3.0, 10, 1.0, PLATES, 1.0)` holds `BigJump(1.0, PLATES)` (the jump check is skipped from 0 kg, never from plate 1).
  - `theFirstEverPlateSet_hasNoTarget_andTheRepsAimIsTheBottomOfTheRange` (`repsToBeat` is 8).
  - `changePlates_changesNoPlateTarget`: every target above is the same with `CoachSettings.changePlates` on and off.
- [ ] **Step 4: Write the failing tests** in `ReturnRulePlatesTest.kt`: `trainedOnTheOtherSide_capsAtItsHeaviestGoodPlate` (this side's target would be plate 10; the other side's 28 days hold 8 and a missed 11 → plate 8, reason ends with `Coach.RE_ENTRY_NOTE`); `aCapAtOrAboveTheTarget_changesNothing`; `notTrainedOnEitherSide_isEasedByTheStepRule` (last 9 → 7, planned reps).
- [ ] **Step 5: Run** `./gradlew :domain:allTests` — they fail. **Step 6: Implement.**
- [ ] **Step 7: Run** `./gradlew :domain:syncWebDomain :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`, then `npx tsc -b && npm test` in `web/` — all pass, no existing test edited.
- [ ] **Step 8: Commit** `feat(domain): the coach works in plate steps on both sides`.

### Task 4: Android storage and backup version 6

**Files:** modify `data/db/Entities.kt`, `Converters.kt`, `Daos.kt`, `AppDatabase.kt`, `data/TrainingRepository.kt`, `data/Backup.kt`; create `app/schemas/com.powertracker.data.db.AppDatabase/6.json`, `web/src/backup/__fixtures__/android-v6.json`. Tests: `app/src/test/kotlin/com/powertracker/data/MigrationTest.kt`, `BackupTest.kt`, `BackupFixtureTest.kt`, new `PlateCountStorageTest.kt`.

**Interfaces — Consumes:** `LoadUnit`, `PlateCount`. **Produces:**

```kotlin
// Entities.kt: each a LAST field
data class ExerciseEntity(..., val archived: Boolean = false, @ColumnInfo(defaultValue = "OFF") val plateCount: PlateCount = PlateCount.OFF)
data class SetEntity(..., val isTopSet: Boolean, @ColumnInfo(defaultValue = "KG") val unit: LoadUnit = LoadUnit.KG)
// Converters.kt: both enums as their name TEXT, as TrainingMode is
// Daos.kt
@Query("UPDATE exercise SET plateCount = :value WHERE id = :id") suspend fun setPlateCount(id: Long, value: PlateCount)
// TrainingRepository.kt: one transaction; with a catalogueName the row is found, restored or inserted first (CatalogueResolver). Returns the row's id.
suspend fun setPlateCount(exerciseId: Long?, catalogueName: String?, value: PlateCount): Long
// Backup.kt
// each new field is LAST and is left out of the JSON when it is the default: @EncodeDefault(EncodeDefault.Mode.NEVER)
// (the backup writer has encodeDefaults = true, so without it every row would gain the key)
data class ExerciseDto(..., val archived: Boolean, val plateCount: String = PlateCount.OFF.name)
data class SetDto(..., val isTopSet: Boolean, val unit: String = LoadUnit.KG.name)
const val VERSION = 6
```

```sql
-- MIGRATION_5_6, added to AppDatabase.build's addMigrations; database version = 6
ALTER TABLE logged_set ADD COLUMN unit TEXT NOT NULL DEFAULT 'KG'
ALTER TABLE exercise ADD COLUMN plateCount TEXT NOT NULL DEFAULT 'OFF'
```

- **The column defaults are needed twice:** by the migration, and by `SeedCallback`, which inserts the seed exercises with raw `ContentValues` on a fresh install.
- **Mapping:** `toDomain` / `toEntity` in `TrainingRepository.kt` carry both fields; `addExercise` writes the exercise's setting.
- **Backup 6:** a row carries `unit` only when it is `PLATES` and `plateCount` only when it is `WHOLE` or `HALF`; absent means kg / off. So a log with no plates exports exactly as version 5 did but for the version number, and versions 1 to 5 restore and export again with no new key on any row. `toEntity` turns an unknown name into a corrupt file, as the other enums do. `decode` also returns `Corrupt("A plate set below plate 1")` for a set with `unit` `PLATES` and `weightKg < 1`. TARGET payloads stay an opaque string. The `Backup` doc comment gains its line for 6.

- [ ] **Step 1: Write the failing tests:**
  - `MigrationTest`: `migrate5to6_addsTheTwoColumns_andEveryStoredRowReadsKgAndOff` (a version 5 database with two exercises and three sets, written as version 5 stored them; `runMigrationsAndValidate(name, 6, true, MIGRATION_5_6)`; every `logged_set.unit` is `'KG'`, every `exercise.plateCount` is `'OFF'`, and each `weightKg`, `reps` and name is as written); `allMigrations_1to6_openInTheApp`.
  - `PlateCountStorageTest`: `aFreshInstall_seedsEveryExerciseOff`; `setPlateCount_changesOnlyThatRow_andNoSetOrAdvice` (the sets' and the advice table's rows are equal before and after: history is not rewritten); `setPlateCount_onACatalogueNameWithNoRow_insertsItsRowOnce` (called twice: one row, `HALF`); `aSetLoggedInPlates_keepsItsUnit_throughHistory`; `archiveThenRestore_keepsTheSetting`; `aSwappedInExercise_startsOff`.
  - `BackupTest`: `version6_roundTripsUnitAndPlateCount`; `aKgSetAndAnOffExercise_exportWithNoNewKey` (neither row's JSON has `unit` / `plateCount`; a plate set's last key is `"unit":"PLATES"` and a `HALF` exercise's `"plateCount":"HALF"`); `versions1to5_restoreWithEverySetKgAndEveryExerciseOff` (each frozen fixture); `aVersion6File_isTooNewForAVersion5Reader` (`decode(text, supportedVersion = 5)` is `TooNew(6)`); `anUnknownUnitOrSettingName_isCorrupt`; `aPlateSetBelowPlateOne_isCorrupt` (Review Focus 4); `aPlateTargetsPayload_comesBackByteForByte`.
  - `BackupFixtureTest`: the "what Android writes now" fixture becomes `android-v6.json`. It is what `android-v5.json` holds, with `"version": 6`, plus what `populate()` gains: two more exercises (one `WHOLE`, one `HALF`), plate sets on them including 7.5, one session holding a kg and a plate set of the same exercise, and a `PLATES` TARGET. Only those two exercises carry `plateCount` and only the plate sets carry `unit`; every other row is as in version 5. New tests: `androidFixture_holdsPlates` (those rows are there, and no kg set or off exercise in the file has the key) and `androidV5Fixture_restoresWithEverySetKgAndEveryExerciseOff` (frozen; its export equals `asUpgradedV5` of it). Then make the edits to existing tests that Global Constraints lists for `BackupTest.kt`, `BackupFixtureTest.kt` and `MigrationTest.kt`, and no others.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest` — they fail.
- [ ] **Step 3: Implement.** Build once so Room exports `6.json`.
- [ ] **Step 4: Write the fixture:** `./gradlew :app:testDebugUnitTest -Pfixtures.update=true`, and check `git status` shows only `android-v6.json` new under `__fixtures__/` (v1 to v5 untouched).
- [ ] **Step 5: Run** `./gradlew :app:testDebugUnitTest` — pass.
- [ ] **Step 6: Commit** by path (the sources, `6.json`, `android-v6.json`): `feat(android): sets store their unit, exercises their plate setting; backup version 6`.

### Task 5: Web storage and backup version 6

**Files:** modify `web/src/db/schema.ts`, `mapping.ts`, `catalogue.ts`, `web/src/backup/format.ts`, `backup.ts`; create `web/src/backup/__fixtures__/web-v6.json`. Tests: `web/src/db/db.test.ts`, `skips.test.ts` (its version pin only), `catalogue.test.ts`, `web/src/backup/backup.test.ts`.

**Interfaces — Consumes:** `JsSet.unit`, `JsExercise.plateCount`. **Produces:**

```ts
// schema.ts
export const LOAD_UNITS = ['KG', 'PLATES'] as const;        export type LoadUnit = (typeof LOAD_UNITS)[number]
export const PLATE_COUNTS = ['OFF', 'WHOLE', 'HALF'] as const; export type PlateCount = (typeof PLATE_COUNTS)[number]
export type SetRow = { ...; unit?: LoadUnit }               // written only as 'PLATES'; absent reads as KG
export type ExerciseRow = { ...; plateCount?: PlateCount }  // written only as 'WHOLE' or 'HALF'; absent reads as OFF
export function setUnit(row: { unit?: LoadUnit }): LoadUnit
export function plateCountOf(row: { plateCount?: PlateCount }): PlateCount
// SCHEMA_VERSIONS gains  { version: 6, stores: {} }  — no upgrade function and no new index
// catalogue.ts: one rw transaction over db.exercises; with a catalogueName the row is found, restored or inserted first. Resolves to the row's id.
// 'OFF' removes the key from the row; it never writes 'OFF'.
export function setPlateCount(db: GritDb, exerciseId: number | null, catalogueName: string | null, value: PlateCount): Promise<number>
// format.ts
export const BACKUP_VERSION = 6
export type ExerciseDto = { ...; plateCount?: string }
export type SetDto = { ...; unit?: string }
```

- **Version 6 stamps nothing (Planner's call, against the design's "stamps every set as kg and every exercise as off").** An absent field already reads as kg / off, and existing tests deep-equal rows across a whole upgrade (`skips.test.ts` `upgrade4to5_…`, `db.test.ts` `upgrade_noMatch_noChange`). Version 6 is still declared, for the version number and so `openGritDb` still writes the safety copy before the version changes; `safety.ts` is not changed. *(Corrected 2026-11-21: declaring it does not make an older app refuse a newer database. Dexie 4.4.7 opens a newer database whose stores match, after a `VersionError`, so a stale 1.16.1 page could read plate counts as kg and export backups without the plate markers. What protects instead: the new service worker reloads every window the old one controlled (reaches 1.16.1), and from 1.17 on `openGritDb` refuses a newer database with its own screen. See Task 9's notes.)*
- **One rule for every row the app writes:** the field is written only when it is not the default. `setFromJs` writes `unit` only for `PLATES`; `exerciseFromJs` writes `plateCount` only for `WHOLE` / `HALF`; `setToJs` / `exerciseToJs` pass `setUnit(r)` / `plateCountOf(r)`.
- **Backup 6:** the export writes `unit` only on a plate set and `plateCount` only on a plate-counted exercise, as the row's last key, as Android does. The parser takes them as optional keys, checks the names against `LOAD_UNITS` / `PLATE_COUNTS`, fills in `KG` / `OFF`, and refuses a `PLATES` set with `weightKg < 1` with the same message as Android. The doc comment gains its line for 6.

- [ ] **Step 1: Write the failing tests:**
  - `db.test.ts`: `upgrade 5 → 6 changes no row, and every set reads kg and every exercise off` (a version 5 database built with the version 5 stores; every table deep-equals what it held; `setUnit`, `plateCountOf` and `loadHistory` give `KG` / `OFF`); `the installed version is 6`; `the safety copy is written before version 6 is installed`.
  - `catalogue.test.ts`: `setPlateCount on a stored row changes only that row`; `setPlateCount back to OFF leaves the row exactly as it was before plates` (deep-equal, no `plateCount` key); `a kg set is stored with no unit key and a plate set with unit PLATES`; `setPlateCount with a catalogue name and no row inserts the row once, with the setting`; `no set and no advice row changes` (deep-equal before and after); `a restored archived row keeps its setting`; `an exercise swapped in by a plan change starts off`.
  - `backup.test.ts`: `version 6 round-trips unit and plateCount`; `a kg set and an off exercise export with no new key`; `versions 1 to 5 restore with every set kg and every exercise off` (each frozen fixture, both apps'); `a version 6 file is too new for a version 5 reader` (`decodeBackup(text, 5)` is `TOO_NEW`, version 6); `an unknown unit or plateCount name is corrupt`; `a plate set below plate 1 is corrupt` (Review Focus 4); `a plate target's payload comes back unchanged`; the "what this app writes now" fixture becomes `web-v6.json`: what `web-v5.json` holds, with `"version": 6`, plus what `buildSampleDb` gains behind a new `plates` option (the same additions as Task 4's fixture: a `WHOLE` and a `HALF` exercise, plate sets including 7.5, one mixed-unit session, a `PLATES` TARGET), with the keys on those rows only; `webFixture_holdsPlates`; `web-v5.json` joins the frozen list and restores through `withV5Defaults` untouched. Then make the edits to existing tests that Global Constraints lists for `backup.test.ts`, `skips.test.ts` and `db.test.ts`, and no others.
- [ ] **Step 2: Run** `npm test` in `web/` — they fail. **Step 3: Implement.**
- [ ] **Step 4: Write the fixture:** `UPDATE_FIXTURES=1 npm test`; `git status` shows only `web-v6.json` new under `__fixtures__/`.
- [ ] **Step 5: Run** `npm test && npx tsc -b` — pass.
- [ ] **Step 6: Commit** by path: `feat(web): sets store their unit, exercises their plate setting; backup version 6`.

### Task 6: Each app restores the other's version 6 backup

Runs after Tasks 4 and 5 are both merged (it needs both fixtures in one tree).

**Files:** tests only: `app/src/test/kotlin/com/powertracker/data/BackupFixtureTest.kt`, `web/src/backup/backup.test.ts`.

- [ ] **Step 1: Write the tests:**
  - Android `webV6Fixture_restoresOnAndroid_withItsPlates`: restore `web-v6.json`, export, and the export equals the fixture (`exportedAt` aside), which holds only if neither app writes a default; the plate sets keep `PLATES` and 7.5, the two exercises keep `WHOLE` and `HALF`, the `PLATES` target's payload is unchanged.
  - Web `restores android-v6.json with its plate sets, settings and plate target`: the same, from Android's file; and `the two version 6 fixtures hold the same keys`: a plate set from each file has the same keys in the same order, ending in `unit`; a plate-counted exercise from each likewise, ending in `plateCount`; a kg set and an off exercise from each have exactly the version 5 keys. If `web/e2e/backup.spec.ts` still expects version 5, change it to 6 here and run `npx playwright test e2e/backup.spec.ts --trace off`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest` and `npm test` — pass. If either fails, the two formats disagree: fix the app that departs from Task 4's and Task 5's Interfaces, regenerate only that app's v6 fixture, and say so in the commit.
- [ ] **Step 3: Commit** `test: each app restores the other's version 6 backup`.

### Task 7: Android Today: the setting, logging and showing plates

**Files:** create `ui/today/PlateCount.kt`; modify `ui/today/SkipToday.kt`, `TodayViewModel.kt`, `TodayScreen.kt`, `ExerciseCard.kt`, `Entry.kt`, `TodayDialogs.kt`, `SessionCards.kt` (the start-session card), `ui/components/SetRow.kt`, `Format.kt`, `ui/progress/ProgressScreen.kt`, `LiftHistoryScreen.kt`. Tests: new `app/src/test/kotlin/com/powertracker/ui/TodayPlatesViewModelTest.kt`, `ui/PlateCountScreenTest.kt`, `ui/PlatesProgressTest.kt`; add to `ui/today/EntryTest.kt`, `ui/design/ScreenshotTest.kt`.

**Interfaces — Consumes:** Tasks 1 to 4. **Produces:**

```kotlin
// TodayViewModel.kt: new LAST fields with defaults, so the guide's sample state and every existing test compile as they are
data class SetEditor(val weightKg: Double?, val reps: Int, val rpe: Double, val unit: LoadUnit = LoadUnit.KG)
data class TodayItem(..., val lastWeightKg: Double?, val unit: LoadUnit = LoadUnit.KG,
    val plateCount: PlateCount = PlateCount.OFF, val plateOffered: Boolean = false)
fun setPlateCount(exerciseId: Long, value: PlateCount)       // a stand-in id (negative) creates its catalogue row in the same write
// TodayScreen.kt: null means "no plate item", which is what the guide's TodayActions() gives
class TodayActions(..., val onCountPlates: ((exerciseId: Long, PlateCount) -> Unit)? = null)
// SkipToday.kt: the ⋮ button shows when either callback is given
fun SkipMenuButton(name: String, onSkipToday: (() -> Unit)?, modifier: Modifier = Modifier, onCountPlates: (() -> Unit)? = null)
// PlateCount.kt
const val COUNT_PLATES = "Count plates on this machine"
fun plateSheetLabel(name: String): String = "Count plates on $name"
@Composable fun PlateCountSheet(name: String, current: PlateCount, note: String?, onChoose: (PlateCount) -> Unit, onDismiss: () -> Unit)
fun plateNote(opened: LoadUnit, chosen: LoadUnit, hasSetsInOpenedUnit: Boolean): String?
// Entry.kt
enum class EntryKind { KG, REPS, RPE, PLATE, PLATE_HALF }     // both plate kinds read and write SetEditor.weightKg
enum class Error : EntryResult { EMPTY, NAN, RANGE, STEP }
// Format.kt (formatKg stays kg-only)
fun formatLoad(value: Double, unit: LoadUnit): String         // KG: formatKg(value) + " kg"; PLATES: PlateStack.text(value)
// ExerciseCard.kt, SetRow.kt: a trailing  unit: LoadUnit = LoadUnit.KG  on doseText(…) and SetRow(…)
```

Test tags: `exercise-menu-plates`, `plate-sheet`, `plate-option-OFF`, `plate-option-WHOLE`, `plate-option-HALF`, `plate-note`, `plate-done`.

**Wording (exact, the same on the web):**
- Menu item: **Count plates on this machine**, under Skip today when both show.
- Sheet: the exercise's name as its heading; **How does this machine show its weight?**; three radio choices: **In kg**; **Whole plates** with the hint **1, 2, 3 …**; **Half steps too** with the hint **1, 1½, 2 …**; the button **Done**. The sheet's accessible name is "Count plates on <name>" (Planner's call: the heading alone would share a name with the form guide's sheet).
- Note line (`plateNote`): null unless `chosen != opened && hasSetsInOpenedUnit`; to plates: **The coach starts this exercise fresh in plates. Your kg sets stay as they are.**; to kg (Planner's call, the design gives only the first): **The coach goes back to kg. Your plate sets stay as they are.** "Opened" is the unit when the sheet opened; whole ↔ half is never a change of unit.
- Field: label **Plate** (drawn as the other stepper labels are), no unit after the number, placeholder **plate**, accessible name **Weight, plates**.
- Refused entries: **Whole plates only** (`PLATE`, `STEP`); **Whole or half plates** (`PLATE_HALF`, `STEP`); **Plate 1 to 50** (`RANGE`, from `PlateStack.MAX_PLATE`).
- Typo dialog: **Did you mean plate 12 × 10?** and **That is a big jump from plate 3.**
- A set everywhere: "plate 7 × 12 @ 8"; spoken "Set 1: plate 7 and a half × 12 at RPE 8".

**Behaviour to build:**
- **A choice takes effect when tapped** (Planner's call); Done only closes. The menu item shows when `item.plateOffered && actions.onCountPlates != null`, on the open card, the folded row **and the start-session card's rows** (`StartSessionCard` in `SessionCards.kt` gains `onCountPlates: ((TodayItem) -> Unit)? = null` beside `onSkipToday`), planned or added, with or without sets. So the setting can be changed before a session starts; `setPlateCount` needs no session. Skip today keeps `canSkip`.
- **`build`:** `unit = PlateStack.unitOf(exercise)`; the stats are `HistoryStats(history.inCurrentUnits())`; the item's targets are those whose payload `unit` equals it; "the last set today" is the last one **in that unit**; `lastWeightKg` likewise; `perSide` is null in plates; a kept editor whose `unit` differs is ignored.
- **`setPlateCount`:** `training.setPlateCount(id.takeIf { it > 0 }, catalogueEntry(id)?.name, value)`, then drop the kept editor under the exercise's id and under its stand-in id.
- **The coach's pass:** its key gains each exercise's `(id, plateCount)`, so a change asks again; `known` and `distinctBy` use `(exerciseId, payload.unit)`. Nothing stored is deleted or rewritten.
- **`logSet`:** the set carries the item's unit; the typo check is `InputValidator.check(set.weightKg, set.reps, item.lastWeightKg, set.unit, PlateStack.step(item.plateCount))`.
- **The stepper in plates:** − and + through `PlateStack.down / up` with the exercise's step; + on an empty field gives 1; − is off at plate 1 and + at 50; the field shows 7 or 7.5 (Planner's call: the text the parser reads back; "7½" is used everywhere else); typed text is parsed as today, then judged by `PlateStack.check`; the keyboard is whole-number for `PLATE` and decimal for `PLATE_HALF`. The plate picture and its notes are not drawn.
- **`topSetIndex`** looks only at sets in the item's unit; each logged row prints in its own unit.
- **A logged set is never edited in another unit.** Neither app can change a logged set's weight, reps or RPE: only its miss point, and deletion. Both act on the stored set and keep its own unit, whatever the exercise's setting is now.
- **Elsewhere:** `targetHeadline` prints "plate 7 × 8–12 @ 8". On Android the unit already reaches Progress and history, because both hold domain `LoggedSet`s (`ExerciseTrend.bestSets`; `LiftHistoryViewModel`'s rows): only the printing changes. `ProgressScreen.kt`'s best-sets line (389) prints "plate 9 × 10" through `formatLoad`'s plate branch, and `LiftHistoryScreen.kt`'s `SetRow` call (182) passes `row.set.unit`.

- [ ] **Step 1: Write the failing tests** in `TodayPlatesViewModelTest.kt`:
  - `turningPlatesOn_emptiesTheField_whateverWasInIt` (Review Focus 1), three cases: 40 typed and kept in the editor; a kg target applied; a kg set logged today. After `setPlateCount(id, WHOLE)`: `editor.weightKg == null`, `editor.unit == PLATES`.
  - `theNextSetIsLoggedInPlates_andEarlierSetsKeepTheirUnit` (the session's stored sets read KG, KG, PLATES).
  - `aKgTargetIsNotShownOrAppliedForAPlateExercise_andNoStoredAdviceChanges`.
  - `switchingBackToKgTheSameDay_showsTheKgTargetAgain` and `turningPlatesBackOnWithOlderPlateHistory_storesAPlateTargetThatDay_besideTheKgOne` (Review Focus 2).
  - `theFieldFills_fromThisUnitOnly`: with the last plate top set at 7 and a later kg session at 60, the field starts at 7.
  - `theFirstPlateSetAfterKg_raisesNoBigJump_andPlate3To5Does` (Review Focus 3): `pending` is null for plate 7 after 60 kg; for 3 → 5 it holds `BigJump(3.0, PLATES)`.
  - `aStandIn_getsItsRowWhenPlatesAreChosen_andItsCardStays` (Lat pulldown added on Today with no row).
  - `plateOffered_isFalseForMainLifts_variations_andAssisted`.
  - `topSet_isMarkedAmongThisUnitsSetsOnly` (60 kg, 62.5 kg, then plate 7 and plate 8).
- [ ] **Step 2: Write the failing tests** in `PlateCountScreenTest.kt` and `EntryTest.kt`:
  - `theStartSessionCard_offersCountPlates_beforeASession_andTheChoiceIsStored` (no session today; ⋮ on Lat pulldown's row → the item → Whole plates: the exercise's row is `WHOLE`); `theStartSessionCard_withDefaultActions_hasNoPlateItem`.
  - `theMenuItem_showsOnAnOfferedExercise_alsoAfterItsFirstSet_andOnAnAddedOne`; `noMenuItem_onBench_onACustomVariation_onAssistedDip`; `benchWithASet_hasNoMenuAtAll`.
  - `theGuidesToday_hasNoPlateItem`: `TodayContent(state, TodayActions())` with Lat pulldown and a logged set shows no ⋮.
  - `theSheet_marksTheCurrentChoice_andAChoiceCallsOnCountPlates`; `theNote_showsOnlyForANewUnitWithSetsInTheOldOne` (kg → whole with kg sets: the plates sentence; whole → half: none; kg → whole with no sets: none; whole → kg with plate sets: the kg sentence).
  - `theStepper_inWholePlates` (+ on empty → 1; 7 → 8; − at 1 is off) and `inHalfSteps` (7 → 7.5); `afterHalfStepsAreTurnedOff_7AndAHalfGoesTo7And8`; `noPlatePicture_forAPlateExercise`.
  - `theFieldsAccessibleName_isWeightPlates`; `aPlateSetRow_readsPlate7AndAHalf_andSpeaksIt`; `theChip_readsPlate7Times8To12`.
  - In `PlatesProgressTest.kt`: `bodybuildingProgress_printsPlate9Times10` (a plate-counted exercise's row reads "plate 9 × 10", and a kg exercise's row beside it is as before); `exerciseHistory_printsEachSetInItsOwnUnit` (an exercise with 60 kg × 10 in one session and plate 7 × 10 in a later one: the first row reads "60 kg × 10", the second "plate 7 × 10", and neither is marked a record).
  - `EntryTest`: `plate_whole` ("7" ok; "7.5" → `STEP`, "Whole plates only"; "0" and "51" → `RANGE`, "Plate 1 to 50"); `plate_half` ("7.5" and "7,5" → 7.5; "7.25" → `STEP`, "Whole or half plates").
  - `theTypoDialog_namesPlates`.
  - `aSetLoggedBeforeTheSwitch_keepsItsUnit_whenItsMissPointIsSetOrItIsDeleted`: Today cannot edit a logged set's numbers on either app; it can only set a missed set's miss point (`setMissPoint`) and delete a set (`deleteSet`). With a missed 60 kg set logged, plates turned on and plate 7 logged: setting the kg set's miss point leaves it `KG` at 60 and its row reading "60 kg"; deleting the plate set leaves the kg set as it was; the selected-set line (`doseText`) prints each set in its own unit.
  - `at320dpAndTwiceTheFontSize_nothingIsCut` (Review Focus 5): the chip "plate 12½ × 8–12 @ 8", the stepper at 12.5 and the sheet are each fully within the screen, and `plate-done` is displayed.
- [ ] **Step 3: Run** `./gradlew :app:testDebugUnitTest` — they fail. **Step 4: Implement.**
- [ ] **Step 5: Screenshots** through `ScreenshotTest.render`: `today-plates-<side>-<scheme>` (the design's sketch: a chip, one logged plate set, the PLATE stepper) and `plate-sheet-<scheme>`, dark and light. Every committed Today and guide shot is byte-for-byte unchanged, or the report says why not.
- [ ] **Step 6: Run** `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest` — pass, no existing test edited (the existing "no menu" checks are all on main lifts; `SkipTodayScreenTest` has a known flake: rerun it alone before calling it one).
- [ ] **Step 7: Commit** `feat(android): count plates on Today: the setting, the stepper and plate sets`.

### Task 8: Web Today: the setting, logging and showing plates

**Files:** create `web/src/screens/today/PlateCount.tsx`; modify `screens/today/SkipToday.tsx`, `ExerciseCard.tsx`, `TodayScreen.tsx`, `TodayDialogs.tsx`, `entry.ts`, `today.css`, `state/useToday.ts`, `screens/today/SessionCards.tsx` (the start-session card), `ui/components/SetRow.tsx`, `format.ts`, `state/useProgress.ts`, `screens/progress/BodybuildingProgress.tsx`, `LiftHistoryScreen.tsx` (it draws the exercise history's rows too), `ExerciseHistoryScreen.tsx`. Tests: new `screens/today/PlateCount.test.tsx`, `state/useTodayPlates.test.tsx`, `web/e2e/plates.spec.ts`; add to `screens/today/entry.test.ts`, `state/useProgress.test.ts`, `web/e2e/narrow.spec.ts`.

**Interfaces — Consumes:** Tasks 1 to 3 and 5. **Produces:**

```ts
// useToday.ts
// Every new field is OPTIONAL and is PRESENT ONLY WHEN NOT THE DEFAULT: `unit` only as 'PLATES', `plateCount` only as 'WHOLE' / 'HALF',
// `plateOffered` only as true. So guideSampleState.ts compiles as it is, and the existing deep-equals of editors and items hold.
export type SetEditor = { weightKg: number | null; reps: number; rpe: number; unit?: LoadUnit }
export type Target = { ...; unit?: LoadUnit }                   // parseTarget sets it only for a PLATES payload
// useProgress.ts
export type BodybuildingRow = { ...; bestSets: { date: string; weightKg: number; reps: number; unit?: LoadUnit }[] }
export type TodayItem = { ...; unit?: LoadUnit; plateCount?: PlateCount; plateOffered?: boolean }
export type SetInput = { weightKg: number; reps: number; rpe: number | null; failed: boolean; unit?: LoadUnit }
// TodayActions gains an OPTIONAL member; useToday always supplies it; a stand-in key creates its catalogue row in the same write
setPlateCount?: (itemKey: number, value: PlateCount) => Promise<void>
// SkipToday.tsx: the ⋮ button renders when either callback is given
export function SkipMenu(props: { name: string; onSkipToday?: () => void; onCountPlates?: () => void }): JSX.Element
// PlateCount.tsx
export const COUNT_PLATES = 'Count plates on this machine'
export const plateSheetLabel = (name: string) => `Count plates on ${name}`
export function plateNote(opened: LoadUnit, chosen: LoadUnit, hasSetsInOpenedUnit: boolean): string | null
export function PlateCountSheet(props: { name: string; current: PlateCount; note: string | null; onChoose: (value: PlateCount) => void; onDismiss: () => void }): JSX.Element
// entry.ts
export type EntryKind = 'kg' | 'reps' | 'rpe' | 'plate' | 'plateHalf'
export type EntryResult = { ok: true; value: number } | { ok: false; reason: 'empty' | 'nan' | 'range' | 'step' }
// format.ts (formatKg stays kg-only)
export function formatLoad(value: number, unit: LoadUnit): string   // KG: `${formatKg(value)} kg`; PLATES: grit.plateText(value)
// ExerciseCard.tsx: ExerciseCard and ExerciseRow take an optional  onCountPlates?: () => void ; doseText gains a trailing  unit: LoadUnit = 'KG'
// SetRow.tsx: an optional  unit?: LoadUnit  prop (absent: kg, so the guide and the showcase are untouched)
```

Test ids match Task 7's tags (`exercise-menu-plates`, `plate-sheet`, `plate-option-OFF|WHOLE|HALF`, `plate-note`, `plate-done`). The sheet is a `dialog` named "Count plates on <name>" holding a `radiogroup` named by the question; the menu entry is a `menuitem`.

**Wording and behaviour** are Task 7's, word for word, with these web specifics:
- **The seam for the guide:** `TodayContent` works the menu out itself (as it does `canSkip`), and the guide draws `TodayContent` with `NO_TODAY_ACTIONS`. So the plate item is offered only when `item.plateOffered` **and** `actions.setPlateCount` is there; `NO_TODAY_ACTIONS` is not changed and has none.
- **`setPlateCount`:** `setPlateCount(db, standIn ? null : id, standInEntry(key)?.name ?? null, value)` from Task 5, then delete the kept editors `editor:<date>:<id>` and the stand-in's from `meta` and from local state, then run `coachPass` again and re-read.
- **`coachPass`:** `known` holds `` `${exerciseId}:${unit}` ``.
- **Reading a unit** is always `x.unit ?? 'KG'` (one helper); writing one follows the rule above, so a kg editor, target, set input and stored row look exactly as they did in 1.16.1.
- **Progress and history:** `useProgress.ts` drops the unit where it maps `trend.bestSets` (about lines 185 and 234): it carries `b.set.unit` across (only when `PLATES`), and `BodybuildingProgress.tsx`'s `bestText` prints "plate 9 × 10". `LiftHistoryScreen.tsx`'s `SetRow` call (74) passes the set's unit.
- **The start-session card:** `StartSessionCard` in `SessionCards.tsx` gains `onCountPlates?: (item: TodayItem) => void` beside `onSkipToday`, offered under the same two conditions as on the cards.
- **`build`:** as Android's; the last top set comes from `grit.lastTopSet`, which Task 2 made current-unit; an editor whose `unit ?? 'KG'` differs from the item's is ignored (this also covers a second tab and an editor kept across a reload).
- **Typo check:** `grit.checkLoadInput(set.weightKg, set.reps, lastWeightKg, set.unit, item.plateCount)`.
- **Stepper and entry:** `grit.plateUp / plateDown / plateCheck / maxPlate`; `inputMode` `numeric` for `plate`, `decimal` for `plateHalf`.
- **Nothing Chromium-only,** as on the rest of Today.

- [ ] **Step 1: Write the failing tests** in `useTodayPlates.test.tsx`: every test of Task 7 Step 1 under the same names in the web's style, plus `an editor kept in meta from before the switch is not put in the PLATE field after a reload` (Review Focus 1) and `the kept editor rows for the exercise and its stand-in are deleted on a change of unit`. Task 7 Step 2's `aSetLoggedBeforeTheSwitch_keepsItsUnit_whenItsMissPointIsSetOrItIsDeleted` goes here too (the web's `setMissPoint` updates only `missPoint`, so the stored row's `unit` is asserted after it).
- [ ] **Step 2: Write the failing tests** in `PlateCount.test.tsx` and `entry.test.ts`: Task 7 Step 2's tests under the same names, the start-session card's two included (the 320 px one is Step 6's e2e here); in `useProgress.test.ts`, `bodybuildingProgress_printsPlate9Times10` and `exerciseHistory_printsEachSetInItsOwnUnit` as in Task 7; `a kg editor, target and set input carry no unit key` (deep-equal to their 1.16.1 shapes); plus `the guide's Today picture has no plate item` (render the guide's Today picture on the bodybuilding side at its last step: no "Options for Lat pulldown" button).
- [ ] **Step 3: Run** `npm test` — they fail. **Step 4: Implement.** Run `npm test && npx tsc -b && npm run build` — pass, no existing test edited.
- [ ] **Step 5: Commit** `feat(web): count plates on Today: the setting, the stepper and plate sets`. The branch can be merged from here.
- [ ] **Step 6 (main checkout only, after the merge): e2e** in `plates.spec.ts`: `count plates end to end` (plan Lat pulldown, start a session, log 60 kg, ⋮ → Count plates on this machine → Whole plates → Done; the field is empty and reads PLATE; + gives 1; type 7, log; the row reads "plate 7 × …"; reload: still plates, the kg set still "60 kg"); `half steps` (7 → 7.5, the row reads "plate 7½"); `no plate item on Bench or on Assisted dip`; `a stand-in gets its row`. In `narrow.spec.ts`: `plates at 320 px and 200 % text` (Review Focus 5): no horizontal scroll, the chip, the stepper and the sheet's choices inside the viewport, Done clickable. Screenshots with a local `shoot` helper defined in `plates.spec.ts`, as the other specs define theirs (there is no shared one): `today-plates-<side>-<scheme>`, `plate-sheet-<scheme>`. Run `npx playwright test --trace off`; the committed Today and guide shots are unchanged.
- [ ] **Step 7: Commit** `test(web): plates end to end, narrow, shots`.

### Task 9: Version 1.17, notes, phone checks, full run

- [ ] **Version** 1.17 on both apps: `versionCode = 28`, `versionName = "1.17"` in `app/build.gradle.kts`; `"version": "1.17.0"` in `web/package.json` and `package-lock.json` (as 1.16 was written there). Reshoot any shot that prints the version.
- [ ] **`docs/next-update.md`:** a section "Done: 1.17, counting plates on stack machines (both apps)", in the house form: what the lifter gets; the owner's four decisions; every Planner's call in this plan with its reason; where Android and the web differ; what was not built (no conversion, no chart, no custom variations, assisted machines stay in kg, no per-gym profiles); the findings under "Where the code differs from the design"; backup version 6 (update both apps together); a 1.17 backup is refused by 1.16.1 and older; the web app updates itself but the Android app does not, so update Android before restoring a newer backup there; follow-up (which way the assisted machines count is its own release). Also in the notes:
  - Never roll the web deploy back below 1.17 once it is out: fix forward.
  - Stale copies on the web: when a new service worker takes over it reloads every Grit window the old one controlled, so a window on "open in another tab" or the lock screen does not stay on old code (Part A, reaches 1.16.1); from 1.17 on, a copy older than the stored log shows "Update Grit to carry on" and opens nothing, with only "Update and reload" (Part B).
- [ ] **README phone checks,** added to each of the three lists (iPhone, Android web, Android app):
  - Count plates: on Today, ⋮ on Lat pulldown → Count plates on this machine → Whole plates. The field reads PLATE and is empty; + gives 1. Log plate 7 × 10: the row reads "plate 7 × 10". Bench has no such item.
  - Half steps: choose Half steps too; + goes 7, 7½, 8. Back on Whole plates, typing 7.5 is refused with "Whole plates only".
  - The next session: the coach's chip is in plates ("plate 7 × …"), never kg, and Progress (bodybuilding) shows "plate 7 × 10".
  - Switch back to In kg: the old kg sets and the kg target are back; the plate sets still read "plate …" in the exercise's history.
  - Backup: export on one app, restore on the other: the plate sets and the setting come across. (A 1.16 app says the file is too new.)
  - Large text: nothing in the sheet or the stepper is cut.
- [ ] **Full run,** one after another, in the main checkout: `./gradlew :domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest --rerun-tasks`; in `web/`: `npm test`, `npx tsc -b`, `npm run build`, `npx playwright test --trace off`, the speed project 3 times.
- [ ] **Commit** `chore: version 1.17, notes`.
- [ ] **Stop before the merge.** The owner sees the plate shots (Today and the sheet, dark and light, both apps) and the note in `docs/next-update.md`.

---

## Planner's calls (all in one place)

1. **Names:** `LoadUnit { KG, PLATES }`, `PlateCount { OFF, WHOLE, HALF }`; columns `logged_set.unit`, `exercise.plateCount`; the same two keys in Dexie rows, backup JSON and (unit only) the TARGET payload. `weightKg` is not renamed. Outside Room a default is never written: the key is there only for plates.
2. **"Current unit" is one history filter** (`inCurrentUnits()`), not a unit parameter on each `HistoryStats` call.
3. **Eligibility where the design's two rules meet:** the row's own category decides; a custom accessory is offered whatever its name, except a name with the word "assisted" ("Unassisted row" is still offered); the three assisted machines are excluded by that same word.
4. **A stored setting on an exercise that is not offered counts as off** (`unitOf`), so a main lift can never be in plates even from an edited backup.
5. **Typed plates run from 1 to 50,** with the message "Plate 1 to 50".
6. **Powerlifting side:** a repeat keeps last time's reps when they are above the plan's (the owner confirmed this on 2026-11-20); the extra rep never aims under the plan's reps; a logged half with Whole plates on gives the lower whole plate for a repeat, an added rep and a step down.
7. **No 5 % cap in plates** (the design implies it; the plan states it).
8. **The typo check** gets a second form and a new JS export (`checkLoadInput`) instead of a changed `checkInput`, so each task ends green.
9. **Backups refuse a plate set under plate 1** on both apps.
10. **The sheet:** a choice applies when tapped; its accessible name is "Count plates on <name>"; the note for going back to kg is "The coach goes back to kg. Your plate sets stay as they are."
11. **The PLATE field shows 7.5,** while every other place prints "7½".
12. **The editor carries its unit** (on the web, only when it is plates), and a kept editor in another unit is ignored as well as deleted.
13. **A pain skip's deletion of that day's OPEN targets is left as it is** (`deleteOpenTargets` on Android and its web twin go by exercise and date, so they take both units' OPEN targets; the coach's pass makes the current unit's again).
14. **Backup fixtures:** each app writes its own v6 fixture in its storage task; the cross-restore is its own small task after both merge.
15. **Dexie version 6 stamps nothing,** against the design's "stamps every set as kg and every exercise as off": absent already reads as kg / off, and existing tests deep-equal rows across an upgrade. The version is still declared (for the number and the safety copy; it does not make an older app refuse the database, see Task 9's notes).
16. **The plate item is also on the start-session card,** so the setting can be changed before a session starts (the design names only "the exercise's menu on Today").
17. **`FatigueRule` skips non-kg main-lift sets,** though the design calls it unchanged: it reads raw `weightKg`, and the guard costs a real log nothing.

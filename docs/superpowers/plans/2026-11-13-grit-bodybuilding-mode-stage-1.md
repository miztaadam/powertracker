# Bodybuilding Mode, Stage 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Settings switch that turns Grit into its bodybuilding side: its own plan and training days, rep-range targets (reps first, then weight), a blue look with a "Bodybuilding" label, one shared training log, and a welcome-back prompt, in the Android and web apps together.

**Architecture:** The mode lives in the shared Kotlin domain: a `TrainingMode` on plan items, sessions and advice, a `BodybuildingCoach` beside the existing `Coach`, and one entry point, `Coaching`, that both apps call. Each app stores the mode fields (Room version 3, Dexie version 3, backup version 3), swaps a second colour palette in at its theme root, and shows the active side's plan, advice and Progress.

**Tech Stack:** Kotlin Multiplatform (`domain/`, JS bridge via `:domain:syncWebDomain`), Android Compose + Room + DataStore (`app/`), React + Vite + Dexie (`web/`), Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-13-grit-bodybuilding-mode-design.md` (stage 1 is §5, §6, §7, §10). Read it with this plan; where they differ, the spec wins and you say so.

## Global Constraints

- The powerlifting side does not change while the mode is off: screens, wording, coach output, committed screenshots. Existing tests pass unedited except where a signature gains a parameter. The one exception is Settings (new group; version line shows 1.2).
- One training log: never split, duplicate or drop sessions or sets.
- The coach never changes a plan without a tap.
- Every training number that is not the owner's is cited to an allowed source (acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport), found with WebSearch restricted to those domains (`allowed_domains`); never fetch those sites directly. The owner's own numbers: 8-12 reps, RPE 8, 3 sets default.
- Both apps ship together: backup version 3 on both; app version 1.2 on both (Task 10).
- Rep ranges print with an en dash: `3 × 8–12 @ 8`.
- Palette (spec §7.2), exact values; adjust a colour only if a contrast test fails, by the least that passes.
- Plain words in comments, commit messages and reports. No model names in code or commit bodies other than your harness's attribution trailer. New commits only: never amend, never push, never touch `main`.
- Never run `playwright install`. End-to-end runs use `--trace off`.
- `.claude/` and `CLAUDE.md` are local files: never commit them. Stage your own paths by name, never `git add -A`.
- After any change under `domain/src/commonMain` or `jsMain`: `./gradlew :domain:syncWebDomain :domain:checkWebDomain` and commit `web/src/domain/generated/`.
- Known flakes: `GoldenScenarioTest.noStrictPressHistory_coachOutputUnchanged` (jsNodeTest, 2 s mocha timeout: re-run alone); the @speed picker checks miss 100 ms about one run in ten (re-run the speed project once; do not change them).

## Review Focus

1. **A log made only on the powerlifting side** must give byte-identical coach output through `Coaching` as through `Coach` today (Task 3 test).
2. **Old data after the upgrade:** a database or backup from before this stage opens with every plan item, session and advice on the powerlifting side and nothing lost (Tasks 4, 5).
3. **Switching sides on a day already trained:** Today stays on that day's session with its own plan; the log gains no second session for the date (Tasks 8, 9).
4. **A bodybuilding plan with an exercise that has no history at all:** no suggestion, no crash, the lifter can still log it (Task 3, Tasks 8, 9).
5. **A lift untrained for six weeks when the lifter returns to a side:** its first target is eased in, never the old top set plus an increase (Task 2).

---

## File map

| Area | Files | Tasks |
|---|---|---|
| Domain model | `domain/.../model/Models.kt`, `model/Advice.kt`, new `model/Sides.kt` | 1 |
| Domain coach | `coach/Coach.kt`, `coach/FatigueRule.kt`, new `coach/ReturnRule.kt`, `e1rm/E1rm.kt` | 2 |
| Domain coach | new `coach/BodybuildingCoach.kt`, `coach/Coaching.kt`, `coach/CoachSources.kt`, `progress/BodybuildingProgress.kt` | 3 |
| JS bridge | `domain/src/jsMain/.../web/GritDomain.kt`, `JsTypes.kt`, `web/src/domain/generated/` | 1, 2, 3 |
| Android storage | `app/.../data/db/Entities.kt`, `Daos.kt`, `AppDatabase.kt`, `data/SettingsRepository.kt`, `data/Backup.kt`, repositories | 4 |
| Web storage | `web/src/db/schema.ts`, `db.ts`, `settings.ts`, `web/src/backup/format.ts`, `backup.ts` | 5 |
| Android look | `app/.../ui/theme/Color.kt`, `Theme.kt`, screen headers, `ui/plan/SettingsScreen.kt` | 6 |
| Web look | `web/src/ui/tokens.css`, `theme.ts`, screen headers, `screens/settings/SettingsScreen.tsx` | 7 |
| Android screens | `app/.../ui/plan/`, `ui/today/`, `ui/coach/`, `ui/progress/` | 8 |
| Web screens | `web/src/screens/{plan,today,coach,progress}/`, `web/src/state/` | 9 |
| Release | `app/build.gradle.kts`, `web/package.json`, `docs/next-update.md`, READMEs | 10 |

Tasks 1 to 3 run in order. Then two tracks run side by side: Android (4, 6, 8) and web (5, 7, 9). The Android track edits only `app/` and `docs/design/screens/`; the web track only `web/` and `docs/design/web/screens/`. Neither track edits `domain/`. Task 10 runs last.

---

### Task 1: The mode in the shared model

**Files:**
- Modify: `domain/src/commonMain/kotlin/com/powertracker/domain/model/Models.kt`, `model/Advice.kt`
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/model/Sides.kt`
- Modify: `domain/src/jsMain/kotlin/com/powertracker/domain/web/JsTypes.kt`, `GritDomain.kt`; regenerate `web/src/domain/generated/`
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/model/SidesTest.kt`

**Interfaces — Produces:**
```kotlin
enum class TrainingMode { POWERLIFTING, BODYBUILDING }
// New trailing parameters, all with these defaults, so every existing call compiles unchanged:
data class PlanItem(..., val mode: TrainingMode = TrainingMode.POWERLIFTING, val repsHigh: Int? = null)
data class LoggedSession(..., val mode: TrainingMode = TrainingMode.POWERLIFTING)
data class Advice(..., val mode: TrainingMode = TrainingMode.POWERLIFTING)
AdvicePayload.Target(..., val repsHigh: Int? = null)   // null: fixed reps
// Sides.kt
fun TrainingHistory.on(mode: TrainingMode): TrainingHistory      // same exercises and baselines, sessions of that side
fun List<PlanItem>.on(mode: TrainingMode): List<PlanItem>
fun List<Advice>.forSide(mode: TrainingMode): List<Advice>       // every DELOAD and RE_ENTRY, plus the rest where mode matches
fun repsText(reps: Int, repsHigh: Int?): String                  // "5", or "8–12" (U+2013)
```
`JsPlanItem`, `JsSession` and the JS advice type gain `mode: String` (`"POWERLIFTING"` / `"BODYBUILDING"`) and `repsHigh: Int?`, defaulted the same way; `GritDomain` exposes `repsText`.

- [ ] **Step 1:** Write `SidesTest`: `on` keeps only that side's sessions and leaves exercises and baselines; `forSide(BODYBUILDING)` returns a POWERLIFTING deload and re-entry but not a POWERLIFTING stall; `repsText(5, null) == "5"`, `repsText(8, 12) == "8–12"`; a `PlanItem` built with the old seven arguments is POWERLIFTING with `repsHigh == null`; a stored `Target` payload JSON without `repsHigh` still decodes, and a Target with `repsHigh == null` encodes without the key.
- [ ] **Step 2:** Run `./gradlew :domain:jvmTest --tests '*SidesTest*'`. Expected: does not compile.
- [ ] **Step 3:** Add the enum, the defaulted fields, `Sides.kt`, and the JS types.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain` and `cd web && npm test && npm run typecheck`. Expected: all pass; no web source change beyond `generated/` unless a constructor call needs the new arguments.
- [ ] **Step 5:** Commit `feat(domain): plan items, sessions and advice know their side`.

### Task 2: The powerlifting coach reads its own side; the return rule

**Files:**
- Modify: `coach/Coach.kt`, `coach/FatigueRule.kt`, `e1rm/E1rm.kt`
- Create: `coach/ReturnRule.kt`
- Test: `commonTest/.../coach/CoachSidesTest.kt`, `ReturnRuleTest.kt`, additions to `FatigueRuleTest.kt`; `GoldenScenarioTest` unchanged and passing

**Interfaces — Consumes:** Task 1. **Produces:**
```kotlin
// Signatures unchanged. Callers pass the WHOLE history; Coach narrows it itself.
Coach.evaluate(history, plan, settings, today, priorAdvice)
Coach.targetsFor(day, history, plan, settings, today, priorAdvice)
object E1rm { fun loadFor(e1rmKg: Double, reps: Int, rpe: Double?): Double }   // inverse of estimate
internal object ReturnRule {
    const val AWAY_DAYS = 28L
    /** [target] eased for a side returned to; unchanged when the exercise was trained on [mode] within AWAY_DAYS. */
    fun ease(target: Advice, item: PlanItem, exercise: Exercise, history: TrainingHistory, mode: TrainingMode,
             settings: CoachSettings, today: LocalDate): Advice
}
```
Behaviour (spec §5.2):
- Progression, minimum history, stall and weak point read `history.on(POWERLIFTING)`.
- `FatigueRule`: the top-set, failed-set, e1RM-trend and RPE-at-the-same-load signals read powerlifting sessions; the readiness signal reads every session.
- The 14-day gap, `lastSessionDate` and re-entry read every session.
- A DELOAD or RE_ENTRY in `priorAdvice` counts whatever its `mode`.
- `ReturnRule.ease`: "family" is the main lift and its variations by `parentLift` for POWERLIFTING, the exercise itself for BODYBUILDING. No family set on `mode` in the last 28 days, then: (a) family sets on the other side in those 28 days: cap the target's weight at `E1rm.loadFor(best recent estimate, target reps, target rpe)`, rounded down with `ProgressionRule.roundDown`; (b) no family set on either side: weight is the last top set on this side times `AdvicePayload.ReEntry().loadFactor`, rounded down, reps as planned, and the reason gains the existing easing-back-in note. Never above the target that came in.

- [ ] **Step 1:** Write the failing tests: (1) for each `GoldenScenarioTest` history, adding bodybuilding sessions of 3 × 10 on bench between the powerlifting ones leaves `evaluate` and `targetsFor` output equal to the output without them, when the last powerlifting session of each lift is within 28 days; (2) fatigue: 3 × 10 bodybuilding sessions after fives raise no e1RM-down or RPE-rising signal, while three low-readiness bodybuilding sessions do raise the readiness signal; (3) `loadFor(estimate(w, r, rpe), r, rpe)` is `w` within 0.01 for w in 40..200, r in 1..12, rpe in {null, 7, 8, 9}; (4) return, branch (a): deadlift last pulled on the powerlifting side 42 days ago, Romanian deadlift (its variation) done on the bodybuilding side last week: target weight is at most what those sets support and at most the incoming target; (5) return, branch (b): deadlift family untrained 42 days on either side, sessions of other lifts every week (so no 14-day gap): target is `roundDown(lastTop × 0.85)`, never `lastTop + 2.5`; (6) an OPEN deload stamped BODYBUILDING clamps powerlifting targets and blocks a new deload.
- [ ] **Step 2:** Run `./gradlew :domain:jvmTest --tests '*CoachSidesTest*' --tests '*ReturnRuleTest*' --tests '*FatigueRuleTest*'`. Expected: the new tests fail.
- [ ] **Step 3:** Implement. `targetsFor` applies `ReturnRule.ease` after `ProgressionRule.target` and before `clamp`.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`. Expected: pass; `GoldenScenarioTest` untouched.
- [ ] **Step 5:** Commit `feat(domain): each coach reads its own side; easing in after time on the other side`.

### Task 3: Bodybuilding targets, the one entry point, Progress rows, welcome back

**Files:**
- Create: `coach/BodybuildingCoach.kt`, `coach/Coaching.kt`, `coach/CoachSources.kt`, `progress/BodybuildingProgress.kt`
- Modify: `domain/src/jsMain/.../GritDomain.kt`, `JsTypes.kt`; regenerate the bundle
- Test: `commonTest/.../coach/BodybuildingCoachTest.kt`, `CoachingTest.kt`, `progress/BodybuildingProgressTest.kt`

**Interfaces — Consumes:** Tasks 1, 2. **Produces:**
```kotlin
object Coaching {   // the apps call this, never Coach or BodybuildingCoach directly
    fun targetsFor(mode: TrainingMode, day: DayOfWeek, history: TrainingHistory, plan: List<PlanItem>,
                   settings: CoachSettings, today: LocalDate, priorAdvice: List<Advice>): List<Advice>
    fun evaluate(mode: TrainingMode, history: TrainingHistory, plan: List<PlanItem>, settings: CoachSettings,
                 today: LocalDate, priorAdvice: List<Advice>): List<Advice>
    /** The welcome-back card: this side has a plan and was left more than 30 days before [today]. */
    fun welcomeBack(leftOn: LocalDate?, today: LocalDate, hasPlan: Boolean): Boolean
}
object BodybuildingCoach {
    const val INCREASE: Double            // the sourced fraction, see CoachSources
    fun targetsFor(day, history, plan, settings, today, priorAdvice): List<Advice>
    /** Reps to put in the reps field for set [setNumber] (1-based): last time's reps for that set + 1, capped at repsHigh; the low end the first time and after an increase. */
    fun repsToBeat(item: PlanItem, history: TrainingHistory, setNumber: Int, today: LocalDate): Int
}
data class ExerciseTrend(val exerciseId: Long, val name: String, val bestSets: List<Pair<LocalDate, LoggedSet>>, val goingUp: Boolean?)
object BodybuildingProgress { fun rows(history: TrainingHistory, plan: List<PlanItem>, today: LocalDate): List<ExerciseTrend> }
```
`Coaching` takes the whole plan and whole advice list, narrows them with `.on(mode)` / `.forSide(mode)`, delegates to `Coach` or `BodybuildingCoach`, and stamps `mode` on what it returns. `Coaching.evaluate(BODYBUILDING, ...)` in stage 1 returns only re-entry and the readiness deload.

Bodybuilding target for one plan item (spec §5.3). "Last time" is the exercise's sets in its most recent BODYBUILDING session before `today`:
- At least `item.sets` sets, none failed, each with `reps >= repsHigh` and `rpe <= targetRpe` (no RPE counts as on target): **increase**. Next weight is `last × (1 + INCREASE)` rounded to the nearest loadable weight and at least one step above `last`: the step is `PlateLoader.step(settings.changePlates)` on the plate grid for a barbell load, 0.5 kg for a free load (`ProgressionRule.isFreeLoad`). `reps = item.reps` (the low end).
- Otherwise: **repeat** the last weight.
- No bodybuilding session yet: with any powerlifting set of that exercise in the last 28 days, a starting weight of `E1rm.loadFor(best estimate, middle of the range rounded down, targetRpe)` rounded down; with none, no advice for that item.
- Then `ReturnRule.ease(..., BODYBUILDING, ...)` and the same deload and re-entry clamps the powerlifting targets get (load factor and fewer sets; the range is kept).
- Payload: `Target(exerciseId, weightKg, reps = item.reps, rpe = item.targetRpe, sets = clamped or null, repsHigh = item.repsHigh)`.
- Reason, exactly: repeat `Last time: 26 × 11, 10, 9. Beat it. Coach suggests 26 × 8–12 @ 8.`; increase `Last time: 26 × 12, 12, 12. Time to add weight. Coach suggests 27.5 × 8–12 @ 8.`; first time `First time on this side. Coach suggests 60 × 8–12 @ 8.` Weights print as the existing coach prints them (no trailing `.0`).

`BodybuildingProgress.rows`: one row per distinct exercise in the BODYBUILDING plan, in plan order; `bestSets` is, for each of its last four bodybuilding sessions (oldest first), the set with the most reps at that session's heaviest weight; `goingUp` is null with fewer than two sessions, else true when the newest session's heaviest weight is above the one before, or equal with more total reps.

`CoachSources.kt`: the citation for `INCREASE` (URL, what the page says, date checked), in the style of `library/Sources.kt`. Search the allowed domains for load-progression guidance for resistance training (ACSM's progression models and NSCA's guidance are the likely sources). Use the value the source gives; if it gives a band, use its low end. If the source gives a different trigger, keep the owner's trigger and note the difference there. If no allowed source supports a size, stop and report.

- [ ] **Step 1:** Write the failing tests. `BodybuildingCoachTest`: each branch above with these cases: 26 kg dumbbell press 11/10/9 at RPE 8, range 8–12 gives repeat at 26 with the exact reason; 12/12/12 at RPE 8 gives increase to a weight strictly above 26 on the 0.5 kg grid; 12/12/12 with one set at RPE 9 gives repeat; only two of three planned sets gives repeat; a barbell lift at 60 kg 12/12/12 gives an increase on the plate grid of at least `PlateLoader.step`; no history gives no advice; powerlifting bench 100 × 5 @ 8 last week gives a first-time weight below 100 with the exact first-time reason; an open deload gives the same range, a lower weight and fewer sets. `repsToBeat`: 11/10/9 gives 12, 11, 10; after an increase gives the low end. `CoachingTest`: for every `GoldenScenarioTest` history, `Coaching.evaluate(POWERLIFTING, ...)` and `targetsFor(POWERLIFTING, ...)` equal `Coach`'s output element for element; advice returned for BODYBUILDING carries that mode; `welcomeBack(null, ...)` false, 30 days false, 31 days true, 31 days with no plan false. `BodybuildingProgressTest`: best set and `goingUp` for a heavier week, an equal-weight-more-reps week, a worse week, a single session.
- [ ] **Step 2:** Run those three test classes. Expected: do not compile.
- [ ] **Step 3:** Find and record the source, then implement. Expose `Coaching`, `repsToBeat`, `BodybuildingProgress.rows` and `welcomeBack` through `GritDomain` with JS types that follow the existing ones.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain` and `cd web && npm test && npm run typecheck`. Expected: pass.
- [ ] **Step 5:** Commit `feat(domain): bodybuilding targets, reps first then weight`.

### Task 4: Android storage, settings and backup

**Files:** `app/src/main/kotlin/com/powertracker/data/db/Entities.kt`, `Daos.kt`, `AppDatabase.kt`, the mappers and repositories that build `PlanItem`, `LoggedSession`, `Advice`; `data/SettingsRepository.kt`; `data/Backup.kt`, `BackupService`; schema JSON under `app/schemas/`. Tests: `app/src/test/.../data/MigrationTest.kt`, the backup and settings tests beside it. Create `app/src/test/resources/backup-v3-android.json`.

**Interfaces — Consumes:** Task 1. **Produces:**
- Room version 3 with `MIGRATION_2_3`: `plan_item.mode TEXT NOT NULL DEFAULT 'POWERLIFTING'`, `plan_item.repsHigh INTEGER` (nullable), `session.mode` and `advice.mode` the same as `plan_item.mode`.
- `AppSettings` gains `trainingMode: TrainingMode = POWERLIFTING`, `bodybuildingTrainingDays: Set<DayOfWeek>` (copied from `trainingDays` the first time the mode is turned on), `powerliftingLeftOn: LocalDate?`, `bodybuildingLeftOn: LocalDate?`.
- `SettingsRepository.setTrainingMode(mode: TrainingMode, today: LocalDate)`: stores the mode, stamps the side being left with `today`, copies the training days on first use. `clearLeftOn(mode)`. `AppSettings.activeTrainingDays` returns the active side's days.
- Plan reads and writes take the side: the plan repository's observe and replace functions gain a `mode` parameter and only touch that side's rows. A new session is stamped with the active mode; new advice with its `mode`.
- `Backup.VERSION = 3`: settings fields above; `mode` and `repsHigh` on plan items; `mode` on sessions and advice. Version 1 and 2 files restore as all powerlifting. Parsing stays strict.

- [ ] **Step 1:** Write the failing tests: migration 2 to 3 on a version 2 database with plan items, sessions, sets and advice keeps every row and stamps POWERLIFTING, `repsHigh` null; a bodybuilding plan saved beside a powerlifting one leaves the powerlifting rows untouched and each read returns only its side; `setTrainingMode` stamps the left side and copies days once; backup version 3 round trip with both sides' plans, a bodybuilding session and both left-on dates; the committed version 2 fixture restores as all powerlifting; a version 4 file is TooNew.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*MigrationTest*' --tests '*Backup*' --tests '*Settings*'`. Expected: fail.
- [ ] **Step 3:** Implement. Every existing caller passes the active mode; behaviour with the mode off is unchanged. Write `backup-v3-android.json` from the round-trip test's data (both sides' plans, one session on each side, one advice on each side) and commit it.
- [ ] **Step 4:** Run `./gradlew :app:testDebugUnitTest`. Expected: pass, no screenshot changed.
- [ ] **Step 5:** Commit `feat(app): storage and backup carry the training side`.

### Task 5: Web storage, settings and backup

**Files:** `web/src/db/schema.ts`, `db.ts`, `settings.ts`, the row mappers and state hooks that build plan items, sessions and advice; `web/src/backup/format.ts`, `backup.ts`. Tests beside each (`db.test.ts`, `backup.test.ts`, settings tests). Create `web/src/backup/fixtures/backup-v3-web.json`.

**Interfaces — Consumes:** Task 1. **Produces:** the same shape as Task 4, in TypeScript:
- `SCHEMA_VERSIONS` gains version 3: `planItems: '++id, weekday, exerciseId, mode'`; its upgrade sets `mode: 'POWERLIFTING'` on every plan item, session and advice row. Row types gain `mode: 'POWERLIFTING' | 'BODYBUILDING'` and, on plan items, `repsHigh?: number`.
- `AppSettings` gains `trainingMode`, `bodybuildingTrainingDays`, `powerliftingLeftOn?`, `bodybuildingLeftOn?` (ISO dates); `setTrainingMode(mode, today)`, `clearLeftOn(mode)`, `activeTrainingDays(settings)`.
- Plan reads and writes take the side. New sessions and advice are stamped.
- `BACKUP_VERSION = 3`, mirroring Task 4 field for field, same JSON key names as Android's.

- [ ] **Step 1:** Write the failing tests, the web twins of Task 4's (upgrade 2 to 3 on real version 2 rows, as the file's rule requires; sides do not touch each other; settings; backup round trip; version 2 restores as powerlifting; version 4 is too new).
- [ ] **Step 2:** Run `cd web && npx vitest run src/db src/backup`. Expected: fail.
- [ ] **Step 3:** Implement; write and commit `backup-v3-web.json` with the same content as described in Task 4.
- [ ] **Step 4:** Run `cd web && npm test && npm run typecheck`. Expected: pass.
- [ ] **Step 5:** Commit `feat(web): storage and backup carry the training side`.

### Task 6: Android look, label and switch

**Files:** `app/.../ui/theme/Color.kt`, `Theme.kt`; the root composable that applies the theme; the header composable(s) of Today, Coach, Progress, Exercises, Plan and Settings; `ui/plan/SettingsScreen.kt`; `app/src/test/.../ui/design/TokenContrastTest.kt`, `ScreenshotTest.kt`; new PNGs in `docs/design/screens/`.

**Interfaces — Consumes:** Task 4's settings. **Produces:** `PowertrackerTheme(mode: TrainingMode = POWERLIFTING, darkTheme: Boolean = isSystemInDarkTheme(), ...)`; `ModeLabel()` composable, drawn only on the bodybuilding side, `testTag("mode-label")`, text `BODYBUILDING`; Settings group **Training style**, first on the screen, switch **Bodybuilding mode** with the supporting line `Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept.`, `testTag("bodybuilding-mode")`.

- [ ] **Step 1:** Write the failing tests: contrast tests run over the two new palettes (the spec §7.2 values); with the mode on, each of the six top-level screens shows `mode-label` and the theme's accent is the blue for dark and for light; with it off there is no `mode-label` anywhere; tapping the switch calls `setTrainingMode` and the theme changes; the switch is the first group in Settings.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement the two palettes (the non-accent, non-surface colours are shared with the existing sets), the label under each header's date or overline, and the switch.
- [ ] **Step 4:** Add screenshot cases `settings-bodybuilding-{dark,light}` and `today-bodybuilding-{dark,light}` (the others come in Task 8). Run `./gradlew :app:testDebugUnitTest -Pscreenshots.update=true`, then `git status`: only the two Settings screenshots (new group) and the new PNGs may differ; the other existing PNGs must be byte-identical. Read the new PNGs, dark and light: the blue look, the label, nothing clipped.
- [ ] **Step 5:** Run `./gradlew :app:testDebugUnitTest` (no update flag). Expected: pass. Commit `feat(app): the bodybuilding side's blue look, label and switch`.

### Task 7: Web look, label and switch

**Files:** `web/src/ui/tokens.css`, `theme.ts`, `tokens.test.ts`; the app root that sets the attribute; the header component(s) of the six top-level screens; `web/src/screens/settings/SettingsScreen.tsx` and its test; `web/e2e/` screenshot specs; new PNGs in `docs/design/web/screens/`.

**Interfaces — Consumes:** Task 5's settings. **Produces:** `data-mode="bodybuilding"` on `<html>` while the mode is on (absent otherwise), with the palette as variable overrides for dark and for the light media query; the `theme-color` meta follows the active background; `<ModeLabel />` (text `BODYBUILDING`, hidden from the tab order, read once by a screen reader as "Bodybuilding mode"); the same Settings group, switch name `Bodybuilding mode`, and supporting line as Task 6.

- [ ] **Step 1:** Write the failing tests: `tokens.test.ts` contrast checks over both new palettes and exact hex pins for them; `noVolt` still passes; Settings renders the group first and toggling writes the setting; with the mode on `document.documentElement.dataset.mode === 'bodybuilding'` and each top-level screen shows the label; off, neither.
- [ ] **Step 2:** Run `cd web && npx vitest run src/ui src/screens/settings`. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Add e2e screenshots `settings-bodybuilding-{dark,light}` and `today-bodybuilding-{dark,light}`. Run `UPDATE_SCREENSHOTS=1 npm run e2e -- --trace off`, then `git status`: only the Settings screenshots and the new PNGs may differ (restore any PNG that differs only by anti-aliasing noise, after a pixel compare). Read the new PNGs.
- [ ] **Step 5:** Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: pass. Commit `feat(web): the bodybuilding side's blue look, label and switch`.

### Task 8: Android screens on the bodybuilding side

**Files:** `app/.../ui/plan/` (PlanScreen, PlanViewModel, the entry editor), `ui/today/` (TodayViewModel, ExerciseCard, the rest-timer host), `ui/coach/` (CoachViewModel, AdviceCard, CoachScreen), `ui/progress/` (ProgressViewModel, ProgressScreen), their tests, `ScreenshotTest.kt`, a cross-restore test reading `web/src/backup/fixtures/backup-v3-web.json`.

**Interfaces — Consumes:** Tasks 3, 4, 6. Call `Coaching.targetsFor` / `Coaching.evaluate` with the mode wherever `Coach` is called today (`TodayViewModel.kt:193`, the coach view model).

Behaviour (spec §7.3 to §7.7, §5.1, §5.3):
- **Plan:** the active side's plan and training days. On the bodybuilding side the entry editor has two reps steppers (low, high; high never below low), new entries default to 3 sets, 8 and 12, RPE 8; a row reads `3 × 8–12 @ 8` via `repsText`. An empty bodybuilding plan shows one line inviting the lifter to add exercises to each day.
- **Today:** follows today's session's mode when a session exists for the date, else the active mode. Range prescription; coach card headline `26 kg × 8–12 @ 8` with the reason minus the suggestion sentence (extend `SUGGESTION_SENTENCE` so it still strips the range form; the powerlifting strings must strip exactly as before); **Apply** fills the weight and `repsToBeat` for the current set. On a bodybuilding session the Top set chip and the "Where did it stall?" question are not shown; Missed still works. Every exercise uses the accessory rest. When the active side differs from today's session's side, one line under the header: `Today's session is on the powerlifting side. Bodybuilding starts with your next session.` (or the reverse).
- **Coach:** the active side's feed (`forSide`). The welcome-back card when `Coaching.welcomeBack(leftOn of the active side, today, plan not empty)`: title `Welcome back to the better side.`, buttons `Resume my plan` and `My goals have changed`; either clears that side's left-on date; the second opens Plan. The same card shows on Today above the first exercise.
- **Progress:** on the bodybuilding side, `BodybuildingProgress.rows` as a list: exercise name, its best sets as `26 × 11`, an up arrow when `goingUp == true`, a level mark when false, nothing when null; a row opens the existing exercise history. The powerlifting charts, records and total are not shown on this side.

- [ ] **Step 1:** Write the failing Robolectric tests for each bullet, including: switching swaps the plan and the days; a session logged on the bodybuilding side appears in that exercise's history when viewed from the powerlifting side; switching on a day already trained keeps Today on that session, shows the line, and the log still has one session for the date; an exercise with no history shows no coach card and can be logged; welcome back after 31 days shows once and not on first entry; the web fixture restores with both sides' plans.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Add screenshots with the mode on, dark and light: `plan-bodybuilding`, `coach-bodybuilding` (with the welcome-back card), `progress-bodybuilding`, and refresh `today-bodybuilding` now that it has real content. Run with `-Pscreenshots.update=true`; `git status` must show no change to any screenshot without `bodybuilding` in its name. Read the new PNGs.
- [ ] **Step 5:** Run `./gradlew :app:testDebugUnitTest`. Expected: pass. Commit `feat(app): plan, today, coach and progress on the bodybuilding side`.

### Task 9: Web screens on the bodybuilding side

**Files:** `web/src/screens/{plan,today,coach,progress}/`, `web/src/state/` (`usePlan.ts`, `useToday.ts`, `useCoach.ts`, `useProgress.ts`, `useRest.ts`), their tests, `web/e2e/` (a new `bodybuilding.spec.ts` and screenshot cases), a cross-restore test reading `app/src/test/resources/backup-v3-android.json`.

**Interfaces — Consumes:** Tasks 3, 5, 7. Same behaviour, wording and screenshot names as Task 8, in the web app; call `grit`'s `Coaching` functions wherever `targetsFor` / `evaluate` are called today (`state/useToday.ts:253`, `useCoach.ts`).

- [ ] **Step 1:** Write the failing vitest and Playwright tests, the twins of Task 8's, plus: the picker and Exercises tab @speed checks pass with the mode on (run the speed project once with the mode seeded on).
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Screenshots as Task 8 Step 4, with `UPDATE_SCREENSHOTS=1`; no existing screenshot without `bodybuilding` in its name may change.
- [ ] **Step 5:** Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: pass. Commit `feat(web): plan, today, coach and progress on the bodybuilding side`.

### Task 10: Version 1.2, notes, full run

**Files:** `app/build.gradle.kts` (`versionName = "1.2"`, `versionCode = 3`), `web/package.json` and the two version fields of `package-lock.json` (`1.2.0`), the eight Settings screenshots that show the version line plus the bodybuilding Settings ones, `docs/next-update.md`, `README.md` (manual phone checklist: one line to try the switch, log a set on each side, and export then restore a backup on the other platform).

- [ ] **Step 1:** Bump both versions; run the versions-agree test (`cd web && npx vitest run src/screens/settings`). Expected: pass.
- [ ] **Step 2:** Regenerate the Settings screenshots on both apps; `git status` shows only Settings PNGs changed.
- [ ] **Step 3:** In `docs/next-update.md` replace the "Bodybuilding mode" idea section with: stage 1 done (what the lifter gets), stages 2 to 4 to come, pointing at the spec.
- [ ] **Step 4:** Full run, once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest`, then `./gradlew --stop`, then `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: all pass.
- [ ] **Step 5:** Commit `chore: version 1.2; bodybuilding mode stage 1 notes`.

# Bodybuilding Mode, Stage 2 Implementation Plan: the plan builder

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the bodybuilding side, the app builds the lifter's weekly plan from three answers (how many days, which exercises they do now, which body parts they most want to improve), shows a draft they can adjust, and saves it.

**Architecture:** The rules live in the shared Kotlin domain as a pure function, `PlanBuilder.build(answers)`, that returns a draft: the same answers always give the same draft, offline. Each app adds a four-step screen (days, exercises, body parts, draft) and one storage call that replaces the bodybuilding side's plan in a single transaction.

**Tech Stack:** Kotlin Multiplatform (`domain/`, JS bridge), Android Compose + Room, React + Dexie, Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-13-grit-bodybuilding-mode-design.md` §8 "Stage 2" (the owner's decisions), with §3 (goals, anti-goals) and §9. Stage 1 is merged (`docs/superpowers/plans/2026-11-13-grit-bodybuilding-mode-stage-1.md`).

## Global Constraints

- Everything in stage 1's Global Constraints still holds: the powerlifting side does not change; one training log; the coach and the builder never change a plan without a tap; plain words; no model names in code or commit bodies other than the harness's attribution trailer; new commits only, never amend, never push, never touch `main`; stage files by path; never commit `.claude/`, `CLAUDE.md` or `.superpowers/`; never `playwright install`; e2e with `--trace off`; after a domain change run `./gradlew :domain:syncWebDomain :domain:checkWebDomain` and commit the bundle.
- The builder asks exactly the owner's questions: days (3 or 4) and which weekdays; which exercises the lifter does now, or, if none, which they would like to do; which body parts they most want to improve. It does not ask about equipment.
- The owner's numbers: 8–12 reps at RPE 8, 3 sets. Sourced numbers (in `CoachSources.kt`, checked on 2026-11-13 with a web search restricted to the allowed sources): about 10 sets per muscle group a week for muscle growth, and every major muscle group trained at least twice a week (ACSM 2026 position stand, as announced at https://acsm.org/resistance-training-guidelines-update-2026/ and in its infographic https://acsm.org/effective-resistance-training-program-infographic/); compound lifts before accessory work, and the exercises that matter most to the goal first (ExRx's summary of the same stand, https://exrx.net/WeightTraining/Guidelines). The builder for Task 1 re-checks these with WebSearch (`allowed_domains` only; never fetch the sites) and records what each page says.
- The designer's own numbers, flagged for the owner, not sourced: at most 6 exercises a session; a priority body part's main exercises get 4 sets where the rest get 3; at most 3 priority body parts.
- The saved plan is ordinary bodybuilding plan items (stage 1's storage). No new stored settings, so the backup format does not change.
- App version 1.3 on both apps (last task).

## Review Focus

1. **The lifter picks nothing at all:** the builder still returns a complete plan from the catalogue; nothing crashes; every session has exercises.
2. **The lifter picks more than fits** (say 40 exercises on 3 days): every session stays within its size, each left-out exercise is named with a reason, and none of the picks is silently dropped.
3. **A pick that is the lifter's own exercise** (not in the catalogue, perhaps with no muscles recorded): it is kept and placed, and counts for no muscle it does not list.
4. **Running the builder over an existing bodybuilding plan:** the old plan is replaced only when the lifter accepts the draft, in one transaction; backing out leaves it untouched; the powerlifting plan is never touched.
5. **Every priority body part chosen when there is no room** (3 priorities, 3 days, many picks): priorities still come first and get their extra sets; the draft says what fell short.

---

## The rules (Task 1 implements exactly these)

**Words.** A *group* is one of the 18 `MuscleGroup`s. An exercise *works* a group when any of its listed muscles maps to it (`MuscleGroups.works`). Its *main group* is the group of its first-listed muscle. A *set counts* for every group the exercise works.

**Sessions.**
- 3 days: three full-body sessions, on the lifter's weekdays in week order. Every group is allowed in every session.
- 4 days: upper, lower, upper, lower, in week order. Upper allows CHEST, LATS, UPPER_BACK, TRAPS, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, BICEPS, TRICEPS, FOREARMS. Lower allows QUADS, HAMSTRINGS, GLUTES, ADDUCTORS, CALVES, LOWER_BACK, ABS, OBLIQUES. An exercise belongs to the kind of session that allows its main group.
- A session holds at most `MAX_EXERCISES = 6` exercises, no exercise twice.

**The groups the builder aims at** (`TARGET_GROUPS`, in this order): CHEST, LATS, QUADS, HAMSTRINGS, GLUTES, SIDE_DELTS, UPPER_BACK, BICEPS, TRICEPS, CALVES, ABS, REAR_DELTS. The other six are worked by the same exercises and are not aimed at on their own. The weekly aim is `WEEKLY_SETS = 10` counted sets for each, and each trained on at least two days.

**Priority groups** (at most 3, from the lifter): moved to the front of the order above, in the order the lifter chose them.

**Building, in this order:**
1. **Place the picks.** Take the lifter's picks in this order: those whose main group is a priority group, then the rest, each part in `TARGET_GROUPS` order of main group, then by name. Give each pick, in turn, to the allowed session with the fewest exercises so far (earliest on a tie) that does not already hold an exercise with the same main group; if every allowed session holds one, to the allowed session with the fewest exercises. A pick that cannot go anywhere because every allowed session is full is left out, with the reason "No room: every session it fits is full."
2. **Repeat for frequency.** For each group in aim order that is trained on fewer than two days: take the exercise already in the plan with that main group (the first placed) and add it to the allowed session with the fewest exercises that does not hold it, if one has room. The same exercise twice a week is intended: it is how the reps-then-weight progression moves fastest.
3. **Fill what is short.** While some session has room: take the group, in aim order, with the largest shortfall against `WEEKLY_SETS` (counted sets so far); stop when no group is short. Choose its exercise: one already in the plan with that main group if a session with room does not hold it; otherwise the first catalogue default for the group (below) not already in the plan. Add it to the allowed session with the fewest exercises and room. If no allowed session has room, mark the group as short and move on.
4. **Order each session:** exercises whose main group is a priority group first, then the rest; inside each part, compound before accessory (`LibraryExercise.kind`; a lifter's own exercise counts as its stored category says), then aim order of main group, then name.
5. **Prescribe:** every exercise 8–12 at RPE 8; 4 sets when its main group is a priority group, otherwise 3.

**Catalogue defaults.** For each of the 12 target groups, the catalogue exercises whose main group it is, ordered: equipment the lifter's picks use first (by how many picks use it, most first), then this fixed order: BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, SMITH, KETTLEBELL, BAND, SPECIALTY_BAR; compound before accessory; then catalogue order. `variant` (0, 1, 2, ...) rotates each group's list by that many places: it is what "another draft" changes. Picks are never changed by `variant`.

**The draft says what it did:** per session its exercises and prescriptions; for each of the 12 target groups its counted weekly sets and the number of days it is trained; the picks left out with reasons; the target groups still short.

**Swapping:** `alternatives(name, draft)` is the catalogue exercises with the same main group that are not in the draft, ordered as the defaults are. Replacing one exercise keeps its place, sets and range.

---

### Task 1: the builder in the shared domain

**Files:**
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/plan/PlanBuilder.kt`, `PlanBuilderModels.kt`
- Modify: `domain/.../coach/CoachSources.kt` (the citations), `domain/.../library/MuscleGroups.kt` (only if `mainGroup` belongs there), `domain/src/jsMain/.../web/GritDomain.kt`, `JsTypes.kt`; regenerate the bundle
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/plan/PlanBuilderTest.kt`

**Interfaces — Produces:**
```kotlin
data class BuilderPick(val name: String, val muscles: List<String>, val compound: Boolean, val equipment: List<Equipment>, val inCatalogue: Boolean)
data class BuilderAnswers(val days: List<DayOfWeek>, val picks: List<BuilderPick>, val priorities: List<MuscleGroup>, val variant: Int = 0)
data class DraftExercise(val name: String, val inCatalogue: Boolean, val sets: Int, val repsLow: Int, val repsHigh: Int, val rpe: Double, val mainGroup: MuscleGroup?)
data class DraftSession(val day: DayOfWeek, val title: String /* "Full body", "Upper", "Lower" */, val exercises: List<DraftExercise>)
data class GroupVolume(val group: MuscleGroup, val weeklySets: Int, val days: Int, val short: Boolean)
data class LeftOut(val name: String, val reason: String)
data class PlanDraft(val sessions: List<DraftSession>, val volume: List<GroupVolume>, val leftOut: List<LeftOut>)
object PlanBuilder {
    const val MAX_EXERCISES = 6; const val WEEKLY_SETS = 10; const val MAX_PRIORITIES = 3
    const val SETS = 3; const val PRIORITY_SETS = 4; const val REPS_LOW = 8; const val REPS_HIGH = 12; const val RPE = 8.0
    val TARGET_GROUPS: List<MuscleGroup>
    fun build(answers: BuilderAnswers): PlanDraft                     // throws IllegalArgumentException unless days.size is 3 or 4, distinct
    fun alternatives(name: String, draft: PlanDraft, picks: List<BuilderPick>): List<LibraryExercise>
    fun swap(draft: PlanDraft, day: DayOfWeek, from: String, to: String): PlanDraft   // volume recomputed
    fun mainGroup(muscles: List<String>): MuscleGroup?
    /** Catalogue and own exercises to pre-tick: in either side's plan, or logged in the 28 days before [today]. */
    fun suggestedPicks(history: TrainingHistory, plans: List<PlanItem>, today: LocalDate): List<Long>
}
```
JS bridge: `planBuilderBuild`, `planBuilderAlternatives`, `planBuilderSwap`, `planBuilderSuggestedPicks`, with JS twins of the types, following `bodybuildingProgress`.

- [ ] **Step 1:** Re-check the three sources by WebSearch and write their citations into `CoachSources.kt` (what each page says, the date, and that the session size, the 4 sets and the three priorities are the designer's numbers, not sourced). If a page does not say what this plan claims, stop and report.
- [ ] **Step 2:** Write the failing `PlanBuilderTest`. Rules as properties over many answers (3 and 4 days; 0, 5, 12 and 40 picks drawn from the catalogue with a fixed seed; 0 to 3 priorities; variants 0 to 3): the same answers give an equal draft; every session has 1 to 6 exercises and no repeat; every exercise is a pick or in the catalogue; every pick is in the draft or in `leftOut`, never both, never neither; on 4 days every exercise sits in a session that allows its main group; compounds come before accessories within the priority part and within the rest; every prescription is 8–12 at RPE 8 with 4 sets exactly when its main group is a priority; `volume` matches a recount from the sessions; `short` is true exactly when a target group has fewer than 10 counted sets. Fixed cases: no picks, 3 days, no priorities gives three sessions of 6 with every target group trained on at least two days and none short (if the rules cannot reach that, report the actual numbers instead of bending the test); no picks, 4 days likewise; priorities [SIDE_DELTS] puts a side-delt exercise first in its sessions with 4 sets and gives SIDE_DELTS more weekly sets than without the priority; 40 picks on 3 days fills 18 places and names 22 left out with the room reason; a pick that is the lifter's own with no muscles is placed, last in its session, counting for nothing; variant 1 differs from variant 0 only in catalogue defaults; `alternatives` never offers an exercise in the draft and all share the main group; `swap` keeps the place and sets; `build` rejects 2 and 5 days. `suggestedPicks`: plan exercises from both sides plus exercises logged within 28 days, each once, not ones logged 29 days ago, not archived ones.
- [ ] **Step 3:** Run `./gradlew :domain:jvmTest --tests '*PlanBuilderTest*'`. Expected: does not compile.
- [ ] **Step 4:** Implement the rules above exactly, and the JS bridge.
- [ ] **Step 5:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain` and `cd web && npm test && npm run typecheck`. Expected: pass.
- [ ] **Step 6:** Commit `feat(domain): the bodybuilding plan builder`.

### Task 2: Android, the builder screen and saving

**Files:** new `app/src/main/kotlin/com/powertracker/ui/plan/builder/` (`PlanBuilderScreen.kt`, `PlanBuilderViewModel.kt`); `ui/plan/PlanScreen.kt` and `PlanViewModel.kt` (entry points); `ui/coach/` and `ui/today/` (where "My goals have changed" leads); `ui/library/ExerciseBrowser.kt` (a multi-select mode for the existing browser; the muscle ticking sheet `MusclesContent` reused with its own heading); `data/TrainingRepository.kt` and `data/db/Daos.kt` (replace one side's plan); navigation in `ui/AppShell.kt`. Tests in `app/src/test/.../ui/plan/builder/` and beside the repository tests. New screenshots `plan-builder-*-bodybuilding-{dark,light}`.

**Interfaces — Consumes:** Task 1. **Produces:** `TrainingRepository.replacePlan(mode: TrainingMode, items: List<Pair<PlanItem, String?>>)` (each item with its catalogue name or null for an own exercise's id), one transaction: delete that side's items, resolve catalogue names with `CatalogueResolver`, insert in order. `PlanDao.clearSide(mode)`.

The screen, four steps with Back and Next, the bodybuilding look, reachable only on the bodybuilding side:
1. **Days.** "How many days a week?" with two choices, `3 days` and `4 days`, then the weekday chips; Next is enabled when exactly that many are chosen. It starts from the bodybuilding training days when there are 3 or 4 of them.
2. **Exercises.** Heading "Which exercises do you do now?" over the existing exercise browser in a multi-select mode (rows tick; search, muscle and equipment filters as today), pre-ticked from `suggestedPicks`. A line shows the count, `12 chosen`. With nothing ticked the heading reads "Which exercises would you like to do?" and Next reads `Choose for me` (the builder then uses the catalogue).
3. **Body parts.** "Which body parts do you most want to improve?" with the 18 groups to tick, at most 3 (a fourth tick is refused with one line saying why); `Skip` is allowed.
4. **Draft.** One card per session: its day and title, each exercise with `3 × 8–12 @ 8`; a `Swap` action per exercise opening `alternatives`; under the sessions, "Weekly sets" listing the 12 target groups with their counted sets and a mark on those short of 10; any left-out picks with their reasons; buttons `Another draft` (next variant) and `Use this plan`.
- `Use this plan`: when a bodybuilding plan already exists, confirm first ("This replaces your bodybuilding plan. Your log is kept."); then `replacePlan(BODYBUILDING, ...)`, set the bodybuilding training days to the chosen weekdays, and return to Plan.
- Leaving at any step before `Use this plan` changes nothing.

Entry points: the empty bodybuilding Plan's line gains a button `Build my plan`; the bodybuilding Plan screen's menu gains `Rebuild my plan`; on the bodybuilding side "My goals have changed" opens the builder (on the powerlifting side it still opens Plan).

- [ ] **Step 1:** Write the failing tests: `replacePlan` replaces only that side's rows, in order, in one transaction (a failure part-way leaves the old plan), and resolves catalogue names; each step's enabling rules and wording above; pre-ticks come from `suggestedPicks`; nothing ticked gives `Choose for me` and a full plan; the fourth priority is refused; the draft shows sessions, weekly sets, short marks and left-out picks; `Swap` replaces in place; `Another draft` changes only defaults; `Use this plan` with an existing plan confirms, then saves and sets the days; backing out at each step leaves the stored plan and days unchanged; the powerlifting plan is untouched throughout; the three entry points; the builder is not reachable on the powerlifting side.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Screenshots, dark and light, of the four steps (`plan-builder-days-bodybuilding`, `-exercises-`, `-body-parts-`, `-draft-`). With `-Pscreenshots.update=true`, `git status` shows only these new PNGs and `plan-bodybuilding-*` if the empty state is pictured. Read them.
- [ ] **Step 5:** Run `./gradlew :app:testDebugUnitTest`. Expected: pass. Commit `feat(app): the bodybuilding plan builder`.

### Task 3: web, the builder screen and saving

**Files:** new `web/src/screens/plan/builder/` (`PlanBuilderScreen.tsx`, its css and tests) and `web/src/state/usePlanBuilder.ts`; `web/src/screens/plan/PlanScreen.tsx`, `web/src/state/usePlan.ts`; `web/src/screens/coach/WelcomeBackCard.tsx` and its callers; `web/src/screens/library/ExerciseBrowser.tsx` (multi-select mode; `MusclesSheet` exported and reused); `web/src/db/catalogue.ts` (replace one side's plan); `web/src/router.tsx` and `App.tsx` (route `#/plan/build`); `web/e2e/bodybuilding.spec.ts` and the screenshot cases.

**Interfaces — Consumes:** Task 1. **Produces:** `replacePlan(db, mode, items: { item, catalogueName: string | null }[]): Promise<void>` in one Dexie transaction. Same four steps, wording, enabling rules, entry points and screenshot names as Task 2. If Task 2's report is there when wording is not fixed by this plan, match it; otherwise use this plan's and list what to compare.

- [ ] **Step 1:** Write the failing vitest and Playwright tests, the twins of Task 2's, plus one end-to-end story: turn the mode on, build a 4-day plan from three ticked exercises and one priority, swap one exercise, use the plan, and see it on Plan and on Today for the first training day.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Screenshots as Task 2 Step 4, with `UPDATE_SCREENSHOTS=1`; no existing screenshot other than those named may change.
- [ ] **Step 5:** Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: pass (a @speed miss: re-run the speed project once, alone). Commit `feat(web): the bodybuilding plan builder`.

### Task 4: version 1.3, notes, full run

**Files:** `app/build.gradle.kts` (`versionName = "1.3"`, `versionCode = 4`), `web/package.json` and the two lock-file fields (`1.3.0`), the Settings screenshots on both apps, `docs/next-update.md`, `README.md` (manual phone checklist: one line to build a plan and train its first day).

- [ ] **Step 1:** Bump both versions; the versions-agree test passes.
- [ ] **Step 2:** Regenerate the Settings screenshots on both apps; only Settings PNGs change.
- [ ] **Step 3:** `docs/next-update.md`: stage 2 done (what the lifter gets; the sourced numbers and the three designer's numbers), stages 3 and 4 to come.
- [ ] **Step 4:** Full run, once: `./gradlew :domain:allTests --rerun-tasks`, `./gradlew :domain:checkWebDomain :app:testDebugUnitTest`, `./gradlew --stop`, then `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`.
- [ ] **Step 5:** Commit `chore: version 1.3; bodybuilding plan builder notes`.

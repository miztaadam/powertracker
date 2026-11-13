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
- The designer's own numbers, flagged for the owner, not sourced: the session templates and their sizes (8 exercises on a full-body day; 8 on an upper day and 6 on a lower day), plus at most one held-over pick a session; 4 sets for a session's first exercise and for a priority body part's exercises, 3 for the rest; at most 3 priority body parts, chosen from the 12 target groups; the hand-picked default exercises.
- The saved plan is ordinary bodybuilding plan items (stage 1's storage). No new stored settings, so the backup format does not change.
- App version 1.3 on both apps (last task).

## Review Focus

1. **The lifter picks nothing at all:** the builder still returns a complete plan from the catalogue; nothing crashes; every session has exercises.
2. **The lifter picks more than fits** (say 40 exercises on 3 days): every session stays within its size, each left-out exercise is named with a reason, and none of the picks is silently dropped.
3. **A pick that is the lifter's own exercise** (not in the catalogue, perhaps with no muscles recorded): it is kept and placed where a slot or a held-over place exists, and counts for no muscle it does not list.
4. **Running the builder over an existing bodybuilding plan:** the old plan is replaced only when the lifter accepts the draft, in one transaction; backing out leaves it untouched; the powerlifting plan is never touched.
5. **Every priority body part chosen when there is no room** (3 priorities, 3 days, many picks): priorities still come first and get their extra sets; the draft says what fell short.

---

## The rules (Task 1 implements exactly these)

*Revised after the first build: placing exercises one muscle at a time gave odd weeks (bench three times, powerlifting variations as defaults) and could not reach its own aim. The builder now works the way programmes are written: each session is a list of slots, and exercises fill the slots.*

**Words.** A *group* is one of the 18 `MuscleGroup`s. An exercise *works* a group when any of its listed muscles maps to it (`MuscleGroups.works`). Its *main group* is the group of its first-listed muscle. A *set counts* for every group the exercise works. The 12 *target groups*, in this order: CHEST, LATS, QUADS, HAMSTRINGS, GLUTES, SIDE_DELTS, UPPER_BACK, BICEPS, TRICEPS, CALVES, ABS, REAR_DELTS.

**Slot kinds.** Each kind accepts picks by main group and has a list of catalogue defaults, by exact name, in order:

| Kind | Accepts main group | Defaults |
|---|---|---|
| CHEST_PRESS | CHEST | DB press, Machine chest press, Bench, Smith bench press |
| CHEST_INCLINE | CHEST | Incline DB press, Incline bench press, Incline machine press, Smith incline press |
| CHEST_FLY | CHEST | Cable crossover, Pec deck, DB fly, Seated cable fly |
| PULL_DOWN | LATS | Lat pulldown, Pull-up, Machine pulldown, Chin-up, Close-grip pulldown |
| ROW | UPPER_BACK, LATS | Seated cable row, One-arm DB row, Machine row, Chest-supported row, Row, T-bar row |
| QUAD_COMPOUND | QUADS, ADDUCTORS | Leg press, Hack squat, Squat, Goblet squat, Smith squat |
| QUAD_ISOLATION | QUADS, ADDUCTORS | Leg extension, Bulgarian split squat, Split squat, Lunge |
| HINGE | HAMSTRINGS, GLUTES, LOWER_BACK | RDL, DB RDL, Hip thrust, Stiff-leg deadlift, Machine hip thrust |
| LEG_CURL | HAMSTRINGS, GLUTES | Seated leg curl, Lying leg curl, Standing leg curl, Cable leg curl |
| SHOULDER_PRESS | FRONT_DELTS | DB shoulder press, Machine shoulder press, Seated barbell press, Arnold press |
| SIDE_DELT | SIDE_DELTS | Lateral raise, Cable lateral raise, Machine lateral raise, Seated lateral raise |
| REAR_DELT | REAR_DELTS, TRAPS | Face pull, Rear delt raise, Machine reverse fly, Cable reverse fly |
| BICEPS | BICEPS, FOREARMS | DB curl, Cable curl, EZ-bar curl, Incline DB curl, Machine curl |
| TRICEPS | TRICEPS | Rope pushdown, Triceps pushdown, DB overhead triceps extension, Skull crusher |
| CALVES | CALVES | Standing calf raise, Seated calf raise, Leg press calf raise, Dumbbell calf raise |
| ABS | ABS, OBLIQUES | Cable crunch, Machine crunch, Reverse crunch, Ab wheel rollout |

A test proves every default name is in the catalogue and has a main group its kind accepts. A kind's defaults are re-ordered by the equipment the lifter's picks use (the primary equipment of each default; most-used first; otherwise the order above), then rotated by `variant` places: that is what "another draft" changes. Picks are never changed by `variant`.

**Session templates** (slots in training order):
- **3 days**, three full-body sessions on the lifter's weekdays in week order, titled "Full body":
  - 1st: QUAD_COMPOUND, CHEST_PRESS, PULL_DOWN, LEG_CURL, SIDE_DELT, BICEPS, TRICEPS, ABS
  - 2nd: HINGE, CHEST_INCLINE, ROW, QUAD_ISOLATION, SHOULDER_PRESS, REAR_DELT, CALVES, BICEPS
  - 3rd: QUAD_COMPOUND, CHEST_FLY, PULL_DOWN, HINGE, SIDE_DELT, TRICEPS, CALVES, ABS
- **4 days**, in week order "Upper", "Lower", "Upper", "Lower":
  - 1st upper: CHEST_PRESS, ROW, SHOULDER_PRESS, PULL_DOWN, SIDE_DELT, REAR_DELT, BICEPS, TRICEPS
  - 1st lower: QUAD_COMPOUND, HINGE, QUAD_ISOLATION, LEG_CURL, CALVES, ABS
  - 2nd upper: CHEST_INCLINE, PULL_DOWN, CHEST_FLY, ROW, SIDE_DELT, REAR_DELT, TRICEPS, BICEPS
  - 2nd lower: HINGE, QUAD_COMPOUND, LEG_CURL, QUAD_ISOLATION, CALVES, ABS

**Building, in this order:**
1. **Priorities add a slot.** Priorities are at most 3, each one of the 12 target groups. Each adds one slot of its kind (CHEST: CHEST_FLY; LATS: PULL_DOWN; UPPER_BACK: ROW; QUADS: QUAD_ISOLATION; HAMSTRINGS: LEG_CURL; GLUTES: HINGE; SIDE_DELTS: SIDE_DELT; REAR_DELTS: REAR_DELT; BICEPS: BICEPS; TRICEPS: TRICEPS; CALVES: CALVES; ABS: ABS) to the end of the session that has the fewest slots of that kind (on 4 days only upper sessions for upper kinds and lower for lower; earliest on a tie).
2. **Place the picks once.** Take the lifter's picks in this order: those whose main group is a priority first, then the rest, each part by target-group order of main group (other groups after, in enum order; no main group last), then by name. Give each pick the first empty slot that accepts its main group, looking at sessions in order of fewest picks placed so far (earliest on a tie) and at slots in template order. A pick with no empty slot that accepts it is held over.
3. **Place the picks a second time.** In the same order, each placed pick takes one more empty slot that accepts its main group in a session that does not already hold it, if there is one. A pick appears at most twice in the week. The same exercise twice a week is intended: it is how reps-then-weight moves fastest.
4. **Held-over picks.** Each goes, in order, to the end of the session with the fewest exercises that may take it (on 4 days: upper for a main group an upper kind accepts, lower for a lower one; either for a pick with no main group, as on 3 days) and that has not yet taken a held-over pick. When every such session has taken one, the pick is left out with the reason "No room: every session it fits is full."
5. **Fill the empty slots with defaults.** Each empty slot gets the first default of its kind that the session does not already hold. The same kind elsewhere in the week gets the same exercise unless that session already holds it.
6. **Order each session:** exercises whose main group is a priority first (in their slot order), then the rest in slot order, held-over picks last.
7. **Prescribe:** every exercise 8–12 at RPE 8. Sets: 4 for an exercise whose main group is a priority, 4 for the exercise in the session's first template slot, 3 for the rest.

**The draft says what it did:** per session its exercises and prescriptions; for each of the 12 target groups its counted weekly sets, the number of days it is trained, and whether it is under 10 a week; the picks left out, with reasons.

**Swapping:** `alternatives(name, draft, picks)` is, for a slot exercise, the catalogue exercises its kind accepts by main group that are not in that session: the kind's defaults first, then the rest in catalogue order. For a held-over pick it is the catalogue exercises with the same main group. Replacing one exercise keeps its place and sets.

**What the default weeks should look like** (no picks, no priorities, variant 0; Task 1 reports the real tables): each big group (chest, lats, quads, hamstrings, glutes, upper back, biceps, triceps) at or above 10 counted sets, and the small ones (side delts, rear delts, calves, abs) trained twice at about 6. A priority lifts its group to 10 or more. If the real tables differ from this, report them; do not bend the rules.

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
    const val WEEKLY_SETS = 10; const val MAX_PRIORITIES = 3; const val HELD_OVER_PER_SESSION = 1
    const val SETS = 3; const val LEAD_SETS = 4; const val PRIORITY_SETS = 4; const val REPS_LOW = 8; const val REPS_HIGH = 12; const val RPE = 8.0
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
- [ ] **Step 2:** Write the failing `PlanBuilderTest`. Rules as properties over many answers (3 and 4 days; 0, 5, 12 and 40 picks drawn from the catalogue with a fixed seed, plus own exercises with and without muscles; 0 to 3 priorities; variants 0 to 3): the same answers give an equal draft; no session holds an exercise twice; a session is never longer than its template plus its priority slots plus one held-over pick; every exercise is a pick or a catalogue default; every pick is in the draft or in `leftOut`, never both, never neither, and never more than twice in the week; on 4 days no upper session holds an exercise whose main group only a lower kind accepts, and the reverse; every prescription is 8–12 at RPE 8, with 4 sets exactly for priority-group exercises and each session's first template slot; priority-group exercises come first in their session; `volume` matches a recount from the sessions; `short` is true exactly under 10. Fixed cases: every default name is in the catalogue under a main group its kind accepts; no picks on 3 days and on 4 days gives exactly the template's exercises (assert the full list of names for variant 0), with the weekly table as the rules give it; priorities [SIDE_DELTS] adds a side-delt exercise, puts side-delt exercises first with 4 sets, and gives SIDE_DELTS strictly more counted weekly sets than the same answers without the priority, and at least 10; a priority never lowers any group's counted sets; five picks land in slots that accept them and appear twice where a second slot exists; 40 picks on 3 days leaves out exactly the ones that found no slot and no held-over place, each with the room reason; an own exercise with no muscles is placed as a held-over pick, counting for nothing; variant 1 differs from variant 0 only in defaults; `alternatives` never offers an exercise already in that session and starts with the kind's defaults; `swap` keeps the place and sets; `build` rejects 2 and 5 days, a repeated weekday, more than 3 priorities, and a priority outside the 12 target groups. `suggestedPicks`: plan exercises from both sides plus exercises logged within 28 days, each once, not ones logged 29 days ago, not archived ones.
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
3. **Body parts.** "Which body parts do you most want to improve?" with the 12 target groups to tick, at most 3 (a fourth tick is refused with one line saying why); `Skip` is allowed.
4. **Draft.** One card per session: its day and title, each exercise with `3 × 8–12 @ 8`; a `Swap` action per exercise opening `alternatives`; under the sessions, "Weekly sets" listing the 12 target groups with their counted sets, a quiet mark on those under 10, and one line: "10 or more a week is the guideline for growth. Make a body part a priority to give it more."; any left-out picks with their reasons; buttons `Another draft` (next variant) and `Use this plan`.
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

# Bodybuilding Mode, Stage 3 Implementation Plan: check-ins, the lagging-muscle coach, Progress

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the bodybuilding side, every four weeks (and at any time from Progress) the lifter marks which body parts are lagging and which are growing well. The coach then suggests more work for the lagging ones and a swap for any exercise stuck for three weeks; one tap applies a suggestion to the plan. Progress shows each body part's ratings, the sets actually done and whether its exercises are moving up.

**Architecture:** The rules live in the shared Kotlin domain as pure functions: `PhysiqueCoach` (more work, stuck exercises, applying either to a plan) and `MuscleProgress` (the Progress rows), reached through `Coaching`. Check-ins are a new stored record on both apps (Room 4, Dexie 4) and go in the backup (version 4). Suggestions are ordinary stored advice of two new types, shown in the Coach feed on the bodybuilding side only.

**Tech Stack:** Kotlin Multiplatform (`domain/`, JS bridge), Android Compose + Room, React + Dexie, Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-13-grit-bodybuilding-mode-design.md` §8 "Stage 3" (the owner's decisions), with §3 (goals, anti-goals) and §9. Stages 1 and 2 are merged (their plans are beside this one).

## Global Constraints

- Everything in stage 1's and stage 2's Global Constraints still holds: the powerlifting side does not change; one training log; the coach never changes a plan without a tap; plain words; no model names in code or commit bodies other than the harness's attribution trailer; new commits only, never amend, never push, never touch `main`; stage files by path; never commit `.claude/`, `CLAUDE.md` or `.superpowers/`; never `playwright install`; e2e with `--trace off`; after a domain change run `./gradlew :domain:syncWebDomain :domain:checkWebDomain` and commit the bundle.
- **The lifter judges, the coach suggests.** Every body part starts each check-in "on track"; the lifter marks the ones that stand out. A suggestion changes the plan only when the lifter taps it.
- **Sourced number** (re-checked by Task 1 with WebSearch, `allowed_domains` only, never fetching the sites, and recorded in `CoachSources.kt`): **at most 20 counted sets a week for one body part.** Baz-Valle et al. 2022, "A Systematic Review of The Effects of Different Resistance Training Volumes on Muscle Hypertrophy", J Hum Kinet 81:199–210, https://pmc.ncbi.nlm.nih.gov/articles/PMC8884877/ : in trained men training each muscle twice a week, the best range for growth was 12–20 weekly sets, with no added benefit above 20. Our count gives a set in full to every group an exercise works (stage 2's rule), so a body part reaches 20 counted sets sooner than it would by the study's count: the ceiling errs on the side of less.
- **The designer's numbers, flagged for the owner, not sourced:** a check-in falls due 28 days after the last one; one round of suggestions for each check-in; a round adds one set to at most two exercises for that body part, or else one new exercise of 3 sets; no exercise is taken above 4 sets; at most 3 body parts get more work from one check-in; no session grows above 10 exercises; an exercise is stuck when it has not moved for 21 days and was done at least twice in that time; a swap is not suggested again for the same exercise within 28 days; up to 3 alternatives are offered.
- **No overall weekly ceiling is sourced.** No allowed source gives a limit on a whole week's sets. In its place this plan limits how fast the week grows: 3 body parts per check-in and 10 exercises per session (both the designer's). This needs the owner's yes before building.
- Check-ins are stored on both apps and go in the backup, version 4. A version 3 file restores with no check-ins. Both apps read each other's version 4 files (shared fixtures).
- App version 1.4 on both apps (last task).

## Review Focus

1. **The ceiling holds whatever the lifter does.** Checking in every day with the same body parts lagging never takes any group past 20 counted sets a week, never takes an exercise past 4 sets and never makes a session longer than 10 exercises, including for the groups a change touches indirectly (a set of incline press also counts for front delts and triceps).
2. **Growing well is left alone:** no suggestion for it and no swap of its exercises.
3. **A suggestion applied after the plan changed** (the exercise was removed, its sets edited, the plan rebuilt) changes only what still matches and says so; it never adds a duplicate.
4. **The powerlifting side:** no check-in card, no new advice types in its feed, Progress unchanged, coach output identical (golden scenarios unedited).
5. **Backups:** a version 4 file from each app restores on the other with every check-in and every new advice; a version 3 file restores as before.

---

## The rules (Task 1 implements exactly these)

**Words.** As in stage 2: a *group* is one of the 18 `MuscleGroup`s; an exercise *works* a group when any of its listed muscles maps to it (`MuscleGroups.works`); its *main group* is its first-listed muscle's group (`PlanBuilder.mainGroup`); a *set counts* for every group the exercise works. *The plan* is the bodybuilding side's plan items. A group's *weekly sets* are the counted sets of the plan's items for it (each item's sets, for each group its exercise works, summed over the week). An exercise *in the plan* matches by id, or for a catalogue exercise by normalised name (`Library.normalise`). *Plan equipment* is the union of the equipment of the plan's catalogue exercises (found by name) plus BODYWEIGHT.

**Check-in.** A check-in is a date and two disjoint sets of groups, `lagging` and `growingWell`; every other group is on track. One check-in per date: saving another on the same date replaces it.

**Due.** A check-in is due on the bodybuilding side when the plan is not empty, the lifter has logged at least one bodybuilding session, and the later of (the latest check-in's date, the first bodybuilding session's date) is 28 or more days before today.

**More work (on saving a check-in).** Every OPEN MORE_WORK advice is set to DISMISSED. Then the lagging groups are taken in order of fewest weekly sets first (ties in `MuscleGroup` order); the first 3 that can be given more work each get one MORE_WORK advice; every other lagging group gets a note. For one lagging group G, in this order:
1. If G's weekly sets are already 20 or more: note `AT_CEILING`.
2. **More sets.** The *direct* exercises are the plan items whose exercise's main group is G and whose sets are under 4, fewest sets first, then by weekday and order. Take them one at a time, at most two: add one set to the item if, with every set already chosen in this round, no group the exercise works would pass 20. If at least one was chosen, that is G's suggestion.
3. **A new exercise.** Candidates: catalogue exercises of category ACCESSORY whose main group is G, not in the plan, every piece of equipment in plan equipment, and whose name contains none of "plank", "carry", "walk", "hold", "clamshell", "quadruped", "rotation" (held, carried or drill work, not done for 8–12 reps). Order: first those whose `PlanBuilder.kindOf` differs from every direct exercise's kind (they work G differently), then by position in their kind's `SlotKind.defaults` (listed ones first, in list order), then catalogue order. The day: among the plan's weekdays with fewer than 10 items, the one with the fewest counted sets of G among those that have some; if none has any, the one with the most items working a group in G's half (upper: CHEST, LATS, UPPER_BACK, TRAPS, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, BICEPS, TRICEPS, FOREARMS; lower: QUADS, HAMSTRINGS, GLUTES, ADDUCTORS, CALVES, LOWER_BACK; ABS and OBLIQUES belong to both), then the fewest items; ties to the earliest weekday. Prescription 3 × 8–12 at RPE 8, added at the end of that day. Take the first candidate whose 3 sets keep every group it works at 20 or under. No weekday with room: note `FULL`. No candidate: note `NO_EXERCISE`.
4. A lagging group past the first 3 that could be given more work: note `LATER`.

Growing-well and on-track groups get nothing.

**Applying more work** is a pure function of the advice and the current plan. A set addition applies only if the plan still has that item (weekday and exercise) at the advice's `fromSets`; it then gets `toSets`. A new exercise applies only if it is still not in the plan and its weekday still has fewer than 10 items. The result lists what applied and what did not, and the message says which: `Added a set to 2 exercises`, `Added Pec deck to Thursday`, or `Your plan has changed since; nothing was added` (the advice is then APPLIED all the same, so the card goes).

**Stuck (when the coach evaluates on the bodybuilding side, as it does today after a session).** For each exercise in the plan, in plan order: skip it when its main group is growing well at the latest check-in; skip every exercise while a DELOAD or RE_ENTRY advice, OPEN or APPLIED, was created in the 21 days ending today; skip it when a SWAP advice for it was created in the 28 days ending today, whatever its status, or one is OPEN. From its bodybuilding sessions with sets not missed: the *baseline* is its latest such session on or before today minus 21 days; the *window* is its such sessions after that date and on or before today. It is stuck when there is a baseline, the window has at least two sessions, and no window session beats the baseline. A session *beats* another when its heaviest weight is above the other's, or the same with more reps in total (the rule `BodybuildingProgress` uses for "going up"). A stuck exercise gets one SWAP advice when it has at least one alternative.

**Alternatives for a swap:** the same candidate rules as a new exercise for its main group (category ACCESSORY, not in the plan, plan equipment, not held or carried), ordered with the same `kindOf` first this time (it takes the same place), then the kind's defaults, then catalogue order; at most 3.

**Applying a swap** with the chosen alternative replaces the exercise in every plan item that holds it (the side's plan only), keeping weekday, order, sets, reps and RPE. If the alternative is in the plan by then, or the stuck exercise is not, nothing changes and the message says so. Otherwise `Swapped Leg extension for Sissy squat`.

**Progress rows.** One row per group, in `MuscleGroup` order, for every group that has weekly sets in the plan, or counted sets done in the 7 days ending today, or a rating other than on track at the latest check-in. Each row: the group's rating at each of its last 4 check-ins (oldest first, with dates), counted sets done in the 7 days ending today (bodybuilding sessions, sets not missed), its weekly sets in the plan, and of the plan's exercises whose main group is it, how many are going up and how many have enough sessions to tell (`BodybuildingProgress.rows`' `goingUp`, non-null).

---

### Task 1: the rules in the shared domain

**Files:**
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/model/CheckIn.kt`, `domain/.../coach/PhysiqueCoach.kt`, `domain/.../progress/MuscleProgress.kt`
- Modify: `model/Advice.kt` (two types, three payloads), `coach/Coaching.kt` (entry points), `coach/CoachSources.kt` (the citation), `domain/src/jsMain/.../web/GritDomain.kt`, `JsTypes.kt`; regenerate the bundle
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/coach/PhysiqueCoachTest.kt`, `progress/MuscleProgressTest.kt`, additions to `CoachingTest.kt`

**Interfaces — Produces:**
```kotlin
enum class MuscleRating { ON_TRACK, GROWING_WELL, LAGGING }
data class CheckIn(val date: LocalDate, val lagging: Set<MuscleGroup>, val growingWell: Set<MuscleGroup>, val id: Long = 0) {
    fun rating(group: MuscleGroup): MuscleRating   // init: require lagging and growingWell disjoint
}
enum class AdviceType { TARGET, STALL, DELOAD, WEAK_POINT, RE_ENTRY, MORE_WORK, SWAP }   // the two new ones last
@Serializable data class SetAddition(val weekday: DayOfWeek, val exerciseId: Long, val name: String, val fromSets: Int, val toSets: Int)
@Serializable data class NewExercise(val weekday: DayOfWeek, val name: String, val sets: Int, val repsLow: Int, val repsHigh: Int, val rpe: Double)
// AdvicePayload gains:
@Serializable data class MoreWork(val group: MuscleGroup, val addSets: List<SetAddition>, val newExercise: NewExercise?, val weeklyBefore: Int, val weeklyAfter: Int) : AdvicePayload
@Serializable data class Swap(val exerciseId: Long, val name: String, val group: MuscleGroup, val since: LocalDate, val alternatives: List<String>) : AdvicePayload
enum class NoteReason { AT_CEILING, NO_EXERCISE, FULL, LATER }
data class GroupNote(val group: MuscleGroup, val reason: NoteReason, val weeklySets: Int)
data class CheckInOutcome(val advice: List<Advice>, val notes: List<GroupNote>)
data class PlanChange(val updated: List<PlanItem>, val removed: List<PlanItem>, val added: List<Pair<PlanItem, String>> /* new item (exerciseId 0) with its catalogue name */, val message: String, val changed: Boolean)
object PhysiqueCoach {
    const val WEEKLY_CEILING = 20; const val MAX_SETS = 4; const val MAX_GROUPS = 3; const val MAX_EXERCISES = 10
    const val CHECK_IN_DAYS = 28; const val STUCK_DAYS = 21; const val SWAP_COOLDOWN_DAYS = 28; const val ALTERNATIVES = 3
    fun weeklySets(plan: List<PlanItem>, exercises: Map<Long, Exercise>, group: MuscleGroup): Int
    fun afterCheckIn(checkIn: CheckIn, history: TrainingHistory, plan: List<PlanItem>, today: LocalDate): CheckInOutcome
    fun stuck(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, priorAdvice: List<Advice>, today: LocalDate): List<Advice>
    fun applyMoreWork(payload: AdvicePayload.MoreWork, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange
    fun applySwap(payload: AdvicePayload.Swap, choice: String, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange   // old items in `removed`, their replacements in `added`
}
data class MuscleRow(val group: MuscleGroup, val ratings: List<Pair<LocalDate, MuscleRating>>, val setsDone: Int, val setsPlanned: Int, val exercisesUp: Int, val exercisesTracked: Int)
object MuscleProgress { fun rows(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, today: LocalDate): List<MuscleRow> }
// Coaching gains:
fun checkInDue(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, today: LocalDate): Boolean
fun afterCheckIn(checkIn: CheckIn, history: TrainingHistory, plan: List<PlanItem>, today: LocalDate): CheckInOutcome   // the side's plan only; advice stamped BODYBUILDING
// Coaching.evaluate gains `checkIns: List<CheckIn> = emptyList()`; on the bodybuilding side it adds PhysiqueCoach.stuck(...). The powerlifting branch ignores it.
```
An app carries out a `PlanChange` in one transaction: save `updated`, delete `removed`, and insert `added` after resolving each catalogue name with its existing catalogue resolver. JS bridge: `physiqueAfterCheckIn`, `physiqueStuck` (or through `coachingEvaluate`'s new parameter), `physiqueApplyMoreWork`, `physiqueApplySwap`, `coachingCheckInDue`, `muscleProgressRows`, `physiqueWeeklySets`, with JS twins of the types, following `bodybuildingProgress` and the plan builder's bridge. The new advice payloads serialise through the same serializer the apps already use for advice payloads, so both apps store and back them up identically.

- [ ] **Step 1:** Re-check the source by WebSearch (`allowed_domains` only) and write its citation into `CoachSources.kt`: what the page says, the date, that our counting reaches 20 sooner than the study's, and that the numbers listed under "the designer's numbers" above are not sourced. If the page does not say what this plan claims, stop and report.
- [ ] **Step 2:** Write the failing tests. Properties over many plans (the stage 2 builder's drafts for 3 and 4 days, variants 0–3, with and without priorities, saved as plan items; plus hand-made plans with an own exercise, a 5-set exercise and a full 10-exercise day) and every single lagging group and many random lagging/growing-well sets (fixed seed): no group's weekly sets pass 20 after applying any suggestion, or after applying 10 check-ins in a row with the same lagging groups; no exercise passes 4 sets except one that was already above; no weekday passes 10 items; at most 3 MORE_WORK advice per check-in and every other lagging group has exactly one note; growing-well and on-track groups get no advice and no note; a suggestion's `weeklyAfter` equals a recount after applying it; the same inputs give equal outputs; a round adds sets or a new exercise, never both. Fixed cases: a lagging group with planned direct exercises under 4 sets gets one more set on at most two, fewest sets first; a group whose direct exercises are all at 4 gets a new exercise that works it differently, on a day that already trains it; a group trained on no day goes to the day that works its half; adductors lagging on a 4-day plan go to a lower day; a group at 20 gets `AT_CEILING`; a plan whose equipment rules out every candidate gets `NO_EXERCISE`; four lagging groups give 3 advice and one `LATER` for the group with the most weekly sets; saving a check-in dismisses the open MORE_WORK advice. Applying: still-matching changes apply; an item edited since, a removed item or an exercise already added are skipped with the plan-changed message; applying twice changes nothing the second time. Stuck: an exercise flat for 21 days over three sessions gives one SWAP with up to 3 alternatives of the same kind first; one that went up once in the window does not; one done once in the window does not; one whose group is growing well does not; none while a deload or re-entry from the last 21 days stands; none again within 28 days of an earlier SWAP for it, dismissed or applied; applying a swap replaces it on every day it is planned, keeping the prescription, and does nothing if the choice is in the plan by then. Due: the four cases of the rule (no plan, no session, 27 and 28 days). Progress rows: the rule's three ways in, ratings oldest first up to 4, sets done counted per group over the 7 days ending today with missed sets left out, going-up counts. Powerlifting: `Coaching.evaluate(POWERLIFTING, ...)` with check-ins given equals the call without them; the golden scenarios pass unedited.
- [ ] **Step 3:** Run `./gradlew :domain:jvmTest --tests '*PhysiqueCoachTest*' --tests '*MuscleProgressTest*' --tests '*CoachingTest*'`. Expected: does not compile.
- [ ] **Step 4:** Implement the rules above exactly, and the JS bridge. In the report, list for each of the 18 groups the first three new-exercise candidates for a default 3-day plan (variant 0), so the brain can check them for odd choices.
- [ ] **Step 5:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain` and `cd web && npm test && npm run typecheck`. Expected: pass.
- [ ] **Step 6:** Commit `feat(domain): check-ins and the lagging-muscle coach`.

### Task 2: Android, storage, check-in screen, Coach and Progress

**Files:** `app/src/main/kotlin/com/powertracker/data/db/` (`Entities.kt` a `CheckInEntity`, `Daos.kt` a `CheckInDao`, `AppDatabase.kt` version 4 with `MIGRATION_3_4`), `data/TrainingRepository.kt` (or a new `CheckInRepository.kt`), `data/Backup.kt` (version 4); new `ui/checkin/` (`CheckInScreen.kt`, `CheckInViewModel.kt`); `ui/coach/` (the due card, the MORE_WORK and SWAP cards, apply and dismiss); `ui/progress/` (the body-part section and the Check in button on the bodybuilding side); `ui/today/TodayViewModel.kt` (pass check-ins to `Coaching.evaluate`); navigation in `ui/AppShell.kt`. Tests beside each, `BackupFixtureTest.kt`, and the shared fixture `web/src/backup/__fixtures__/android-v4.json` (the one file outside `app/` this task writes). Screenshots below.

**Interfaces — Consumes:** Task 1. **Produces:** storage `check_ins(id, date UNIQUE, lagging TEXT, growingWell TEXT)` with groups as their Kotlin names, comma-separated; backup version 4 adds a top-level `checkIns: [{ "id": 1, "date": <epochDay>, "lagging": ["CHEST"], "growingWell": ["QUADS"] }]` (an empty array when none; absent in older files means none), and advice of the two new types with their payload strings, unchanged otherwise.

What the lifter sees, on the bodybuilding side only:
1. **Coach:** when a check-in is due, a card at the top of the feed: title `Time for your check-in`, line `Four weeks on this plan. Mark the body parts that stand out; everything else stays on track.`, button `Check in`. It has no dismiss: a check-in with nothing marked is a valid answer and takes two taps.
2. **Check-in screen**, in the bodybuilding look, three steps with Back:
   - `Which body parts are lagging?` over the 18-group muscle grid the Exercises tab already has, multi-select, nothing ticked at first; line `Leave a body part unticked if it is on track.`; Next.
   - `Which are growing well?`, the same grid without the groups ticked as lagging (shown but not tickable); Save.
   - Saved: `Check-in saved`, then one line per lagging group: `More work suggested on Coach` for those with advice, and for the others by note: AT_CEILING `{Group} already gets {n} sets a week, the most the coach adds.`, NO_EXERCISE `No other {group} exercise uses the equipment in your plan.`, FULL `Every session that could take one already has 10 exercises.`, LATER `The coach adds to three body parts at a time. It will look at {group} again at your next check-in.`; buttons `See Coach` (when there is advice) and `Done`.
   - Save writes, in one transaction: the check-in (replacing one of the same date), the old OPEN MORE_WORK advice set to DISMISSED, the new advice. Leaving before Save changes nothing.
3. **Coach cards:** MORE_WORK: tag `More work`, title `{Group} is lagging`, then either `One more set of Incline DB press on Monday (3 → 4)` (one line per set addition) or `Add Pec deck on Thursday, 3 × 8–12 @ 8`, then `{Group}: 12 → 14 sets a week`; buttons `Add to plan` and `Dismiss`. SWAP: tag `Stuck`, title `{Exercise} hasn't moved in 3 weeks`, line `Same weight and no more reps since {date}. Another exercise for {group} can get it moving.`, one button per alternative `Swap for {name}`, and `Dismiss`. Applying carries out `PlanChange` in one transaction (catalogue names resolved as today), sets the advice APPLIED and shows its message; a failed write brings the card back, as today. A SWAP card whose exercise is no longer in the plan is not shown.
4. **Progress:** a `Check in` button at the top, and a `Body parts` section above the stage 1 exercise rows: one row per `MuscleProgress` row with the group, its latest rating as a small label (`Lagging`, `Growing well`, `On track`), `11 of 14 planned sets in the last 7 days`, `2 of 3 exercises going up` (left out when none can tell), and its earlier ratings as quiet text (`Lagging · On track · Growing well`, oldest first). Before the first check-in, a line under the button: `Your first check-in is four weeks after your first bodybuilding session. You can check in any time.`

- [ ] **Step 1:** Write the failing tests: migration 3 to 4 on a real version 3 database keeps every row and adds an empty table; save, replace on the same date, and read check-ins; backup version 4 round trip with check-ins and both new advice types; a version 3 fixture restores with no check-ins; a version 5 file is TooNew; the web's `web-v4.json` restores with every check-in and advice. Behaviour: the due card shows at 28 days and not at 27, never on the powerlifting side; the three steps and their wording; a group ticked lagging cannot be ticked growing well; Save stores one check-in, dismisses the old MORE_WORK advice and stores the new; leaving before Save stores nothing; MORE_WORK `Add to plan` changes the plan as the advice says and the plan-changed case shows its message; SWAP replaces the exercise on every planned day; Dismiss; Progress rows and the Check in button; nothing of this on the powerlifting side, whose screens and screenshots are unchanged.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Screenshots, dark and light: `checkin-lagging-bodybuilding`, `checkin-growing-bodybuilding`, `checkin-saved-bodybuilding`, `coach-checkin-bodybuilding` (the due card, a MORE_WORK card and a SWAP card), and the existing `progress-bodybuilding` which now shows the body parts (it changes on purpose). With `-Pscreenshots.update=true`, `git status` shows only these. Read them.
- [ ] **Step 5:** Write `android-v4.json` (a backup made by this app with two check-ins, one MORE_WORK and one SWAP advice, both sides' plans and a few sessions). Run `./gradlew :app:testDebugUnitTest`. Expected: pass. Commit `feat(app): check-ins and the lagging-muscle coach`.

### Task 3: web, storage, check-in screen, Coach and Progress

**Files:** `web/src/db/schema.ts`, `db.ts` (Dexie version 4, a `checkIns` store `++id, &date`), a new `web/src/db/checkIns.ts`; `web/src/backup/format.ts`, `backup.ts` (version 4); new `web/src/screens/checkin/` (`CheckInScreen.tsx`, its css and tests) and `web/src/state/useCheckIn.ts`; `web/src/screens/coach/` and `web/src/state/useCoach.ts` (the due card, the two cards, apply and dismiss); `web/src/screens/progress/` and `web/src/state/useProgress.ts`; `web/src/state/useToday.ts` (pass check-ins to `coachingEvaluate`); `web/src/router.tsx` and `App.tsx` (route `#/checkin`); the muscle grid of the Exercises tab reused (exported if needed); `web/e2e/bodybuilding.spec.ts` and the screenshot cases; fixture `web/src/backup/__fixtures__/web-v4.json`.

**Interfaces — Consumes:** Task 1. **Produces:** the same storage meaning and the same backup version 4 as Task 2, byte-compatible: the same keys, epochDay dates, Kotlin enum names, the same advice payload strings. Same screens, wording, rules and screenshot names as Task 2. If Task 2's report is there when wording or placement is not fixed by this plan, match it; otherwise use this plan's and list what to compare.

- [ ] **Step 1:** Write the failing vitest and Playwright tests, the twins of Task 2's, with `android-v4.json` restoring once Task 2 has written it (until then the test is marked to wait for it, not skipped silently: say so in the report), plus one end-to-end story: turn the mode on, build a plan, log bodybuilding sessions dated so a check-in is due, see the card, mark Chest lagging and Quads growing well, save, see the Saved lines, apply the Chest card on Coach, and find the extra set on Plan and the ratings on Progress.
- [ ] **Step 2:** Run the focused tests. Expected: fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Screenshots as Task 2 Step 4, with `UPDATE_SCREENSHOTS=1`; no existing screenshot other than those named may change.
- [ ] **Step 5:** Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: pass (a @speed miss: re-run the speed project once, alone). Commit `feat(web): check-ins and the lagging-muscle coach`.

### Task 4: version 1.4, notes, full run

**Files:** `app/build.gradle.kts` (`versionName = "1.4"`, `versionCode = 5`), `web/package.json` and the two lock-file fields (`1.4.0`), the Settings screenshots on both apps, `docs/next-update.md`, `README.md` (manual phone checklist: one line to check in and apply a suggestion).

- [ ] **Step 1:** Bump both versions; the versions-agree test passes.
- [ ] **Step 2:** Regenerate the Settings screenshots on both apps; only Settings PNGs change.
- [ ] **Step 3:** `docs/next-update.md`: stage 3 done (what the lifter gets; the sourced ceiling; the designer's numbers), stage 4 to come.
- [ ] **Step 4:** Full run, once: `./gradlew :domain:allTests --rerun-tasks`, `./gradlew :domain:checkWebDomain :app:testDebugUnitTest`, `./gradlew --stop`, then `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`.
- [ ] **Step 5:** Commit `chore: version 1.4; check-in and lagging-muscle coach notes`.

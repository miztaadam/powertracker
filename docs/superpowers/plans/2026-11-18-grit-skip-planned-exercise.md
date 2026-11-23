# Skip a planned exercise (version 1.13): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On Today, a planned exercise with no logged sets can be skipped for today, with an optional reason. It leaves the list, is stored as its own record, and the coach responds by reason. This works on both sides and both apps, and skips are in backups.

**Architecture:**
- **Domain:** skips are a new record, `Skip`, beside sessions. One new pure entry point, `Coaching.afterSkip`, turns a stored skip into at most one new card of a new advice type, `SKIP`. `Coaching.targetsFor` gains the skips, so that a pain skip holds that exercise's weight.
- **The web runs the same domain code,** through the generated JS package (`web/src/domain/generated`, `:domain:checkWebDomain`). So every rule is written once, in Kotlin.
- **Each app adds:** a store (Room v5 / a new Dexie version), the backup field, and the Today and Coach UI.

**Tech Stack:** Kotlin Multiplatform domain; Android Compose, Room, Robolectric; React 18, Dexie, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-18-grit-skip-planned-exercise-design.md` (approved by the owner on 2026-11-18). Read it first.

## Global Constraints
- **Skips never leak into numbers:** they are never a set, a missed set, a failure, a stall, a deload or ease-back-in signal. With no skips stored, every existing coach output, chart, best, volume and progression is unchanged, and the existing tests prove it unedited.
- **The main lifts are never swapped out:** any exercise whose category is `SQUAT`, `BENCH`, `DEADLIFT` or `STRICT_PRESS`. Those get a card only for pain, and that card has Dismiss only.
- **Thresholds** (product rules, no source needed), counted per exercise and per side over the 28 days ending on the skip's date, that skip included:
  - **busy:** the 2nd busy skip gives a card;
  - **pain:** the 1st pain skip gives a card;
  - **repeat:** the 3rd skip of any reason other than time gives a card;
  - **time:** never a card.
- **Card priority:** one skip creates at most one card, in the order pain, busy, repeat. There is no card in three cases:
  - an OPEN `SKIP` card or OPEN `SWAP` card for that exercise exists;
  - a `SKIP` card was created for it in the last 7 days *(my call, replacing the design's "one card per lift per week")*;
  - a dismissed card's memory blocks it (below).
- **A pain skip with no alternative left** (not a main lift) falls back to the main-lift pain card: the hold notice, with Dismiss only. A pain skip always gives a card unless one of the three cases above blocks it.
- **`SKIP` advice and the existing rails:** its `lift` is the exercise's `MainLift` for a main-lift category, and null otherwise.
  - With `SKIP` cards in `priorAdvice`, `Coaching.evaluate` and `Coaching.targetsFor` return exactly what they return without them (tested).
  - Every `when` or `switch` on `AdviceType` on both apps, the web's included, handles `SKIP`.
  - A `SKIP` card is hidden once its exercise is not in that side's plan, as `Swap` already is. A dismissed one follows the coach's existing 28-day dismissal memory, so a repeat card is not recreated within 28 days of its dismissal at the same or lower severity.
- **The pain hold belongs to the lifter:** a pain skip on either side holds that exercise's weight on both sides, until a session holding a logged set of it is dated after the skip, on either side. It has no time limit.
  - **Held means no increase decision is made.** The target repeats that exercise's latest session on the side being targeted: the same weight, and the same reps aim it had then. It is not a cap applied afterwards, so the bodybuilding coach's reset to the low end of the range after an increase does not happen either.
  - **If TARGET advice is stored ahead of time** (check `applyTarget(adviceId)` and where targets are stored on both apps), a pain skip dismisses that exercise's OPEN TARGET advice in the same transaction, so the next target is worked out with the hold.
- **Cards appear at the moment of the skip,** with no session needed. **Undo** and **Bring back** delete the skip and any still-OPEN `SKIP` card whose `skipId` is that skip's, in one transaction. A card that was already applied or dismissed stays.
- **One skip per exercise, per date, per side.** Skipping again replaces the reason and runs `afterSkip` again.
- **Exact strings:**
  - row menu: "Skip today";
  - sheet title: "Why skip <name>?";
  - the four reasons: "Busy or no equipment", "Pain", "Out of time", "Just skip";
  - snackbar: "<name> skipped." with "Undo";
  - list line: "Skipped today", each with "Bring back";
  - Finish's summary adds ", n skipped" when n ≥ 1.
  - **Card text**, with `<name>` the exercise name:
    - **busy:** "You've skipped <name> twice in 4 weeks because you were busy or had no equipment. Swap it for one of these?"
    - **pain, not a main lift:** "You skipped <name> because of pain. Try one of these instead. Its weight won't go up until you do <name> again. If the pain persists or gets worse, see a physician."
    - **pain, a main lift, or no alternative left:** "You skipped <name> because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician."
    - **repeat:** "You've skipped <name> 3 times in 4 weeks. Keep it in your plan, or swap it for one of these?"
- **The pain line's source** is ExRx.net, "Exercise Adaptation / Injury Prevention Tidbits", https://exrx.net/Injury/InjuryTidbits. It advises seeing a physician when pain persists or worsens (a physical therapist can refer you on), and progressing again only when the load can be done pain-free. ACSM's "Resting After an Injury: A Thing of the Past", https://acsm.org/resting-after-an-injury-a-thing-of-the-past/, supports training around the painful area. Both were found on 2026-11-18 by a web search restricted to the allowed sources, without opening the pages. Cite them in `CoachSources.kt` in its existing style.
- **Haptics:** one tick when a skip is stored, none on cancel, Undo or Bring back.
- **Version:** 1.13.0 on both apps, with versionCode the next after 1.12.3's.
- **Existing tests** pass unedited. List any that changed, each with its reason.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`, and never `playwright install`;
  - both apps' versions agree;
  - `git fetch` before any merge.

## Findings from Task 1 (binding on Tasks 2–5)
- **Both apps store only that day's TARGET advice** when Today opens, and skip recomputing an exercise that already has a target today, whatever its status. So:
  - **`skip` with reason PAIN** also **deletes** (not dismisses) today's OPEN TARGET advice for that exercise and side, in the same transaction, so that the next target is worked out with the hold.
  - **`unskip`** also deletes today's OPEN TARGET advice for that exercise and side, so that the target is worked out again without the hold.
  - Test both on each app.
- **Pass `skips` everywhere targets are worked out:**
  - `targetsFor(..., skips)`;
  - `repsToBeat(..., skips)` on Android, and `repsToBeatWithSkips` on the web.

  Without the second, a held bodybuilding exercise keeps its weight but resets its reps.
- **Task 1 added `AdvicePayload.Skipped`,** so the two exhaustive `when (payload)` in `CoachViewModel.kt` (lines ~139 and ~195) no longer compile. Task 2 adds a stopgap branch that hides the card (`null` / no-op) so the app builds, and Task 3 replaces it with the real card.
- **The exact signatures** are in Task 1's report, copied under "Task 1 signatures" in `docs/next-update.md` when 1.13 ships.

## Review Focus
1. **Sides are separate on Today:** a skip on the bodybuilding side leaves the same exercise on the powerlifting side's Today, and the reverse. Only the pain hold crosses sides.
2. **Midnight:** a skip dated yesterday doesn't hide the exercise today, and "Skipped today" is empty.
3. **The same exercise twice on one day** (two plan items): one skip hides both rows, and Bring back returns both.
4. **Undo after the card was applied or dismissed:** the skip goes, but the applied swap or the dismissal stays. Undo never edits the plan.
5. **Restoring a backup:**
   - a 1.12 backup (no `skips`) restores with no skips;
   - a 1.13 backup round-trips its skips;
   - skips for a custom exercise go when that exercise is deleted, but stay, with `planItemId` set to null, when their plan item is deleted.

---

### Task 1: Domain: skips, the SKIP card, the pain hold
**Files:**
- Modify: `domain/src/commonMain/kotlin/com/powertracker/domain/model/Models.kt` (add `SkipReason`, `Skip`)
- Modify: `domain/src/commonMain/kotlin/com/powertracker/domain/model/Advice.kt` (append `SKIP` to `AdviceType`, add `AdvicePayload.Skipped`)
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/coach/SkipCoach.kt`
- Modify: `Coaching.kt`, `Coach.kt` and `BodybuildingCoach.kt` (the hold), `PhysiqueCoach.kt` (side-aware swap), `CoachSources.kt`
- Test: `domain/src/commonTest/.../coach/SkipCoachTest.kt`, plus hold tests beside the existing `targetsFor` tests
- Regenerate `web/src/domain/generated` the way the repo does, so that `:domain:checkWebDomain` passes. Export the new types and functions for JS like their neighbours.

**Interfaces (produces):**
- `enum class SkipReason { BUSY, PAIN, TIME }`. The names are stored, so new values go last.
- `data class Skip(val id: Long, val date: LocalDate, val mode: TrainingMode, val exerciseId: Long, val planItemId: Long?, val reason: SkipReason?)`
- `AdvicePayload.Skipped(val skipId: Long, val exerciseId: Long, val name: String, val reason: SkipReason?, val alternatives: List<String>)`. It is `@Serializable`, and `alternatives` is empty for a main lift.
- `Coaching.afterSkip(skip: Skip, skips: List<Skip>, history: TrainingHistory, plan: List<PlanItem>, today: LocalDate, priorAdvice: List<Advice>): List<Advice>`
  - It returns zero or one `SKIP` advice, stamped `mode = skip.mode`.
  - `skips` is every stored skip, including `skip`.
- `Coaching.targetsFor(..., skips: List<Skip> = emptyList())`, and the same parameter on `Coach.targetsFor` and `BodybuildingCoach.targetsFor`.
  - A held exercise's target repeats its latest session on that side (weight and reps aim), with no increase decision. See Global Constraints.
- `Coaching.applySkipSwap(payload: AdvicePayload.Skipped, choice: String, mode: TrainingMode, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange`
  - It swaps the exercise in every plan item of `mode`.
  - On the bodybuilding side it keeps `PhysiqueCoach`'s ceiling check and its "changed since" messages. On the powerlifting side there is no ceiling.
- **Alternatives:** reuse `PhysiqueCoach.swapAlternatives`, made side-aware without changing its bodybuilding output. Take the first 3.
  - **busy:** only alternatives whose first equipment differs from the exercise's.
  - **pain and repeat:** the list as it comes.
  - If no alternative remains, a non-main-lift card is not created.

- [ ] **Write the failing tests in `SkipCoachTest`:**
  - `busy_secondInWindow_givesCard_firstDoesNot`;
  - `busy_secondOutside28Days_noCard`;
  - `busy_alternativesHaveDifferentFirstEquipment`;
  - `pain_first_givesCardWithAlternatives_andSourcedLine`, which asserts the exact pain string;
  - `pain_mainLift_givesCardWithNoAlternatives`, for each of the 4 main-lift categories;
  - `pain_noAlternativeLeft_givesHoldCard`;
  - `openSwapCard_blocksSkipCard`;
  - `skipCards_inPriorAdvice_leaveEvaluateAndTargetsUnchanged`;
  - `busy_mainLift_noCard`;
  - `repeat_thirdNonTime_givesCard_secondDoesNot`;
  - `time_neverCard`, and `time_notCountedTowardRepeat`;
  - `openSkipCard_blocksAnother`;
  - `cardInLast7Days_blocksAnother`;
  - `dismissedRepeat_notRecreatedWithin28Days`;
  - `onlyOneCardPerSkip_painWins`;
  - `otherSidesSkips_notCountedTowardThresholds`;
  - `card_carriesSkipId_andMode`.
- [ ] **Write the failing hold tests,** powerlifting and bodybuilding:
  - `painSkip_holdsTargetWeight`, and on the bodybuilding side `painSkip_keepsRepsAim_noResetToLowEnd`;
  - `painSkip_onOtherSide_holdsToo`;
  - `loggedSessionAfterSkip_releasesHold`, on either side;
  - `busySkip_doesNotHold`;
  - `noSkips_targetsUnchanged`, against the existing fixtures.
- [ ] **Write the failing tests** for `applySkipSwap`, one per side, and for the bodybuilding ceiling refusal.
- [ ] Run `./gradlew :domain:allTests`: the new tests fail.
- [ ] Implement the interfaces above. Add the two `CoachSources` entries.
- [ ] Run `./gradlew :domain:allTests --rerun-tasks :domain:checkWebDomain`: everything passes, and the generated web package is updated.
- [ ] Commit `feat(domain): skipping a planned exercise and the coach's response`.

### Task 2: Android: storage and backup
**Files:** `app/src/main/kotlin/com/powertracker/data/db/` (the entity, the DAO, `AppDatabase` version 5, and a migration from 4 to 5 with the exported schema), the training repository, `data/Backup.kt`, and the tests beside them.

**Interfaces:**
- Consumes: Task 1's `Skip`, `SkipReason`, `AdvicePayload.Skipped` and `AdviceType.SKIP`.
- Produces, on `TrainingRepository`:
  - `skips(): Flow<List<Skip>>`;
  - `skip(skip: Skip, cards: suspend (stored: Skip, all: List<Skip>) -> List<Advice>): Long`. It upserts on the unique key (date, mode, exerciseId), then inserts what `cards` returns, all in one transaction, and returns the skip's id;
  - `unskip(skipId: Long)`, which deletes the skip and the OPEN `SKIP` advice carrying that `skipId`, in one transaction.
- **Foreign keys:**
  - when an exercise is deleted, its skips are deleted (CASCADE);
  - when a plan item is deleted, the skip's `planItemId` is set to null.

- [ ] **Failing tests:**
  - the migration from 4 to 5 keeps all data (the existing migration-test pattern);
  - upsert replaces the reason;
  - `unskip` keeps APPLIED and DISMISSED cards;
  - the cascade and set-null behaviour;
  - a backup round-trips skips;
  - a 1.12-format backup restores with no skips;
  - every 1.13 backup has the new format version, with or without skips.
- [ ] Implement, then run `./gradlew :app:testDebugUnitTest`: all pass.
- [ ] Commit `feat(app): store skips and include them in backups`.

### Task 3: Android: Today and Coach
**Files:** `ui/today/TodayViewModel.kt`, `TodayScreen.kt`, `ui/coach/CoachViewModel.kt`, the Coach card UI, and tests and screenshots.

**Interfaces:**
- Consumes: Tasks 1 and 2.
- Produces:
  - `TodayViewModel.skip(exerciseId: Long, planItemId: Long?, reason: SkipReason?)`, which stores the skip through `skip(...)` with `Coaching.afterSkip`, and ticks once;
  - `TodayViewModel.bringBack(exerciseId: Long)`, which un-skips today's skip of that exercise on this side;
  - `TodayState.skippedToday: List<SkippedToday(exerciseId, name, skipId)>`;
  - `FinishSummary` gains `skipped: Int`.
  - **Today's targets pass `skips`** to `Coaching.targetsFor`.
  - **The Coach screen** shows a `SKIP` card as a swap chooser when `alternatives` is non-empty, using `Coaching.applySkipSwap` and the existing Swap card's look. With no alternatives, it shows Dismiss only.

- [ ] **Failing tests:**
  - `skip_menuOnlyOnPlannedRowWithNoSets`;
  - `skip_sheetCancel_storesNothing`;
  - `skip_eachReason_storedAndRowLeaves`;
  - `skip_undo_removesSkipAndOpenCard`;
  - `skip_bringBack_returnsRowInPlace`;
  - `skip_sameExerciseTwiceOnDay_hidesBoth`;
  - `skip_otherSideUnaffected`;
  - `skip_yesterdaysSkip_notHiddenToday`;
  - `skip_withoutSession_works`;
  - `finish_countsSkipped`;
  - `skip_ticksOnce`;
  - `skip_survivesRecreation`, for the sheet open with a reason not yet chosen;
  - `coach_skipCard_swapApplies`, on both sides;
  - `coach_mainLiftPainCard_dismissOnly`;
  - `painSkip_nextTodayTarget_isHeld`, an app-level check that the call site passes `skips`;
  - `skipEveryPlannedExercise_noRestDayView_skippedLineShown`;
  - `afterSwapApplied_skippedLine_dropsSwappedName`: no **Bring back** that does nothing;
  - `doubleTapSkipToday_doesNotPickAReason`, using the 300 ms window after the sheet opens, as the add form does.
- [ ] Implement, then run `./gradlew :app:testDebugUnitTest`: all pass.
- [ ] Re-shoot with `-Pscreenshots.update=true`, adding the row menu, the sheet, the "Skipped today" line and a `SKIP` card, on both sides, dark and light. Only new shots, plus any shot of Today's rows if the menu icon changes them. List them.
- [ ] Commit `feat(app): skip a planned exercise on Today`.

### Task 4: Web: storage and backup
**Files:** `web/src/db/db.ts` (a new `SCHEMA_VERSIONS` entry with a `skips` table: `++id, &[date+mode+exerciseId], exerciseId, planItemId`), the data functions beside the session ones, `web/src/backup/backup.ts`, and tests.

**Interfaces:** the same three operations as Task 2 (`skips`, `skip`, `unskip`), in one Dexie transaction each, with the same deletion behaviour done explicitly where the exercise or the plan item is deleted.

- [ ] Write the same failing tests as Task 2, in vitest.
- [ ] Implement, then run `npm test`: all pass.
- [ ] Commit `feat(web): store skips and include them in backups`.

### Task 5: Web: Today and Coach
**Files:** `web/src/state/useToday.ts`, `web/src/screens/today/*`, the Coach state and screen, and tests.

**Interfaces:** the same behaviour and strings as Task 3, using the generated domain's `afterSkip`, `targetsFor` with skips, and `applySkipSwap`.

- [ ] Write the failing vitest tests, with the same names as Task 3 except `survivesRecreation`.
- [ ] **Add the e2e** `skip_painOnBench_rowLeaves_cardShows_undoTakesBoth`.
- [ ] Implement. Then run `npm test`, the typecheck, `npm run build` and the full e2e with `--trace off`: all pass.
- [ ] Run the speed project three times: Today's and the picker's speed checks still pass.
- [ ] Re-shoot with `UPDATE_SCREENSHOTS=1`, adding the same new shots as Task 3. Put back the known noise (form-guide, exercise-guide-actions, today, today-rest) unless Today's rows really changed. List the shots.
- [ ] Commit `feat(web): skip a planned exercise on Today`.

### Task 6: Version 1.13, notes, full run
- Version 1.13.0 on both apps, plus the Settings screenshots.
- `docs/next-update.md`: add a "Done: skip a planned exercise (1.13)" section, with the changed tests and screenshots.
- README phone checks:
  - skip on Today with each reason;
  - Undo;
  - Bring back;
  - the pain card on a main lift and on an accessory;
  - the weight held next time.
- Full run: Gradle `:domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest`, then web `npm test`, typecheck, build, the full e2e and 3 speed runs.
- Commit `chore: version 1.13; skip a planned exercise`.

Then:
1. independent reviews, one for the domain and one per app;
2. fixes;
3. merge under the owner's standing permission.

# Choose sets and reps when adding to the plan (version 1.12): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On Plan's **Add**, picking an exercise opens the sets/reps form, filled in with today's ready-made numbers. **Add** stores the item with the chosen numbers, and **Cancel** stores nothing.

**Architecture:** Reuse the existing Edit form (`PlanItemDialog`) on both apps in an "add" mode: the title is "Add to <Day>", the confirm button reads "Add", and the item has not been saved yet. The ready-made numbers come from the same `newItem` code that adding uses today, split out so the form can show them before anything is stored. The add path takes the chosen numbers and keeps its single transaction (the catalogue row and the plan item together).

**Tech Stack:** Kotlin, Compose, Robolectric; React 18, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-17-grit-plan-add-sets-reps-design.md` (approved by the owner on 2026-11-17).

## Global Constraints
- **Unchanged:**
  - editing a row;
  - Today's **Add exercise**;
  - the plan builder;
  - the Exercises tab's "Add to plan" (`addOnce`, both apps);
  - the ready-made numbers;
  - the picker itself and its 100 ms speed checks.
- **The form's limits are Edit's:**
  - sets 1–10;
  - fixed reps up to `MAX_PLAN_REPS` (20);
  - a range's top no lower than its bottom, up to `MAX_RANGE_REPS` (30);
  - RPE in steps of 0.5 within `MIN_RPE`–`MAX_RPE`.
- **Changing the exercise inside the add form** keeps the numbers on screen, as Edit does (my call: what you typed is not overwritten).
- **Haptics:** one tick when the item is stored, none on Cancel.
- **Existing tests** pass unedited, except those that pick on Plan's Add and expect the item stored at once. Those gain the **Add** tap. List each change with its reason.
- **Screenshots:** only new add-form shots, plus any existing shot that showed Plan's Add flow. List them.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`;
  - both apps' versions agree;
  - `git fetch` before any merge.

## Review Focus
1. **A double tap on Add** stores one item, not two.
2. **Android, rotation or process death while the add form is open:** the chosen numbers and the picked exercise survive, and nothing is stored until **Add**.
3. **Change, then Cancel:** nothing is added. A custom exercise created through Change stays in the library.
4. **Creating your own exercise fails:** no form opens, and the failure shows as today.
5. **Bodybuilding side:** the form opens as 3 × 8–12 @ 8, and **Add** stores `reps` 8 and `repsHigh` 12, or whatever was chosen.

---

### Task 1: Android (beside Task 2)
**Files:**
- `app/src/main/kotlin/com/powertracker/ui/plan/PlanViewModel.kt`
- `app/src/main/kotlin/com/powertracker/ui/plan/PlanScreen.kt`: the `PlanDialog` type, the `Add` branch, `PlanItemDialog`
- Tests: the existing Plan screen and view-model tests under `app/src/test/`, and the screenshot test that covers Plan's dialogs.

**Interfaces (produces):**
- `PlanViewModel.draft(day: DayOfWeek, entry: PickerEntry): PlanItem` returns an item with id 0 and today's ready-made numbers, using the existing `newItem` and the current `mode` and `defaultRpe`. It stores nothing.
- `PlanViewModel.add(day: DayOfWeek, entry: PickerEntry, sets: Int, reps: Int, rpe: Double, repsHigh: Int?, onAdded: () -> Unit = {})` stores the item at the end of `day` with those numbers, in the same `training.savePlanItem(item, entry.catalogueName)` call as today. The old three-argument `add` goes, if nothing else uses it.
- `PlanDialog.AddDetails(day, entry, draft)` is saved across rotation in the same way `PlanDialog.Edit` is.
- `PlanItemDialog` gains `confirmText: String`. Edit passes "Save" and add passes "Add". In add mode the form shows the picked entry's name.

**Steps:**
- [ ] **Write the failing tests:**
  - `planAdd_pick_opensFormWithDefaults`: powerlifting, a compound shows Sets 3, Reps 5 and the default RPE;
  - `planAdd_changeNumbers_addStoresThem`: 4 × 6 @ 7.5 is stored exactly, and nothing is stored before **Add**;
  - `planAdd_cancel_storesNothing`;
  - `planAdd_bodybuilding_storesRange`: 3 × 8–12 @ 8 by default, and a changed range is stored;
  - `planAdd_ownExercise_opensForm`;
  - `planAdd_doubleTapAdd_storesOne`;
  - `planAdd_survivesRecreation`: the state is restored with the numbers kept;
  - `planAdd_ticksOnceWhenStored`.
- [ ] Run `./gradlew :app:testDebugUnitTest`: the new tests fail.
- [ ] Implement the interfaces above. Update the comment above the `Add` branch.
- [ ] Run `./gradlew :app:testDebugUnitTest`: all pass. List each existing test that changed.
- [ ] Re-shoot the screenshots with `-Pscreenshots.update=true`. Add the add-form shots: powerlifting and bodybuilding, dark and light. Only those should change; list them.
- [ ] Commit `feat(app): choose sets and reps when adding to the plan`.

### Task 2: Web (beside Task 1)
**Files:**
- `web/src/state/usePlan.ts`
- `web/src/screens/plan/PlanScreen.tsx`
- `web/src/screens/plan/PlanDialogs.tsx`
- Tests: `PlanScreen.test.tsx`, and the e2e specs that add on Plan (grep `Add to` in `web/e2e/`), plus `screenshots.spec.ts`.

**Interfaces (produces):**
- `PlanActions.draft(weekday: number, entry: PickerEntry): Promise<{ sets: number; reps: number; targetRpe: number; repsHigh?: number }>` uses the existing `newItem` and `activeSide`. It stores nothing.
- `PlanActions.add(weekday: number, entry: PickerEntry, chosen: { sets: number; reps: number; targetRpe: number; repsHigh?: number })`: today's `add` with the chosen numbers laid over `newItem`'s. It keeps the `sliding` wrapper and the single `addPlanItem` call.
- `OpenDialog` gains `{ kind: 'addDetails'; weekday: number; entry: PickerEntry; draft: … }`.
- `PlanItemDialog` gains `confirmText?: string` (default "Save"). Its `item` prop accepts the draft, which has no id. In add mode, `exerciseName` is the entry's name.

**Steps:**
- [ ] **Write the failing vitest tests** in `PlanScreen.test.tsx`, with the same eight names and assertions as Task 1 (`survivesRecreation` is not needed on the web).
- [ ] Run `npm test -- PlanScreen`: they fail.
- [ ] Implement the interfaces above. Update the comment above `dialog?.kind === 'add'`.
- [ ] **Add the e2e test** `planAdd_chooseSetsReps_storedAndShownInRow`: pick Bench on Monday, set 5 × 3, tap **Add**, and the row reads 5 × 3.
- [ ] Run `npm test`, typecheck, build, and the full e2e with `--trace off`: all pass. List each existing test that changed.
- [ ] Re-shoot the screenshots with `UPDATE_SCREENSHOTS=1`. Add the add-form shots: powerlifting and bodybuilding, dark and light. Only those may change, apart from the known form-guide, exercise-guide-actions, today and today-rest noise, which is put back. List them.
- [ ] Commit `feat(web): choose sets and reps when adding to the plan`.

### Task 3: Version 1.12, notes, full run
- Version 1.12 on both apps (versionCode 17, web 1.12.0), and the Settings screenshots.
- `docs/next-update.md`: add a "Done: choose sets and reps when adding (1.12)" section.
- README phone checks: on Plan, **Add** on a day, then pick an exercise. The form opens with 3 × 5 or 3 × 10 (powerlifting) or 3 × 8–12 (bodybuilding). **Add** stores what you set, and **Cancel** stores nothing.
- Full run, then five quiet speed runs.
- Commit `chore: version 1.12; choose sets and reps when adding`.

Then:
1. independent reviews per platform;
2. fixes;
3. merge under the owner's standing permission.
